package com.example.attendanceapp.Dashboards;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceapp.LoginScreen.LoginActivity;
import com.example.attendanceapp.ProfileActivity;
import com.example.attendanceapp.R;
import com.example.attendanceapp.adaptors.StudentListAdapter;
import com.example.attendanceapp.bluetooth.BluetoothHelper;
import com.example.attendanceapp.models.Student;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TeacherDashboard extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // UI Components
    private DrawerLayout drawerLayout;
    private TextView tvAttendanceStatus, tvDetectedCount;
    private MaterialAutoCompleteTextView spinnerYear, spinnerBranches;
    private RecyclerView rvStudents;
    private StudentListAdapter adapter;
    private Button btnStartAttendance, btnStopAttendance;
    private ProgressBar progressScan;
    private View indicatorBluetooth;
    private LinearLayout emptyStateLayout;
    private String teacherSubject;
    private static final int DISCOVERABLE_DURATION = 300;
    private static final int MIN_RSSI = -70; // Minimum signal strength for "in range"
    private static final String SESSION_PREFIX = "ATT_"; // Shorter prefix for session names
    private final Map<String, Integer> deviceRssiMap = new HashMap<>();


    // Bluetooth and scanning
    private BluetoothHelper bluetoothHelper;
    private boolean isScanning = false;
    private Handler scanHandler;
    private Runnable scanRunnable;
    private final Set<String> detectedDevices = new HashSet<>();

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> bluetoothEnableLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;

    // Constants
    private static final int SCAN_DURATION = 120000; // 2 minutes
    private String currentSessionId = null;
    private String currentBranch = null;
    private String currentYear = null;
    private String originalBluetoothName = null;
    private DatabaseReference activeSessionRef;
    private ValueEventListener attendanceListener;
    private ActivityResultLauncher<Intent> discoverableLauncher;


    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
    })
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        initializeActivityResultLaunchers();
        initializeViews();
        loadTeacherSubject();
        setupToolbar();
        setupNavigationDrawer();
        setupStudentList();
        setupButtonListeners();
        setupBluetooth();
        setupBackPressHandler();


        // Setup year spinner
        ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.years_array,
                android.R.layout.simple_spinner_item
        );
        spinnerYear.setAdapter(yearAdapter);

        // Setup branch spinner
        ArrayAdapter<CharSequence> branchAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.branches,
                android.R.layout.simple_spinner_item
        );
        spinnerBranches.setAdapter(branchAdapter);

    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    private void initializeActivityResultLaunchers() {
        // Bluetooth enable request
        bluetoothEnableLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (hasBluetoothPermissions()) {
                            startAttendanceScan();
                        }
                    } else {
                        showSnackbar("Bluetooth is required for attendance", false);
                    }
                }
        );

        discoverableLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_CANCELED) {
                        showSnackbar("Discoverability required for students", false);
                        stopAttendanceScan();
                    }
                }
        );
        // Permissions request
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = true;
                    for (Boolean granted : result.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }

                    if (allGranted) {
                        if (bluetoothHelper.isBluetoothEnabled()) {
                            startAttendanceScan();
                        } else {
                            requestEnableBluetooth();
                        }
                    } else {
                        showErrorDialog();
                    }
                }
        );
    }
    private void loadTeacherSubject() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            teacherSubject = documentSnapshot.getString("subject");
                            Log.d("TeacherDashboard", "Loaded teacher subject: " + teacherSubject);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("TeacherDashboard", "Error loading teacher subject", e);
                        teacherSubject = "Unknown Subject";
                    });
        }
    }


    private void initializeViews() {
        drawerLayout = findViewById(R.id.main_teacher_layout);
        tvAttendanceStatus = findViewById(R.id.tvAttendanceStatus);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerBranches = findViewById(R.id.spinnerBranches);
        rvStudents = findViewById(R.id.rvStudents);
        btnStartAttendance = findViewById(R.id.btnStartAttendance);
        btnStopAttendance = findViewById(R.id.btnStopAttendance);
        progressScan = findViewById(R.id.progressScan);
        tvDetectedCount = findViewById(R.id.tvDetectedCount);
        indicatorBluetooth = findViewById(R.id.indicatorBluetooth);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);

        scanHandler = new Handler(Looper.getMainLooper());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    private void setupNavigationDrawer() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupStudentList() {
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentListAdapter(new ArrayList<>());
        rvStudents.setAdapter(adapter);
        updateEmptyState();
    }

    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
    })
    private void setupButtonListeners() {
        btnStartAttendance.setOnClickListener(v -> {
            if (validateInputs()) return;

            if (!hasBluetoothPermissions()) {
                requestBluetoothPermissions();
                return;
            }

            if (!bluetoothHelper.isBluetoothEnabled()) {
                requestEnableBluetooth();
                return;
            }

            startAttendanceScan();
        });

        btnStopAttendance.setOnClickListener(v -> stopAttendanceScan());
    }

    private void setupBluetooth() {
        bluetoothHelper = new BluetoothHelper(this);
        updateBluetoothStatus(bluetoothHelper.isBluetoothEnabled());
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @RequiresPermission(allOf = {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            })
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (isScanning) {
                    showExitScanningDialog();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private boolean validateInputs() {
        String selectedYear = spinnerYear.getText().toString().trim();
        String selectedBranch = spinnerBranches.getText().toString().trim();

        if (selectedYear.isEmpty()) {
            spinnerYear.setError("Please select a year");
            showSnackbar("Please select a year", false);
            return true;
        }

        if (selectedBranch.isEmpty()) {
            spinnerBranches.setError("Please select a branch");
            showSnackbar("Please select a branch", false);
            return true;
        }

        return false;
    }

    private boolean hasBluetoothPermissions() {
        return bluetoothHelper != null && bluetoothHelper.hasPermissions();
    }

    private void requestBluetoothPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.BLUETOOTH_SCAN);
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT);

        permissionLauncher.launch(permissions.toArray(new String[0]));
    }

    private void requestEnableBluetooth() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            requestBluetoothPermissions();
            return;
        }

        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        bluetoothEnableLauncher.launch(enableBtIntent);
    }

    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
    })
    private void startAttendanceScan() {
        if (validateInputs()) return;

        if (teacherSubject == null || teacherSubject.isEmpty()) {
            showSnackbar("Teacher subject not found. Please restart the app.", false);
            return;
        }

        currentSessionId = UUID.randomUUID().toString().substring(0, 6);
        currentBranch = spinnerBranches.getText().toString().trim();
        currentYear = spinnerYear.getText().toString().trim();

        // ✅ Create a more detectable session name
        String sessionName = SESSION_PREFIX + currentBranch + "_" + currentYear + "_" + teacherSubject + "_" + currentSessionId;

        createActiveSession(teacherSubject, currentSessionId, currentBranch, currentYear);

        if (originalBluetoothName == null) {
            originalBluetoothName = bluetoothHelper.getBluetoothName();
        }

        bluetoothHelper.setBluetoothName(sessionName);
        makeTeacherDiscoverable();
        bluetoothHelper.startDiscovery();
        startScan();
        startAttendanceMonitoring();

        // Use string resources
        tvAttendanceStatus.setText(getString(R.string.session_active_format, currentBranch, currentYear));
        tvAttendanceStatus.setTextColor(ContextCompat.getColor(this, R.color.green_600));

        Log.d("TeacherDashboard", "✅ Session started: " + sessionName);
    }
    private void makeTeacherDiscoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION);
        discoverableLauncher.launch(discoverableIntent);
    }

    private void createActiveSession(String teacherSubject, String currentSessionId, String currentBranch, String currentYear) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("sessionId", currentSessionId);
        sessionData.put("subject", teacherSubject); // Add subject here
        sessionData.put("branch", currentBranch);
        sessionData.put("year", currentYear);
        assert user != null;
        sessionData.put("teacherId", user.getUid());
        sessionData.put("teacherName", user.getDisplayName());
        sessionData.put("startTime", System.currentTimeMillis());
        sessionData.put("active", true);
        sessionData.put("attendanceCount", 0);


        activeSessionRef = FirebaseDatabase.getInstance()
                .getReference("activeSessions")
                .child(currentBranch)
                .child(currentYear)
                .child(currentSessionId);

        activeSessionRef.setValue(sessionData)
                .addOnSuccessListener(aVoid -> Log.d("TeacherDashboard", "Active session created"))
                .addOnFailureListener(e -> {
                    Log.e("TeacherDashboard", "Error creating session", e);
                    showSnackbar("Error creating session", false);
                });
    }

    private void startAttendanceMonitoring() {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DatabaseReference attendanceRef = FirebaseDatabase.getInstance()
                .getReference("attendanceRecords")
                .child(currentDate)
                .child(currentBranch)
                .child(currentYear);

        attendanceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Student> attendedStudents = new ArrayList<>();

                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    String studentId = studentSnapshot.getKey();
                    String studentName = studentSnapshot.child("studentName").getValue(String.class);
                    String status = studentSnapshot.child("status").getValue(String.class);

                    if ("Present".equals(status)) {
                        attendedStudents.add(new Student(studentId, studentName, status));
                    }
                }

                runOnUiThread(() -> {
                    adapter.updateWithRealAttendance(attendedStudents);
                    updateDetectionCount();
                    updateEmptyState();
                    updateActiveSessionCount(attendedStudents.size());
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TeacherDashboard", "Error monitoring attendance", error.toException());
            }
        };

        attendanceRef.addValueEventListener(attendanceListener);
    }

    private void updateActiveSessionCount(int count) {
        if (activeSessionRef != null) {
            activeSessionRef.child("attendanceCount").setValue(count);
        }
        tvDetectedCount.setText(String.valueOf(count));
        if (count > 0) {
            tvAttendanceStatus.setText(getString(R.string.session_active_with_count,
                    currentBranch, currentYear, count));
        }
    }

    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
    })
    private void stopAttendanceScan() {
        if (activeSessionRef != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("active", false);
            updates.put("endTime", System.currentTimeMillis());
            activeSessionRef.updateChildren(updates);
        }

        if (attendanceListener != null) {
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            DatabaseReference attendanceRef = FirebaseDatabase.getInstance()
                    .getReference("attendanceRecords")
                    .child(currentDate)
                    .child(currentBranch)
                    .child(currentYear);
            attendanceRef.removeEventListener(attendanceListener);
        }

        if (originalBluetoothName != null && bluetoothHelper!=null) {
            bluetoothHelper.setBluetoothName(originalBluetoothName);
        }

        isScanning = false;
        updateUIForScanning(false);

        if (bluetoothHelper != null) {
            bluetoothHelper.cancelDiscovery();
        }

        try {
            unregisterReceiver(discoveryReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }

        if (scanRunnable != null) {
            scanHandler.removeCallbacks(scanRunnable);
        }

        if (adapter.getItemCount() > 0) {
            saveAttendanceDataEnhanced();
        } else {
            tvAttendanceStatus.setText("No students detected in this session");
            tvAttendanceStatus.setTextColor(ContextCompat.getColor(this, R.color.orange_600));
        }

        // Reset session variables
        currentSessionId = null;
        currentBranch = null;
        currentYear = null;
        activeSessionRef = null;
        attendanceListener = null;
    }

    private void saveAttendanceDataEnhanced() {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Create session summary
        Map<String, Object> sessionSummary = new HashMap<>();
        sessionSummary.put("sessionId", currentSessionId);
        sessionSummary.put("branch", currentBranch);
        sessionSummary.put("year", currentYear);
        assert user != null;
        sessionSummary.put("teacherId", user.getUid());
        sessionSummary.put("teacherName", user.getDisplayName());
        sessionSummary.put("date", currentDate);
        sessionSummary.put("startTime", System.currentTimeMillis());
        sessionSummary.put("totalPresent", adapter.getItemCount());
        sessionSummary.put("status", "completed");

        // Save session summary
        FirebaseDatabase.getInstance()
                .getReference("sessionSummaries")
                .child(currentDate)
                .child(currentBranch)
                .child(currentYear)
                .child(currentSessionId)
                .setValue(sessionSummary)
                .addOnSuccessListener(aVoid -> {
                    tvAttendanceStatus.setText("Session saved successfully");
                    tvAttendanceStatus.setTextColor(ContextCompat.getColor(this, R.color.green_600));
                    showSnackbar("Attendance session saved", true);
                })
                .addOnFailureListener(e -> {
                    tvAttendanceStatus.setText("Error saving session");
                    tvAttendanceStatus.setTextColor(ContextCompat.getColor(this, R.color.red_600));
                    showSnackbar("Failed to save session", false);
                    Log.e("TeacherDashboard", "Error saving session", e);
                });

        // Save to teacher's session history
        FirebaseDatabase.getInstance()
                .getReference("teacherSessions")
                .child(user.getUid())
                .child(currentDate)
                .child(currentSessionId)
                .setValue(sessionSummary);
    }

    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
    })
    // Remove the simulated discovery and rely on real Bluetooth
    private void startScan() {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }

        isScanning = true;
        detectedDevices.clear();
        deviceRssiMap.clear();
        updateUIForScanning(true);

        adapter.clearData();
        updateDetectionCount();

        // Start discovery with enhanced filter
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryReceiver, filter);

        bluetoothHelper.startDiscovery();

        // Restart discovery periodically for better detection
        scanRunnable = new Runnable() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
            @Override
            public void run() {
                if (isScanning && bluetoothHelper != null) {
                    bluetoothHelper.cancelDiscovery();
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(() -> {
                        if (isScanning) {
                            bluetoothHelper.startDiscovery();
                            scanHandler.postDelayed(this, 10000); // Restart every 10 seconds
                        }
                    }, 1000);
                }
            }
        };
        scanHandler.postDelayed(scanRunnable, 10000);

        // Auto-stop after scan duration
        scanHandler.postDelayed(() -> {
            if (isScanning) {
                stopAttendanceScan();
                tvAttendanceStatus.setText(getString(R.string.scan_completed_automatically));
                showSnackbar("Scan completed", true);
            }
        }, SCAN_DURATION);
    }
    // Update UI methods to use string resources
    private void updateUIForScanning(boolean scanning) {
        if (scanning) {
            tvAttendanceStatus.setText(getString(R.string.scanning_for_students));
            tvAttendanceStatus.setTextColor(ContextCompat.getColor(this, R.color.blue_600));
            progressScan.setVisibility(View.VISIBLE);
            btnStartAttendance.setEnabled(false);
            btnStopAttendance.setEnabled(true);
        } else {
            tvAttendanceStatus.setText(getString(R.string.ready_to_scan));
            tvAttendanceStatus.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            progressScan.setVisibility(View.GONE);
            btnStartAttendance.setEnabled(true);
            btnStopAttendance.setEnabled(false);
        }
        updateEmptyState();
    }
    private void updateEmptyState() {
        boolean hasStudents = adapter.getItemCount() > 0;
        rvStudents.setVisibility(hasStudents ? View.VISIBLE : View.GONE);
        emptyStateLayout.setVisibility(hasStudents ? View.GONE : View.VISIBLE);
    }

    private void startSimulatedDiscovery() {
        Handler simulationHandler = new Handler(Looper.getMainLooper());

        simulationHandler.postDelayed(() -> {
            if (!isScanning) return;
            addSimulatedStudent("S001", "Jay Raghuvanshi");
        }, 2000);

        simulationHandler.postDelayed(() -> {
            if (!isScanning) return;
            addSimulatedStudent("S002", "Hardik Tampu");
        }, 5000);

        simulationHandler.postDelayed(() -> {
            if (!isScanning) return;
            addSimulatedStudent("S003", "Shrashti Jadon");
        }, 8000);

        simulationHandler.postDelayed(() -> {
            if (!isScanning) return;
            addSimulatedStudent("S004", "Rajesh Jadon");
        }, 12000);
    }

    private void addSimulatedStudent(String id, String name) {
        Student student = new Student(id, name, "Present");
        adapter.addStudentIfNew(student);
        updateDetectionCount();
        updateEmptyState();
    }

    private void updateDetectionCount() {
        int count = adapter.getItemCount();
        tvDetectedCount.setText(String.valueOf(count));
    }

    // Bluetooth state receiver
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @RequiresPermission(allOf = {
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
        })
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        updateBluetoothStatus(true);
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        updateBluetoothStatus(false);
                        if (isScanning) {
                            stopAttendanceScan();
                            showSnackbar("Bluetooth turned off, scanning stopped", false);
                        }
                        break;

                }
            }
        }
    };
    // Replace your existing discoveryReceiver with this enhanced version
    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    updateBluetoothStatus(true);
                    if (isScanning) {
                        makeTeacherDiscoverable(); // Re-enable discoverability
                    }
                }
            }

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    if (ActivityCompat.checkSelfPermission(TeacherDashboard.this,
                            Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }

                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress();

                    // Get signal strength (RSSI)
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                    Log.d("TeacherDashboard", "Found device: " + deviceName + " RSSI: " + rssi);

                    if (deviceName != null && isStudentDevice(deviceName)) {
                        // Check if device is in range
                        if (rssi >= MIN_RSSI) {
                            if (!detectedDevices.contains(deviceAddress)) {
                                detectedDevices.add(deviceAddress);
                                deviceRssiMap.put(deviceAddress, rssi);

                                // Extract student info from device name
                                String[] parts = deviceName.split("-");
                                if (parts.length >= 3) {
                                    String studentId = parts[1]; // Student-S001-Name format
                                    String studentName = parts[2].replace("_", " ");

                                    Student student = new Student(studentId, studentName, "Present");
                                    adapter.addStudentIfNew(student);
                                    updateDetectionCount();
                                    updateEmptyState();

                                    // Save to Firebase immediately
                                    saveStudentAttendance(studentId, studentName, rssi);

                                    Log.d("TeacherDashboard", "✅ Student detected: " + studentName + " (RSSI: " + rssi + ")");
                                }
                            } else {
                                // Update RSSI for existing device
                                deviceRssiMap.put(deviceAddress, rssi);
                            }
                        } else {
                            Log.d("TeacherDashboard", "❌ Device out of range: " + deviceName + " (RSSI: " + rssi + ")");
                        }
                    }
                }
            }
        }
    };

    private void saveStudentAttendance(String studentId, String studentName, int rssi) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> attendanceData = new HashMap<>();
        attendanceData.put("studentId", studentId);
        attendanceData.put("studentName", studentName);
        attendanceData.put("status", "Present");
        attendanceData.put("timestamp", System.currentTimeMillis());
        attendanceData.put("time", currentTime);
        attendanceData.put("rssi", rssi);
        attendanceData.put("sessionId", currentSessionId);
        attendanceData.put("subject", teacherSubject);

        // Save to attendance records
        FirebaseDatabase.getInstance()
                .getReference("attendanceRecords")
                .child(currentDate)
                .child(currentBranch)
                .child(currentYear)
                .child(studentId)
                .setValue(attendanceData)
                .addOnSuccessListener(aVoid ->
                        Log.d("TeacherDashboard", "✅ Attendance saved for: " + studentName))
                .addOnFailureListener(e ->
                        Log.e("TeacherDashboard", "❌ Error saving attendance", e));
    }
    private void updateBluetoothStatus(boolean enabled) {
        if (enabled) {
            indicatorBluetooth.setBackgroundResource(R.drawable.circle_bluetooth_on);
        } else {
            indicatorBluetooth.setBackgroundResource(R.drawable.circle_bluetooth_off);
        }
    }

    private boolean isStudentDevice(String deviceName) {
        return deviceName != null && (deviceName.startsWith("Student-") ||
                deviceName.startsWith("AttendanceApp") ||
                deviceName.contains("Student"));
    }

    private void showErrorDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("Bluetooth permissions are required for attendance features")
                .setPositiveButton("OK", null)
                .show();
    }

    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
    })
    private void showExitScanningDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Stop Scanning?")
                .setMessage("Scanning is in progress. Do you want to stop and exit?")
                .setPositiveButton("Stop & Exit", (dialog, which) -> {
                    stopAttendanceScan();
                    finish();
                })
                .setNegativeButton("Continue Scanning", null)
                .show();
    }

    private void showSnackbar(String message, boolean isSuccess) {
        Snackbar snackbar = Snackbar.make(drawerLayout, message, Snackbar.LENGTH_LONG);
        if (!isSuccess) {
            snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.error_color));
        }
        snackbar.show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_logout) {
            logoutUser();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logoutUser() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);
        if (bluetoothHelper != null) {
            updateBluetoothStatus(bluetoothHelper.isBluetoothEnabled());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(bluetoothReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up active session
        if (activeSessionRef != null && attendanceListener != null) {
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            DatabaseReference attendanceRef = FirebaseDatabase.getInstance()
                    .getReference("attendanceRecords")
                    .child(currentDate)
                    .child(currentBranch)
                    .child(currentYear);
            attendanceRef.removeEventListener(attendanceListener);
        }

        // Stop scanning if active
        if (isScanning) {
            try {
                stopAttendanceScan();
            } catch (SecurityException e) {
                Log.e("TeacherDashboard", "Error stopping scan during cleanup", e);
            }
        }
    }

    // Data classes
    public static class AttendanceRecord {
        private String studentId;
        private String studentName;
        private String status;
        private Long timestamp;
        public AttendanceRecord() {}
        public AttendanceRecord(String studentId, String studentName, String status, Long timestamp) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.status = status;
            this.timestamp = timestamp;
        }

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }

        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }

    public static class StudentAttendanceRecord {
        private String date;
        private String subject;
        private String status;
        private Long timestamp;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
}