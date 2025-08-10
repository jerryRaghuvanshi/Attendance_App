package com.example.attendanceapp.Dashboards;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.attendanceapp.adaptors.ActiveSubjectAdapter;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.attendanceapp.LoginScreen.LoginActivity;
import com.example.attendanceapp.ProfileActivity;
import com.example.attendanceapp.R;
import com.example.attendanceapp.TimetableActivity;
import com.example.attendanceapp.adaptors.AttendanceHistoryAdapter;
import com.example.attendanceapp.bluetooth.BluetoothHelper;
import com.example.attendanceapp.models.AttendanceRecord;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StudentDashboard extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "StudentDashboard";

    private DrawerLayout drawerLayout;
    private PieChart pieChart;
    private long lastDiscoveryTime = 0;
    private TextView tvOverallPercentage, tvSubjectPercentage, tvSubjectName;
    private RecyclerView rvAttendanceHistory;
    private AttendanceHistoryAdapter adapter;
    private final Handler discoveryHandler = new Handler();
    private ActiveSubjectAdapter activeSubjectsAdapter;
    private TextView tvAttendanceStatus;
    private BluetoothHelper bluetoothHelper;
    private Button btnMarkPresent;
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 100;
    private String activeSessionId = null;
    private String activeSubject = null;
    private final Handler sessionCheckHandler = new Handler();
    private String studentBranch;
    private TextView tvActiveSessionCount;
    private String studentYear;
    private DatabaseReference activeSessionsRef;
    private RecyclerView rvActiveSubjects;
    private ValueEventListener activeSessionsListener;
    private final Set<String> devicesInRange = new HashSet<>();
    // Fixed: Separate lists for active and inactive sessions
    private final List<ActiveSession> activeSessions = new ArrayList<>();
    private final List<ActiveSession> inactiveSessions = new ArrayList<>();
    private final List<ActiveSession> allSessions = new ArrayList<>();

    private Button btnToggleView;
    private TextView tvOverallLabel, tvOverallStats, tvSubjectStats, tvPresentCount, tvAbsentCount;
    private TextInputLayout layoutSubjectSpinner;
    private boolean showingOverallView = true;
    private final List<String> availableSubjects = new ArrayList<>();
    private final String currentSelectedSubject = null;
    private Button btnActiveSessions, btnInactiveSessions;
    private LinearLayout emptySessionsLayout;
    private boolean showingActiveSessions = true;

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        initializeEnhancedViews();
        setupActiveSubjectsRecyclerView();
        setupAttendanceHistoryRecyclerView();
        setupPullToRefresh();


        bluetoothHelper = new BluetoothHelper(this);
        btnMarkPresent = findViewById(R.id.btnMarkPresent);
        btnMarkPresent.setOnClickListener(v -> markPresent());

        // Set up tab listeners
        btnActiveSessions.setOnClickListener(v -> showActiveSessions());
        btnInactiveSessions.setOnClickListener(v -> showInactiveSessions());

        loadStudentProfile();
        ensureBluetoothEnabled();

        // ✅ Check permissions before setting up Bluetooth
        if (hasBluetoothPermissions()) {
            setupBluetoothMonitoring();
            startRangeMonitoring();
            startContinuousDiscovery();
        } else {
            requestBluetoothPermissions();
        }
    }
    private void ensureBluetoothEnabled() {
        if (bluetoothHelper != null && !bluetoothHelper.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED) {
                startActivity(enableBtIntent);
            }
        }
    }
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
    })
    private void setupStudentBluetoothName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && bluetoothHelper != null) {
            String studentName = user.getDisplayName() != null ? user.getDisplayName() : "Student";
            String studentDeviceName = "Student-" + studentBranch + "-" + studentYear + "-" + studentName + "-" + user.getUid().substring(0, 6);
            bluetoothHelper.setBluetoothName(studentDeviceName);
            Log.d(TAG, "Set student device name: " + studentDeviceName);
        }
    }

    private boolean hasBluetoothPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    // Simplified Bluetooth receiver - just look for teacher devices
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && ActivityCompat.checkSelfPermission(StudentDashboard.this,
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {

                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress();

                    Log.d(TAG, "Device discovered: " + deviceName + " (" + deviceAddress + ")");

                    // Check if this is a teacher device
                    if (deviceName != null && deviceName.startsWith("AttendanceSession-")) {
                        Log.d(TAG, "Teacher device found: " + deviceName);
                        handleTeacherDeviceFound(deviceName, deviceAddress);
                    }
                }
            }
        }
    };

    private void handleTeacherDeviceFound(String deviceName, String deviceAddress) {
        try {
            // Parse session info from device name
            String[] parts = deviceName.split("-");
            if (parts.length >= 3) {
                String sessionId = parts[1];
                String subject = parts[2];

                Log.d(TAG, "Session detected - ID: " + sessionId + ", Subject: " + subject);

                // Add to devices in range
                devicesInRange.add(deviceAddress);
                lastDiscoveryTime = System.currentTimeMillis();

                // Update active session
                activeSessionId = sessionId;
                activeSubject = subject;

                // Update UI
                runOnUiThread(() -> {
                    tvAttendanceStatus.setText("Teacher session found: " + subject);
                    tvAttendanceStatus.setTextColor(ContextCompat.getColor(this, R.color.green_600));

                    btnMarkPresent.setVisibility(View.VISIBLE);
                    btnMarkPresent.setEnabled(true);
                    btnMarkPresent.setText("Mark Present for " + subject);
                    btnMarkPresent.setBackgroundColor(ContextCompat.getColor(this, R.color.green_600));
                });

                // Update session data if exists
                updateSessionInRange(sessionId, deviceAddress);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling teacher device", e);
        }
    }

    private void updateSessionInRange(String sessionId, String deviceAddress) {
        for (ActiveSession session : allSessions) {
            if (session.getSessionId() != null && session.getSessionId().equals(sessionId)) {
                session.setInBluetoothRange(true);
                session.setTeacherDeviceAddress(deviceAddress);

                runOnUiThread(() -> {
                    if (activeSubjectsAdapter != null) {
                        activeSubjectsAdapter.notifyDataSetChanged();
                    }
                });
                break;
            }
        }
    }

    private void requestBluetoothPermissions() {
        String[] permissions = {
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION // Required for Bluetooth discovery
        };

        ActivityCompat.requestPermissions(this, permissions, BLUETOOTH_PERMISSION_REQUEST_CODE);
    }

    private void startRangeMonitoring() {
        Handler rangeHandler = new Handler(Looper.getMainLooper());

        Runnable rangeChecker = new Runnable() {
            @Override
            public void run() {
                // Simple timeout - clear session if no discovery in last 60 seconds
                long currentTime = System.currentTimeMillis();
                if ((currentTime - lastDiscoveryTime) > 60000) {
                    devicesInRange.clear();
                    activeSessionId = null;
                    activeSubject = null;

                    runOnUiThread(() -> {
                        tvAttendanceStatus.setText("Searching for teacher sessions...");
                        tvAttendanceStatus.setTextColor(ContextCompat.getColor(StudentDashboard.this, R.color.orange_600));
                        btnMarkPresent.setVisibility(View.GONE);
                    });
                }

                // Check every 10 seconds
                rangeHandler.postDelayed(this, 10000);
            }
        };

        rangeHandler.post(rangeChecker);
    }
    private void updateRangeStatus(boolean inRange, int rssi, String deviceName) {
        runOnUiThread(() -> {
            if (inRange && deviceName != null) {
                // Extract subject from device name
                String[] parts = deviceName.split("-");
                String subject = parts.length > 2 ? parts[2] : "Unknown";

                String rangeMessage = "📶 In range: " + subject + " (Signal: " + rssi + ")";
                tvAttendanceStatus.setText(rangeMessage);
                tvAttendanceStatus.setTextColor(ContextCompat.getColor(this, R.color.green_600));

                if (btnMarkPresent != null) {
                    btnMarkPresent.setEnabled(true);
                    btnMarkPresent.setText("Mark Present");
                    btnMarkPresent.setBackgroundColor(ContextCompat.getColor(this, R.color.blue_600));
                }
            } else {
                tvAttendanceStatus.setText("📡 Searching for teacher sessions...");
                tvAttendanceStatus.setTextColor(ContextCompat.getColor(this, R.color.orange_600));

                if (btnMarkPresent != null) {
                    btnMarkPresent.setEnabled(false);
                    btnMarkPresent.setText("Out of Range");
                    btnMarkPresent.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_400));
                }
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Log.d(TAG, "All Bluetooth permissions granted");
                setupBluetoothMonitoring();
            } else {
                Log.w(TAG, "Bluetooth permissions denied");
                showSnackbar("Bluetooth permissions are required for attendance marking");
            }
        }
    }
    private void setupActiveSubjectsRecyclerView() {
        RecyclerView rvActiveSubjects = findViewById(R.id.rvActiveSubjects);
        rvActiveSubjects.setLayoutManager(new LinearLayoutManager(this));
        // Replace the current initialization with:
        activeSubjectsAdapter = new ActiveSubjectAdapter(new ArrayList<>(), new ActiveSubjectAdapter.OnSessionClickListener() {
            @Override
            public void onSessionClick(ActiveSession session) {
                onActiveSessionClick(session);
            }

            @Override
            public void onMarkPresentClick(ActiveSession session) {
                markAttendanceForSession(session);
            }

            @Override
            public void onMarkAttendanceClick(ActiveSession session) {
                markAttendanceForSession(session);
            }
        });        rvActiveSubjects.setAdapter(activeSubjectsAdapter);
    }


    private void showSessionDetails(ActiveSession session) {
        Toast.makeText(this, "Session: " + session.getSubject() + " by " + session.getTeacherName(), Toast.LENGTH_SHORT).show();
    }

    private void markAttendanceForSession(ActiveSession session) {
        if (session.isActive() && session.isInBluetoothRange()) {
            tvAttendanceStatus.setText("Marking attendance...");
            markPresent();
        } else {
            showSnackbar("Cannot mark attendance - session not available or out of range");
        }
    }

    private void initializeEnhancedViews() {
        btnToggleView = findViewById(R.id.btnToggleView);
        tvOverallLabel = findViewById(R.id.tvOverallLabel);
        tvOverallStats = findViewById(R.id.tvOverallStats);
        tvSubjectStats = findViewById(R.id.tvSubjectStats);
        tvPresentCount = findViewById(R.id.tvPresentCount);
        tvAbsentCount = findViewById(R.id.tvAbsentCount);
        tvSubjectName = findViewById(R.id.tvSubjectName);
        rvActiveSubjects = findViewById(R.id.rvActiveSubjects);
        tvSubjectPercentage = findViewById(R.id.tvSubjectPercentage);
        tvOverallPercentage = findViewById(R.id.tvOverallPercentage);
        LinearLayout layoutSubjectAttendance = findViewById(R.id.layoutSubjectAttendance);
        layoutSubjectSpinner = findViewById(R.id.layoutSubjectSpinner);
        AutoCompleteTextView spinnerSubject = findViewById(R.id.spinnerSubject);
        pieChart = findViewById(R.id.pieChart);
        tvAttendanceStatus = findViewById(R.id.tvAttendanceStatus);
        btnActiveSessions = findViewById(R.id.btnActiveSessions);
        btnInactiveSessions = findViewById(R.id.btnInactiveSessions);
        emptySessionsLayout = findViewById(R.id.emptySessionsLayout);

        setupPieChart();
        setupEnhancedUIListeners();
    }

    private void showActiveSessions() {
        Log.d(TAG, "Showing active sessions. Count: " + activeSessions.size());

        showingActiveSessions = true;
        updateTabStyles();

        List<ActiveSession> filteredActiveSessions = new ArrayList<>();
        long now = System.currentTimeMillis();
        long sessionTimeout = 5 * 60 * 1000;

        for (ActiveSession session : allSessions) {
            if (session.isActive() && (now - session.getStartTime() <= sessionTimeout)) {
                filteredActiveSessions.add(session);
            }
        }


        List<ActiveSession> currentData = activeSubjectsAdapter.getSessions();
        // In showActiveSessions()
        if (!filteredActiveSessions.equals(currentData)) {
            activeSubjectsAdapter.setSessions(filteredActiveSessions);
            // Use specific notification instead
            if (filteredActiveSessions.size() != currentData.size()) {
                activeSubjectsAdapter.notifyDataSetChanged(); // Only when size changes
            } else {
                activeSubjectsAdapter.notifyItemRangeChanged(0, filteredActiveSessions.size());
            }
        }
        updateActiveSessionCountUI();

        if (filteredActiveSessions.isEmpty()) {
            emptySessionsLayout.setVisibility(View.VISIBLE);
        } else {
            emptySessionsLayout.setVisibility(View.GONE);
        }
    }
    private void updateSubjectStatsUI(String subject, int present, int total) {
        runOnUiThread(() -> {
            int percentage = total > 0 ? (present * 100) / total : 0;

            tvSubjectName.setText(subject);
            tvSubjectPercentage.setText(getString(R.string.percentage_format, percentage));
            tvSubjectStats.setText(getString(R.string.sessions_format, present, total));

            // Update colors based on attendance percentage
            int color;
            if (percentage >= 75) {
                color = ContextCompat.getColor(this, R.color.green_600);
            } else if (percentage >= 50) {
                color = ContextCompat.getColor(this, R.color.orange_600);
            } else {
                color = ContextCompat.getColor(this, R.color.red_600);
            }
            tvSubjectPercentage.setTextColor(color);

            // Update pie chart for this subject
            updateSubjectPieChart(present, total - present, subject);
        });
    }
    private void loadSubjectAttendanceStats(String subject) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || studentBranch == null || studentYear == null) {
            showErrorMessage("Unable to load subject data");
            return;
        }

        DatabaseReference attendanceRef = FirebaseDatabase.getInstance()
                .getReference("attendanceRecords");

        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int subjectPresent = 0;
                int subjectTotal = 0;
                List<AttendanceRecord> subjectHistory = new ArrayList<>();

                // Calculate subject-specific attendance
                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    String date = dateSnapshot.getKey();

                    // Look for sessions of this subject
                    for (DataSnapshot sessionSnapshot : dateSnapshot.getChildren()) {
                        String sessionSubject = sessionSnapshot.child("subject").getValue(String.class);

                        if (subject.equals(sessionSubject)) {
                            subjectTotal++;

                            if (sessionSnapshot.hasChild(user.getUid())) {
                                subjectPresent++;

                                // Add to history
                                DataSnapshot studentRecord = sessionSnapshot.child(user.getUid());
                                Long timestamp = studentRecord.child("timestamp").getValue(Long.class);
                                String timeStr = timestamp != null ?
                                        new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp)) :
                                        "Unknown";

                                subjectHistory.add(new AttendanceRecord(date, subject, timeStr));
                            } else {
                                // Add absent record
                                subjectHistory.add(new AttendanceRecord(date, subject, "Absent"));
                            }
                        }
                    }
                }

                // Update UI with subject-specific data
                updateSubjectStatsUI(subject, subjectPresent, subjectTotal);

                // Update history
                runOnUiThread(() -> {
                    if (adapter != null) {
                        adapter.updateData(subjectHistory);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading subject data", error.toException());
                showErrorMessage("Failed to load subject attendance data");
            }
        });
    }
    private boolean canMarkAttendance(ActiveSession session) {
        if (session == null) {
            Log.w(TAG, "Session is null");
            return false;
        }

        if (!session.isActive()) {
            Log.w(TAG, "Session is not active");
            return false;
        }

        if (!session.isInBluetoothRange()) {
            Log.w(TAG, "Session is not in Bluetooth range");
            return false;
        }

        // Check if session is for this student's branch and year
        if (!studentBranch.equals(session.getBranch()) || !studentYear.equals(session.getYear())) {
            Log.w(TAG, "Session is not for this student's branch/year");
            return false;
        }

        // Check if session hasn't expired (within 5 minutes of start time)
        long currentTime = System.currentTimeMillis();
        long sessionTimeout = 5 * 60 * 1000; // 5 minutes
        if (currentTime - session.getStartTime() > sessionTimeout) {
            Log.w(TAG, "Session has expired");
            return false;
        }

        return true;
    }

    private void refreshAllData() {
        if (showingOverallView) {
            loadAttendanceData();
        } else if (currentSelectedSubject != null) {
            loadSubjectAttendanceStats(currentSelectedSubject);
        }

        // Refresh active sessions
        loadActiveSessions();

        // Update session count
        updateActiveSessionCountUI();

        Log.d(TAG, "All data refreshed");
    }
    private void checkExistingAttendance(String sessionId, OnAttendanceCheckListener listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            listener.onResult(false, "User not logged in");
            return;
        }

        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DatabaseReference attendanceRef = FirebaseDatabase.getInstance()
                .getReference("attendanceRecords")
                .child(currentDate)
                .child(sessionId)
                .child(user.getUid());

        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean alreadyMarked = snapshot.exists();
                String message = alreadyMarked ? "Attendance already marked for today" : "Can mark attendance";
                listener.onResult(alreadyMarked, message);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onResult(false, "Error checking attendance: " + error.getMessage());
            }
        });
    }
    private interface OnAttendanceCheckListener {
        void onResult(boolean alreadyMarked, String message);
    }

    private void showInactiveSessions() {
        Log.d(TAG, "Showing inactive sessions");

        showingActiveSessions = false;
        updateTabStyles();

        // Filter for truly inactive sessions
        List<ActiveSession> filteredInactiveSessions = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (ActiveSession session : allSessions) {
            // Session is inactive if:
            // 1. active flag is false, OR
            // 2. has endTime set, OR
            // 3. started more than 5 minutes ago and still marked active
            boolean isInactive = !session.isActive() ||
                    session.getEndTime() > 0 ||
                    (now - session.getStartTime() > 5 * 60 * 1000);

            if (isInactive) {
                filteredInactiveSessions.add(session);
            }
        }

        // Sort by start time (most recent first)
        filteredInactiveSessions.sort((a, b) -> Long.compare(b.getStartTime(), a.getStartTime()));

        activeSubjectsAdapter.setSessions(filteredInactiveSessions);
        activeSubjectsAdapter.notifyDataSetChanged();

        // Update UI
        updateActiveSessionCountUI();

        if (filteredInactiveSessions.isEmpty()) {
            emptySessionsLayout.setVisibility(View.VISIBLE);
            findViewById(R.id.rvActiveSubjects).setVisibility(View.GONE);
        } else {
            emptySessionsLayout.setVisibility(View.GONE);
            findViewById(R.id.rvActiveSubjects).setVisibility(View.VISIBLE);
        }

        Log.d(TAG, "Filtered inactive sessions count: " + filteredInactiveSessions.size());
    }

    private void updateActiveSessionCountUI() {
        int activeCount = 0;
        for (ActiveSession session : allSessions) {
            if (session.isActive() && (System.currentTimeMillis() - session.getStartTime() <= 5 * 60 * 1000)) {
                activeCount++;
            }
        }

        if (tvActiveSessionCount != null) {
            // ✅ Use string resource
            tvActiveSessionCount.setText(getString(R.string.active_sessions_count, activeCount));
        }

        if (tvAttendanceStatus != null) {
            if (activeCount > 0) {
                tvAttendanceStatus.setText(getString(R.string.ready_to_mark_attendance, String.valueOf(activeCount)));
            } else {
                tvAttendanceStatus.setText(R.string.no_active_sessions);
            }
        }
    }
    private void loadStudentProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.d(TAG, "Loading student profile for user: " + user.getUid());

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            studentBranch = documentSnapshot.getString("branch");
                            studentYear = documentSnapshot.getString("year");

                            Log.d(TAG, "Student profile loaded - Branch: " + studentBranch + ", Year: " + studentYear);

                            if (studentBranch != null && studentYear != null) {
                                // Load sessions after profile is loaded
                                loadActiveSessions();
                                loadAttendanceData();
                                // Start with active sessions view
                                showActiveSessions();
                            } else {
                                Log.e(TAG, "Branch or year is null in profile");
                                showErrorMessage("Profile incomplete. Please contact admin.");
                            }
                        } else {
                            Log.e(TAG, "Student profile document doesn't exist");
                            showErrorMessage("Profile not found. Please contact admin.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading student profile", e);
                        showErrorMessage("Error loading profile. Please try again.");
                    });
        } else {
            Log.e(TAG, "No authenticated user found");
            showErrorMessage("Please login first.");
        }
    }

    // Fixed: Improved session loading with proper error handling and logging
    private void loadActiveSessions() {
        if (studentBranch == null || studentYear == null) {
            Log.e(TAG, "Cannot load sessions - Branch: " + studentBranch + ", Year: " + studentYear);
            return;
        }

        Log.d(TAG, "Loading active sessions for branch: " + studentBranch + ", year: " + studentYear);

        // Remove existing listener if present
        if (activeSessionsRef != null && activeSessionsListener != null) {
            activeSessionsRef.removeEventListener(activeSessionsListener);
        }

        activeSessionsRef = FirebaseDatabase.getInstance()
                .getReference("activeSessions")
                .child(studentBranch)
                .child(studentYear);

        activeSessionsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Active sessions data changed. Snapshot exists: " + snapshot.exists());
                Log.d(TAG, "Number of session snapshots: " + snapshot.getChildrenCount());

                // Clear all session lists
                allSessions.clear();
                activeSessions.clear();
                inactiveSessions.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot sessionSnapshot : snapshot.getChildren()) {
                        ActiveSession session = sessionSnapshot.getValue(ActiveSession.class);
                        if (session != null) {
                            // Set the session ID from the key
                            session.setSessionId(sessionSnapshot.getKey());
                            session.setInBluetoothRange(false);

                            // Add to all sessions list
                            allSessions.add(session);

                            Log.d(TAG, "Session loaded - ID: " + session.getSessionId() +
                                    ", Subject: " + session.getSubject() +
                                    ", Active: " + session.isActive() +
                                    ", Branch: " + session.getBranch() +
                                    ", Year: " + session.getYear());

                            // Categorize sessions
                            if (session.isActive()) {
                                activeSessions.add(session);
                            } else {
                                // Only add recent inactive sessions (within 24 hours)
                                long now = System.currentTimeMillis();
                                long twentyFourHours = 24 * 60 * 60 * 1000;
                                if (now - session.getStartTime() <= twentyFourHours) {
                                    inactiveSessions.add(session);
                                }
                            }
                        } else {
                            Log.w(TAG, "Session data is null for key: " + sessionSnapshot.getKey());
                        }
                    }
                } else {
                    Log.d(TAG, "No active sessions found in database");
                }

                Log.d(TAG, "Sessions categorized - Active: " + activeSessions.size() +
                        ", Inactive: " + inactiveSessions.size() +
                        ", Total: " + allSessions.size());

                // Update UI on main thread
                runOnUiThread(() -> {
                    updateActiveSessionCountUI();

                    // Refresh current view
                    if (showingActiveSessions) {
                        showActiveSessions();
                    } else {
                        showInactiveSessions();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading active sessions: " + error.getMessage(), error.toException());
                runOnUiThread(() -> showErrorMessage("Error loading sessions: " + error.getMessage()));
            }
        };

        // Add the listener
        activeSessionsRef.addValueEventListener(activeSessionsListener);

        Log.d(TAG, "Active sessions listener attached to: " + activeSessionsRef.getRef());
    }

    private void updateTabStyles() {
        if (showingActiveSessions) {
            btnActiveSessions.setBackgroundColor(ContextCompat.getColor(this, R.color.green_50));
            btnActiveSessions.setTextColor(ContextCompat.getColor(this, R.color.green_700));
            btnActiveSessions.setTypeface(null, Typeface.BOLD);

            btnInactiveSessions.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_100));
            btnInactiveSessions.setTextColor(ContextCompat.getColor(this, R.color.gray_600));
            btnInactiveSessions.setTypeface(null, Typeface.NORMAL);
        } else {
            btnActiveSessions.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_100));
            btnActiveSessions.setTextColor(ContextCompat.getColor(this, R.color.gray_600));
            btnActiveSessions.setTypeface(null, Typeface.NORMAL);

            btnInactiveSessions.setBackgroundColor(ContextCompat.getColor(this, R.color.blue_50));
            btnInactiveSessions.setTextColor(ContextCompat.getColor(this, R.color.blue_700));
            btnInactiveSessions.setTypeface(null, Typeface.BOLD);
        }
    }

    private void onActiveSessionClick(ActiveSession session) {
        showSessionDetails(session);
        Log.d(TAG, "Session clicked - ID: " + session.getSessionId() +
                ", Subject: " + session.getSubject() +
                ", Active: " + session.isActive());

        if (session.isActive() &&
                session.getBranch() != null && session.getBranch().equals(studentBranch) &&
                session.getYear() != null && session.getYear().equals(studentYear)) {

            activeSessionId = session.getSessionId();
            activeSubject = session.getSubject();
            btnMarkPresent.setVisibility(View.VISIBLE);

            // ✅ Fixed: Use string resource with placeholder
            tvAttendanceStatus.setText(getString(R.string.ready_to_mark_attendance, activeSubject));
            tvAttendanceStatus.setVisibility(View.VISIBLE);

            Log.d(TAG, "Session selected for attendance: " + activeSubject);
        } else if (!session.isActive()) {
            Toast.makeText(this, "This session is not active", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Session not available for your class", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupEnhancedUIListeners() {
        btnToggleView.setOnClickListener(v -> {
            showingOverallView = !showingOverallView;
            if (showingOverallView) {
                btnToggleView.setText(R.string.overall);
                tvOverallLabel.setText(R.string.overall);
                layoutSubjectSpinner.setVisibility(View.GONE);
            } else {
                btnToggleView.setText(R.string.by_subject);
                tvOverallLabel.setText(R.string.by_subject);
                layoutSubjectSpinner.setVisibility(View.VISIBLE);
            }
            animateViewTransition();
        });
    }

    private void updateOverallAttendanceUI(int totalPresent, int totalSessions,
                                           Map<String, Integer> subjectSessions,
                                           Map<String, Integer> studentAttendance) {
        runOnUiThread(() -> {
            int overallPercentage = totalSessions > 0 ? (totalPresent * 100) / totalSessions : 0;

            // ✅ Fixed: Use string resources
            tvOverallPercentage.setText(getString(R.string.percentage_format, overallPercentage));
            tvOverallStats.setText(getString(R.string.sessions_format, totalPresent, totalSessions));
            tvPresentCount.setText(getString(R.string.present_count, totalPresent));
            tvAbsentCount.setText(getString(R.string.absent_count, totalSessions - totalPresent));

            updateOverallPieChart(totalPresent, totalSessions - totalPresent);

            String bestSubject = findBestSubject(subjectSessions, studentAttendance);
            if (bestSubject != null) {
                updateSubjectDisplay(bestSubject, subjectSessions, studentAttendance);
            }

            updateAttendanceStatusColor(overallPercentage);
            updateCardBackground(overallPercentage);
        });
    }
    // Add debug method to test session creation
    private void testCreateSession() {
        if (studentBranch != null && studentYear != null) {
            DatabaseReference testRef = FirebaseDatabase.getInstance()
                    .getReference("activeSessions")
                    .child(studentBranch)
                    .child(studentYear)
                    .push();

            ActiveSession testSession = new ActiveSession();
            testSession.setSessionId(testRef.getKey());
            testSession.setSubject("Test Subject");
            testSession.setTeacherName("Test Teacher");
            testSession.setActive(true);
            testSession.setStartTime(System.currentTimeMillis());
            testSession.setBranch(studentBranch);
            testSession.setYear(studentYear);

            testRef.setValue(testSession)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Test session created successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error creating test session", e));
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startBluetoothDiscovery() {
        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "Missing Bluetooth permissions");
            requestBluetoothPermissions();
            return;
        }

        if (!bluetoothHelper.isBluetoothEnabled()) {
            Log.w(TAG, "Bluetooth not enabled");
            showErrorMessage("Please enable Bluetooth to detect sessions");
            return;
        }

        // Clear previous discovery results
        devicesInRange.clear();

        // Reset all sessions' range status
        for (ActiveSession session : allSessions) {
            session.setInBluetoothRange(false);
        }

        try {
            bluetoothHelper.cancelDiscovery();
            Thread.sleep(100);

            bluetoothHelper.startDiscovery(); // Fixed: Removed boolean assignment
            Log.d(TAG, "Bluetooth discovery started successfully");

            runOnUiThread(() -> {
                if (activeSubjectsAdapter != null) {
                    activeSubjectsAdapter.notifyDataSetChanged();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error starting Bluetooth discovery", e);
            showErrorMessage("Error starting device discovery: " + e.getMessage());
        }

        // Schedule next discovery cycle
        sessionCheckHandler.removeCallbacks(this::startBluetoothDiscovery);
        sessionCheckHandler.postDelayed(this::startBluetoothDiscovery, 15000); // Every 15 seconds
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop discovery
        if (discoveryHandler != null) {
            discoveryHandler.removeCallbacksAndMessages(null);
        }

        // Unregister Bluetooth receiver
        try {
            unregisterReceiver(bluetoothReceiver);
        } catch (IllegalArgumentException e) {
            // Not registered
        }

        // Remove listeners
        if (activeSessionsRef != null && activeSessionsListener != null) {
            activeSessionsRef.removeEventListener(activeSessionsListener);
        }

        // Cancel Bluetooth discovery
        if (bluetoothHelper != null) {
            try {
                bluetoothHelper.cancelDiscovery();
            } catch (SecurityException e) {
                Log.e(TAG, "Permission denied for canceling discovery", e);
            }
        }
    }

    private void setupSubjectSpinner() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && studentBranch != null && studentYear != null) {


            activeSessionsListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    allSessions.clear();
                    activeSessions.clear();
                    inactiveSessions.clear();

                    for (DataSnapshot sessionSnapshot : snapshot.getChildren()) {
                        ActiveSession session = sessionSnapshot.getValue(ActiveSession.class);
                        if (session != null) {
                            session.setSessionId(sessionSnapshot.getKey());
                            allSessions.add(session);

                            // Check if session is truly active based on timing and status
                            long now = System.currentTimeMillis();
                            long sessionTimeout = 5 * 60 * 1000; // 5 minutes

                            boolean isCurrentlyActive = session.isActive() &&
                                    (session.getEndTime() == 0 || session.getEndTime() > now) &&
                                    (now - session.getStartTime()) < sessionTimeout;

                            if (isCurrentlyActive) {
                                activeSessions.add(session);
                            } else {
                                inactiveSessions.add(session);
                            }
                        }
                    }

                    runOnUiThread(() -> {
                        if (showingActiveSessions) {
                            showActiveSessions();
                        } else {
                            showInactiveSessions();
                        }
                    });

                    Log.d(TAG, "Sessions loaded - Total: " + allSessions.size() +
                            ", Active: " + activeSessions.size() +
                            ", Inactive: " + inactiveSessions.size());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading sessions", error.toException());
                }
            };
        }
    }


    private void updateAttendanceStatusColor(int percentage) {
        int color;
        if (percentage >= 75) {
            color = ContextCompat.getColor(this, R.color.green_600);
        } else if (percentage >= 50) {
            color = ContextCompat.getColor(this, R.color.orange_600);
        } else {
            color = ContextCompat.getColor(this, R.color.red_600);
        }

        // Fix: Use string resource instead of concatenation
        tvOverallPercentage.setText(String.valueOf(percentage) + "%");
        tvOverallPercentage.setTextColor(color);
    }
    private void updateCardBackground(int percentage) {
        int backgroundColor;
        if (percentage >= 75) {
            backgroundColor = ContextCompat.getColor(this, R.color.green_50);
        } else if (percentage >= 60) {
            backgroundColor = ContextCompat.getColor(this, R.color.orange_50);
        } else {
            backgroundColor = ContextCompat.getColor(this, R.color.red_50);
        }

        // Update the attendance card background
        View attendanceCard = findViewById(R.id.layoutSubjectAttendance);
        if (attendanceCard != null) {
            attendanceCard.setBackgroundColor(backgroundColor);
        }
    }
    private String findBestSubject(Map<String, Integer> subjectSessions,
                                   Map<String, Integer> studentAttendance) {
        String bestSubject = null;
        double bestPercentage = -1;

        for (String subject : subjectSessions.keySet()) {
            int total = subjectSessions.getOrDefault(subject, 0);
            int present = studentAttendance.getOrDefault(subject, 0);

            if (total > 0) {
                double percentage = (present * 100.0) / total;
                if (percentage > bestPercentage) {
                    bestPercentage = percentage;
                    bestSubject = subject;
                }
            }
        }

        return bestSubject;
    }

    private void updateSubjectDisplay(String subject, Map<String, Integer> subjectSessions,
                                      Map<String, Integer> studentAttendance) {
        int subjectTotal = subjectSessions.getOrDefault(subject, 0);
        int subjectPresent = studentAttendance.getOrDefault(subject, 0);
        int subjectPercentage = subjectTotal > 0 ? (subjectPresent * 100) / subjectTotal : 0;

        tvSubjectName.setText(subject);
        // ✅ Use string resources
        tvSubjectPercentage.setText(getString(R.string.percentage_format, subjectPercentage));
        tvSubjectStats.setText(getString(R.string.sessions_format, subjectPresent, subjectTotal));

        // Update subject percentage color
        int color;
        if (subjectPercentage >= 75) {
            color = ContextCompat.getColor(this, R.color.green_600);
        } else if (subjectPercentage >= 50) {
            color = ContextCompat.getColor(this, R.color.orange_600);
        } else {
            color = ContextCompat.getColor(this, R.color.red_600);
        }
        tvSubjectPercentage.setTextColor(color);
    }

    private void showSubjectSpecificChart(String subject) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || studentBranch == null || studentYear == null) {
            showErrorMessage("Unable to load subject data");
            return;
        }

        DatabaseReference attendanceRef = FirebaseDatabase.getInstance()
                .getReference("attendanceRecords");

        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int subjectPresent = 0;
                int subjectTotal = 0;
                List<AttendanceRecord> subjectHistory = new ArrayList<>();

                // Calculate subject-specific attendance
                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    String date = dateSnapshot.getKey();
                    DataSnapshot subjectSnapshot = dateSnapshot
                            .child(studentBranch)
                            .child(studentYear)
                            .child(subject);

                    if (subjectSnapshot.exists()) {
                        subjectTotal++;
                        if (subjectSnapshot.hasChild(user.getUid())) {
                            subjectPresent++;

                            // Add to history
                            DataSnapshot studentRecord = subjectSnapshot.child(user.getUid());
                            String time = studentRecord.child("timestamp").getValue(String.class);
                            subjectHistory.add(new AttendanceRecord(date, subject, time));
                        } else {
                            // Add absent record
                            subjectHistory.add(new AttendanceRecord(date, subject, "--"));
                        }
                    }
                }

                // Update UI with subject-specific data
                int finalSubjectPresent = subjectPresent;
                int finalSubjectTotal = subjectTotal;
                runOnUiThread(() -> updateSubjectSpecificUI(subject, finalSubjectPresent, finalSubjectTotal, subjectHistory));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StudentDashboard", "Error loading subject data", error.toException());
                showErrorMessage("Failed to load subject attendance data");
            }
        });
    }


    private void updateSubjectSpecificUI(String subject, int present, int total,
                                         List<AttendanceRecord> history) {
        int percentage = total > 0 ? (present * 100) / total : 0;

        // Update main display
        tvOverallLabel.setText(subject);
        tvOverallPercentage.setText(getString(R.string.percentage_format, percentage));
        tvOverallStats.setText(getString(R.string.sessions_format, present, total));

        // Update legend
        tvPresentCount.setText(getString(R.string.present_count, present));
        tvAbsentCount.setText(getString(R.string.absent_count, total - present));

        // Update pie chart
        updateSubjectPieChart(present, total - present, subject);

        // ✅ Fix: Use existing tvSubjectStats instead of non-existent tvRecentSession
        if (!history.isEmpty()) {
            AttendanceRecord recent = history.get(0);
            // Show recent session info in the subject stats area
            tvSubjectStats.setText(getString(R.string.recent_session, recent.getDate()));
        }

        updateAttendanceStatusColor(percentage);

        if (rvAttendanceHistory != null && rvAttendanceHistory.getAdapter() != null) {
            ((AttendanceHistoryAdapter) rvAttendanceHistory.getAdapter()).updateData(history);
        }
    }
    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterTextSize(12f);
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(false);
        pieChart.setHighlightPerTapEnabled(true);

        // Remove legend (we have custom legend)
        pieChart.getLegend().setEnabled(false);

        // Remove entry labels
        pieChart.setDrawEntryLabels(false);
    }
    private void updateOverallPieChart(int present, int absent) {
        List<PieEntry> entries = new ArrayList<>();

        if (present + absent > 0) {
            entries.add(new PieEntry(present, "Present"));
            if (absent > 0) {
                entries.add(new PieEntry(absent, "Absent"));
            }
        } else {
            entries.add(new PieEntry(1, "No Data"));
        }

        updatePieChart(entries, "Overall\nAttendance");
    }

    private void updateSubjectPieChart(int present, int absent, String subject) {
        List<PieEntry> entries = new ArrayList<>();

        if (present + absent > 0) {
            entries.add(new PieEntry(present, "Present"));
            if (absent > 0) {
                entries.add(new PieEntry(absent, "Absent"));
            }
        } else {
            entries.add(new PieEntry(1, "No Data"));
        }

        updatePieChart(entries, subject + "\nAttendance");
    }

    private void updatePieChart(List<PieEntry> entries, String centerText) {
        PieDataSet dataSet = new PieDataSet(entries, "");

        // Set colors
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(ContextCompat.getColor(this, R.color.green_600));
        colors.add(ContextCompat.getColor(this, R.color.red_600));
        colors.add(ContextCompat.getColor(this, R.color.gray_400)); // For "No Data"
        dataSet.setColors(colors);

        // Configure dataset
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(5f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));

        pieChart.setData(data);
        pieChart.setCenterText(centerText);
        pieChart.invalidate();

        // Animate chart
        pieChart.animateY(1000, Easing.EaseInOutQuad);
    }

    private void animateViewTransition() {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(pieChart, "alpha", 1f, 0f);
        fadeOut.setDuration(200);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(pieChart, "alpha", 0f, 1f);
        fadeIn.setDuration(200);
        fadeIn.setStartDelay(200);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(fadeOut, fadeIn);
        animatorSet.start();
    }
    private void animateCardClick(View card) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(card, "scaleX", 1f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(card, "scaleY", 1f, 0.95f, 1f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(150);
        animatorSet.start();
    }

    private void showErrorMessage(String message) {
        runOnUiThread(() -> {
            if (tvAttendanceStatus != null) {
                tvAttendanceStatus.setText(message);
                tvAttendanceStatus.setTextColor(ContextCompat.getColor(this, R.color.red_600));
            }

            // Show snackbar
            View rootView = findViewById(android.R.id.content);
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
        });
    }

    public void refreshAttendanceData() {
        if (showingOverallView) {
            loadAttendanceData();
        } else if (currentSelectedSubject != null) {
            showSubjectSpecificChart(currentSelectedSubject);
        }
    }

    private void setupPullToRefresh() {
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                refreshAttendanceData();
                swipeRefreshLayout.setRefreshing(false);
            });

            swipeRefreshLayout.setColorSchemeResources(
                    R.color.green_600,
                    R.color.blue_600,
                    R.color.orange_600
            );
        }
    }



    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
    })
    private void setupBluetoothMonitoring() {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }

        if (bluetoothHelper == null) {
            bluetoothHelper = new BluetoothHelper(this);
        }

        // Ensure Bluetooth is enabled
        if (!bluetoothHelper.isBluetoothEnabled()) {
            enableBluetooth();
            return;
        }

        // Set up intent filter for device discovery
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        // Unregister any existing receiver
        try {
            unregisterReceiver(bluetoothReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }

        // Register the receiver
        registerReceiver(bluetoothReceiver, filter);
        Log.d(TAG, "Bluetooth receiver registered");

        // Start continuous discovery
        startContinuousBluetoothDiscovery();
    }

    private void enableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED) {
            startActivityForResult(enableBtIntent, 1001);
        }
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startContinuousBluetoothDiscovery() {
        discoveryHandler.post(new Runnable() {
            @Override
            public void run() {
                if (bluetoothHelper != null && bluetoothHelper.isBluetoothEnabled()) {
                    try {
                        // Cancel any ongoing discovery
                        bluetoothHelper.cancelDiscovery();

                        // Wait before starting new discovery
                        Thread.sleep(1000);

                        // Start discovery
                        boolean started = bluetoothHelper.startDiscovery();
                        Log.d(TAG, "Bluetooth discovery started: " + started);

                        if (!started) {
                            Log.w(TAG, "Failed to start Bluetooth discovery");
                        }
                    } catch (SecurityException e) {
                        Log.e(TAG, "Permission denied for Bluetooth discovery", e);
                        requestBluetoothPermissions();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Thread interrupted", e);
                    }

                    // Schedule next discovery in 20 seconds
                    discoveryHandler.postDelayed(this, 20000);
                }
            }
        });
    }
    private void showSnackbar(String message) {
        View rootView = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);

        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.red_600));

        snackbar.setTextColor(Color.WHITE);
        snackbar.show();
    }

    private void showSuccessMessage(String message) {
        runOnUiThread(() -> {
            tvAttendanceStatus.setText(message);
            tvAttendanceStatus.setTextColor(ContextCompat.getColor(this, R.color.green_600));
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    // Simplified continuous discovery
    // In your StudentDashboard, use the enhanced methods:
    private void startContinuousDiscovery() {
        discoveryHandler.post(new Runnable() {
            @Override
            public void run() {
                if (bluetoothHelper != null && bluetoothHelper.isBluetoothEnabled()) {
                    try {
                        if (ActivityCompat.checkSelfPermission(StudentDashboard.this,
                                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {

                            // Cancel any ongoing discovery first
                            bluetoothHelper.cancelDiscovery();

                            // Wait a bit before starting new discovery
                            Thread.sleep(500);

                            // Start discovery
                            boolean started = bluetoothHelper.startDiscovery();
                            Log.d(TAG, "Bluetooth discovery started: " + started);

                            if (!started) {
                                Log.w(TAG, "Failed to start Bluetooth discovery");
                            }
                        }
                    } catch (SecurityException e) {
                        Log.e(TAG, "Permission denied for Bluetooth discovery", e);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Thread interrupted", e);
                    }

                    // Schedule next discovery in 15 seconds
                    discoveryHandler.postDelayed(this, 15000);
                }
            }
        });
    }
    private void ensureBluetoothReady() {
        if (bluetoothHelper == null) {
            bluetoothHelper = new BluetoothHelper(this);
        }

        if (!bluetoothHelper.isBluetoothEnabled()) {
            Log.w(TAG, "Bluetooth not enabled - requesting enable");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableBtIntent, 1002);
            }
            return;
        }

        // Make sure device is discoverable
        makeDeviceDiscoverable();
    }

    private void makeDeviceDiscoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                == PackageManager.PERMISSION_GRANTED) {
            startActivity(discoverableIntent);
        }
    }

    // Remove complex range validation and simplify to just device discovery
    private void parseSessionInfo(String deviceName, String deviceAddress) {
        Log.d(TAG, "🔍 Parsing device: " + deviceName + " at " + deviceAddress);

        if (deviceName != null && deviceName.startsWith("AttendanceSession-")) {
            try {
                String[] parts = deviceName.split("-");
                Log.d(TAG, "Device parts: " + java.util.Arrays.toString(parts));

                if (parts.length >= 3) {
                    String sessionId = parts[1];
                    String subject = parts[2];
                    // Remove branch/year validation - just use the session info

                    Log.d(TAG, "Teacher device found - Session: " + sessionId + ", Subject: " + subject);

                    // Immediately mark as in range and available for attendance
                    updateTeacherDeviceFound(sessionId, subject, deviceAddress);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing session info: " + deviceName, e);
            }
        }
    }

    private void updateTeacherDeviceFound(String sessionId, String subject, String deviceAddress) {
        Log.d(TAG, "📡 Teacher device found for session: " + sessionId);

        // Set active session immediately
        activeSessionId = sessionId;
        activeSubject = subject;
        devicesInRange.add(deviceAddress);
        lastDiscoveryTime = System.currentTimeMillis();

        runOnUiThread(() -> {
            // Show session found and enable attendance button
            tvAttendanceStatus.setText("Session found: " + subject);
            tvAttendanceStatus.setTextColor(ContextCompat.getColor(this, R.color.green_600));

            btnMarkPresent.setText("Mark Present");
            btnMarkPresent.setVisibility(View.VISIBLE);
            btnMarkPresent.setEnabled(true);
            btnMarkPresent.setBackgroundColor(ContextCompat.getColor(this, R.color.green_600));
        });
    }
    private void updateSessionBluetoothInfo(String sessionId, String deviceAddress, String subject) {
        Log.d(TAG, "📡 Updating Bluetooth info for session: " + sessionId);

        boolean sessionFound = false;
        int sessionPosition = -1;

        for (int i = 0; i < allSessions.size(); i++) {
            ActiveSession session = allSessions.get(i);
            if (session.getSessionId() != null && session.getSessionId().equals(sessionId)) {
                session.setInBluetoothRange(true);
                session.setTeacherDeviceAddress(deviceAddress);
                devicesInRange.add(deviceAddress);
                sessionFound = true;
                sessionPosition = i;
                Log.d(TAG, "✅ Session " + sessionId + " marked as IN BLUETOOTH RANGE");
                break;
            }
        }

        if (!sessionFound) {
            Log.w(TAG, "⚠️ Session " + sessionId + " not found in allSessions list");
        }

        final boolean found = sessionFound;
        final int position = sessionPosition;

        runOnUiThread(() -> {
            if (activeSubjectsAdapter != null) {
                // ✅ More efficient: notify specific item change when possible
                if (position >= 0) {
                    activeSubjectsAdapter.notifyItemChanged(position);
                } else {
                    activeSubjectsAdapter.notifyDataSetChanged();
                }
            }
            updateActiveSessionCountUI();

            if (tvAttendanceStatus != null && found) {
                tvAttendanceStatus.setText(getString(R.string.teacher_device_detected, subject));
                tvAttendanceStatus.setTextColor(ContextCompat.getColor(this, R.color.green_600));
            }
        });
    }
    private void verifySessionWithDatabase() {
        DatabaseReference sessionRef = FirebaseDatabase.getInstance()
                .getReference("activeSessions")
                .child(studentBranch)
                .child(studentYear)
                .child(activeSessionId);

        sessionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.w(TAG, "Session not found in database");
                    runOnUiThread(() -> {
                        // ✅ Use string resource
                        tvAttendanceStatus.setText(R.string.invalid_session_detected);
                        tvAttendanceStatus.setTextColor(ContextCompat.getColor(
                                StudentDashboard.this, R.color.red_600));
                        btnMarkPresent.setVisibility(View.GONE);
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error verifying session", error.toException());
            }
        });
    }
    private void startSessionMonitoring() {
        // Check if session is still valid
        // Check every 30 seconds
        Runnable sessionCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (activeSessionId != null) {
                    // Check if session is still valid
                    verifySessionWithDatabase();
                }
                sessionCheckHandler.postDelayed(this, 30000); // Check every 30 seconds
            }
        };
        sessionCheckHandler.postDelayed(sessionCheckRunnable, 30000);
    }

    private void markPresent() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showSnackbar("Please login again");
            return;
        }

        // Simple check: if teacher device was discovered, allow attendance
        if (activeSessionId == null || activeSubject == null) {
            showSnackbar("No active session detected. Please wait for teacher to start session.");
            return;
        }

        // Show loading state
        runOnUiThread(() -> {
            btnMarkPresent.setText("Marking attendance...");
            btnMarkPresent.setEnabled(false);
        });

        String studentId = user.getUid();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DatabaseReference attendanceRef = FirebaseDatabase.getInstance()
                .getReference("attendanceRecords")
                .child(currentDate)
                .child(activeSessionId)
                .child(studentId);

        Map<String, Object> attendanceData = new HashMap<>();
        attendanceData.put("studentId", studentId);
        attendanceData.put("studentName", user.getDisplayName());
        attendanceData.put("sessionId", activeSessionId);
        attendanceData.put("subject", activeSubject);
        attendanceData.put("timestamp", System.currentTimeMillis());
        attendanceData.put("status", "Present");

        Log.d(TAG, "Marking attendance for: " + activeSubject + " on " + currentDate);

        attendanceRef.setValue(attendanceData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Attendance marked successfully");
                    runOnUiThread(() -> {
                        btnMarkPresent.setText("Attendance Marked ✓");
                        btnMarkPresent.setBackgroundColor(ContextCompat.getColor(this, R.color.green_600));
                        showSuccessMessage("Attendance marked successfully!");

                        // Disable button to prevent duplicate marking
                        btnMarkPresent.setEnabled(false);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error marking attendance", e);
                    runOnUiThread(() -> {
                        btnMarkPresent.setText("Mark Present");
                        btnMarkPresent.setEnabled(true);
                        showSnackbar("Failed to mark attendance. Please try again.");
                    });
                });
    }
    // In your setupActiveSessionsRecyclerView() method
    private void setupActiveSessionsRecyclerView() {
        rvActiveSubjects.setLayoutManager(new LinearLayoutManager(this));

        // ✅ Update adapter initialization with proper listener
        activeSubjectsAdapter = new ActiveSubjectAdapter(activeSessions, new ActiveSubjectAdapter.OnSessionClickListener() {
            @Override
            public void onSessionClick(StudentDashboard.ActiveSession session) {
                onActiveSessionClick(session);
            }

            @Override
            public void onMarkPresentClick(ActiveSession session) {

            }

            @Override
            public void onMarkAttendanceClick(StudentDashboard.ActiveSession session) {
                // ✅ Connect to your existing markPresent method
                if (session.isInBluetoothRange() && session.isActive()) {
                    activeSessionId = session.getSessionId();
                    activeSubject = session.getSubject();
                    markPresent();
                } else {
                    showErrorMessage("Session not available for attendance");
                }
            }
        });

        rvActiveSubjects.setAdapter(activeSubjectsAdapter);
    }

    private boolean isTeacherDeviceDetected(String sessionId) {
        if (sessionId == null) return false;

        // Check if we've detected the teacher device recently (within last 30 seconds)
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastDiscoveryTime) < 30000 && !devicesInRange.isEmpty();
    }


    private void logBluetoothStatus() {
        Log.d(TAG, "=== Bluetooth Status ===");
        Log.d(TAG, "Bluetooth enabled: " + bluetoothHelper.isBluetoothEnabled());
        Log.d(TAG, "Has permissions: " + bluetoothHelper.hasPermissions());
        Log.d(TAG, "Devices in range: " + devicesInRange.size());
        Log.d(TAG, "Active sessions: " + activeSessions.size());
        Log.d(TAG, "Student branch/year: " + studentBranch + "/" + studentYear);

        for (ActiveSession session : activeSessions) {
            Log.d(TAG, "Session: " + session.getSubject() +
                    ", ID: " + session.getSessionId() +
                    ", In range: " + session.isInBluetoothRange() +
                    ", Branch/Year: " + session.getBranch() + "/" + session.getYear());
        }
        Log.d(TAG, "======================");
    }
    private void updateSessionHistory() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "User is null in updateSessionHistory");
            return;
        }

        if (activeSessionId == null) {
            Log.e(TAG, "activeSessionId is null in updateSessionHistory");
            return;
        }

        if (studentBranch == null || studentYear == null) {
            Log.e(TAG, "Student branch or year is null: " + studentBranch + "/" + studentYear);
            return;
        }

        String studentId = user.getUid();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DatabaseReference sessionHistoryRef = FirebaseDatabase.getInstance()
                .getReference("sessionHistory")
                .child(currentDate)
                .child(studentBranch)
                .child(studentYear)
                .child(activeSessionId);

        Map<String, Object> sessionUpdate = new HashMap<>();
        sessionUpdate.put("subject", activeSubject);
        sessionUpdate.put("attendedStudents/" + studentId, true);
        sessionUpdate.put("lastUpdated", System.currentTimeMillis());

        sessionHistoryRef.updateChildren(sessionUpdate);
    }
    private void initializeAttendanceTracking() {
        loadStudentProfile();
        loadAttendanceData();
        updateActiveSessionCountUI();
    }

    private void loadData() {
        // Load real attendance data instead of sample data
        loadAttendanceData();
    }
    private void updateSessionAttendanceCount() {
        DatabaseReference sessionRef = FirebaseDatabase.getInstance()
                .getReference("activeSessions")
                .child(studentBranch)
                .child(studentYear)
                .child(activeSessionId)
                .child("attendanceCount");

        sessionRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Integer currentCount = mutableData.getValue(Integer.class);
                if (currentCount == null) {
                    mutableData.setValue(1);
                } else {
                    mutableData.setValue(currentCount + 1);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean committed, DataSnapshot dataSnapshot) {
                if (databaseError != null) {
                    Log.e("StudentDashboard", "Error updating attendance count", databaseError.toException());
                }
            }
        });
    }
    private void initializeViews() {
        drawerLayout = findViewById(R.id.main_student_layout);
        pieChart = findViewById(R.id.pieChart);
        tvActiveSessionCount = findViewById(R.id.tvActiveSessionCount);
        tvOverallPercentage = findViewById(R.id.tvOverallPercentage);
        tvSubjectPercentage = findViewById(R.id.tvSubjectPercentage);
        tvSubjectName = findViewById(R.id.tvSubjectName);
        rvAttendanceHistory = findViewById(R.id.rvAttendanceHistory);
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
    private void setupAttendanceChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(android.R.color.transparent);
        pieChart.setTransparentCircleRadius(61f);
    }
    private void setupAttendanceHistoryRecyclerView() {
        if (rvAttendanceHistory != null) {
            rvAttendanceHistory.setLayoutManager(new LinearLayoutManager(this));
            adapter = new AttendanceHistoryAdapter(new ArrayList<>());
            rvAttendanceHistory.setAdapter(adapter);
        }
    }
    private void updateSubjectStats(Map<String, int[]> subjectStats) {
        // Update pie chart and text views with subject data
        // Example: show first subject or most recent
        if (!subjectStats.isEmpty()) {
            Map.Entry<String, int[]> firstEntry = subjectStats.entrySet().iterator().next();
            String subject = firstEntry.getKey();
            int present = firstEntry.getValue()[0];
            int total = firstEntry.getValue()[1];
            int percentage = total > 0 ? (present * 100) / total : 0;

            tvSubjectName.setText(subject);
            tvSubjectPercentage.setText(percentage + "%");

            // Update pie chart
            List<PieEntry> entries = new ArrayList<>();
            entries.add(new PieEntry(present, "Present"));
            entries.add(new PieEntry(total - present, "Absent"));

            // ... rest of pie chart setup ...
        }
    }
    private void fetchActiveSessions() {
        if (studentBranch == null || studentYear == null) {
            Log.e("StudentDashboard", "Branch or year is null. Cannot load sessions.");
            return;
        }
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("activeSessions")
                .child(studentBranch)
                .child(studentYear);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ActiveSession> activeList = new ArrayList<>();
                for (DataSnapshot sessionSnap : snapshot.getChildren()) {
                    ActiveSession session = sessionSnap.getValue(ActiveSession.class);
                    if (session != null && session.isActive()) {
                        activeList.add(session);
                    }
                }
                activeSubjectsAdapter.setSessions(activeList);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }




    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
        } else if (id == R.id.nav_attendance_history) {
        } else if (id == R.id.nav_timetable) {
            Intent timetableIntent = new Intent(this, TimetableActivity.class);
            startActivity(timetableIntent);
        } else if (id == R.id.nav_profile) {
            Intent profileIntent = new Intent(this, ProfileActivity.class);
            startActivity(profileIntent);
        } else if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @Override
    protected void onResume() {
        super.onResume();
        startBluetoothDiscovery();
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @Override
    protected void onPause() {
        super.onPause();
        sessionCheckHandler.removeCallbacks(this::startBluetoothDiscovery);
        bluetoothHelper.cancelDiscovery();
    }
    // Remove this method entirely, or replace with:
    private void refreshAdapter() {
        if (activeSubjectsAdapter != null) {
            activeSubjectsAdapter.notifyDataSetChanged();
        }
    }
    private void loadAttendanceData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && studentBranch != null && studentYear != null) {
            loadOverallAttendanceData(user.getUid());
        }
    }

    // Fix the AttendanceRecord instantiation and missing methods
    private void loadOverallAttendanceData(String studentId) {
        DatabaseReference attendanceRef = FirebaseDatabase.getInstance()
                .getReference("attendance");

        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Integer> subjectSessionCounts = new HashMap<>();
                Map<String, Integer> studentAttendanceCounts = new HashMap<>();
                List<AttendanceRecord> attendanceHistory = new ArrayList<>();

                // Iterate through all dates
                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    String date = dateSnapshot.getKey();
                    DataSnapshot branchSnapshot = dateSnapshot.child(studentBranch);
                    if (branchSnapshot.exists()) {
                        DataSnapshot yearSnapshot = branchSnapshot.child(studentYear);
                        if (yearSnapshot.exists()) {
                            // Iterate through all subjects
                            for (DataSnapshot subjectSnapshot : yearSnapshot.getChildren()) {
                                String subject = subjectSnapshot.getKey();

                                // Count total sessions for this subject on this date
                                int totalStudentsInSession = (int) subjectSnapshot.getChildrenCount();
                                if (totalStudentsInSession > 0) {
                                    subjectSessionCounts.put(subject,
                                            subjectSessionCounts.getOrDefault(subject, 0) + 1);
                                }

                                // Check if this student was present
                                if (subjectSnapshot.hasChild(studentId)) {
                                    studentAttendanceCounts.put(subject,
                                            studentAttendanceCounts.getOrDefault(subject, 0) + 1);

                                    // Add to history - Fix constructor call
                                    AttendanceRecord record = new AttendanceRecord(date, subject, "present");
                                    // Remove setTimestamp call since method doesn't exist
                                    attendanceHistory.add(record);
                                }
                            }
                        }
                    }
                }

                // Calculate overall statistics
                int totalSessions = subjectSessionCounts.values().stream().mapToInt(Integer::intValue).sum();
                int totalPresent = studentAttendanceCounts.values().stream().mapToInt(Integer::intValue).sum();

                // Update UI with overall data
                updateOverallAttendanceUI(totalPresent, totalSessions, subjectSessionCounts, studentAttendanceCounts);

                // Update attendance history
                runOnUiThread(() -> {
                    if (adapter != null) {
                        adapter.updateData(attendanceHistory);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load attendance data: " + error.getMessage());
            }
        });
    }
    private void loadSubjectAttendanceData(String studentId) {
        // Get session history to see all activated sessions
        DatabaseReference sessionsRef = FirebaseDatabase.getInstance()
                .getReference("sessionHistory")
                .child(studentBranch)
                .child(studentYear);

        sessionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, SubjectAttendanceData> subjectData = new HashMap<>();

                for (DataSnapshot sessionSnapshot : snapshot.getChildren()) {
                    String subject = sessionSnapshot.child("subject").getValue(String.class);
                    String sessionId = sessionSnapshot.getKey();

                    if (subject != null) {
                        if (!subjectData.containsKey(subject)) {
                            subjectData.put(subject, new SubjectAttendanceData());
                        }

                        SubjectAttendanceData data = subjectData.get(subject);
                        assert data != null;
                        data.totalSessions++;

                        // Check if student attended this session
                        if (sessionSnapshot.child("attendedStudents").hasChild(studentId)) {
                            data.attendedSessions++;
                        }
                    }
                }

                // Update UI with subject-specific data
                updateSubjectAttendanceUI(subjectData);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StudentDashboard", "Error loading session history", error.toException());
            }
        });
    }
    private void setupActiveSessionsAdapter() {
        activeSubjectsAdapter = new ActiveSubjectAdapter(this, new ArrayList<>());

        activeSubjectsAdapter.setOnSessionClickListener(new ActiveSubjectAdapter.OnSessionClickListener() {
            @Override
            public void onSessionClick(StudentDashboard.ActiveSession session) {
                onActiveSessionClick(session);
            }

            @Override
            public void onMarkPresentClick(StudentDashboard.ActiveSession session) {
                if (session.isInBluetoothRange() && session.isActive()) {
                    activeSessionId = session.getSessionId();
                    activeSubject = session.getSubject();
                    markPresent();
                } else {
                    showErrorMessage("Cannot mark attendance - session not in range or inactive");
                }
            }

            @Override
            public void onMarkAttendanceClick(ActiveSession session) {

            }
        });

        rvActiveSubjects.setLayoutManager(new LinearLayoutManager(this));
        rvActiveSubjects.setAdapter(activeSubjectsAdapter);
    }
    private void updateSubjectAttendanceUI(Map<String, SubjectAttendanceData> subjectData) {
        runOnUiThread(() -> {
            // Create adapter data for subject-wise attendance
            List<AttendanceRecord> attendanceRecords = new ArrayList<>();

            for (Map.Entry<String, SubjectAttendanceData> entry : subjectData.entrySet()) {
                String subject = entry.getKey();
                SubjectAttendanceData data = entry.getValue();

                int percentage = data.totalSessions > 0 ?
                        (data.attendedSessions * 100) / data.totalSessions : 0;

                String status = percentage >= 75 ? "Good" : percentage >= 60 ? "Average" : "Poor";
                String details = data.attendedSessions + "/" + data.totalSessions + " (" + percentage + "%)";

                attendanceRecords.add(new AttendanceRecord(subject, details, status));
            }

            adapter.updateData(attendanceRecords);
        });
    }

    private static class SubjectAttendanceData {
        int totalSessions = 0;
        int attendedSessions = 0;
    }

    public static class ActiveSession {
        private String sessionId;
        private long startTime;
        private long endTime;
        private String subject;
        private String teacherName;
        private String teacherId;
        private boolean active;
        private String teacherDeviceId;
        private boolean inBluetoothRange;
        private int attendanceCount;
        private String branch; // Fixed: Changed from 'Branch' to 'branch'
        private String year;

        public ActiveSession() {
        }

        public String getBranch() {
            return branch;
        }
        public void setBluetoothInRange(boolean inRange) {
            this.inBluetoothRange = inRange;
        }

        public void setTeacherDeviceAddress(String address) {
            this.teacherDeviceId = address;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }
        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }

        public String getYear() {
            return year;
        }

        public void setYear(String year) {
            this.year = year;
        }

        // Getters and setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public String getTeacherName() { return teacherName; }
        public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

        public String getTeacherId() { return teacherId; }
        public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }

        public String getTeacherDeviceId() { return teacherDeviceId; }
        public void setTeacherDeviceId(String teacherDeviceId) { this.teacherDeviceId = teacherDeviceId; }

        public boolean isInBluetoothRange() { return inBluetoothRange; }
        public void setInBluetoothRange(boolean inBluetoothRange) { this.inBluetoothRange = inBluetoothRange; }

        public int getAttendanceCount() { return attendanceCount; }
        public void setAttendanceCount(int attendanceCount) { this.attendanceCount = attendanceCount; }
    }

}