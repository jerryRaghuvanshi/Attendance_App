package com.example.attendanceapp.settings;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceapp.R;
import com.example.attendanceapp.bluetooth.BluetoothHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Comprehensive Bluetooth settings and management activity
 */
public class BluetoothSettingsActivity extends AppCompatActivity {
    private static final String TAG = "BluetoothSettings";
    private static final int BLUETOOTH_PERMISSION_REQUEST = 100;
    private static final int BLUETOOTH_ENABLE_REQUEST = 101;

    // UI Components
    private SwitchCompat switchBluetoothMonitoring;
    private SwitchCompat switchAutoScan;
    private SwitchCompat switchNotifications;
    private Slider sliderScanInterval;
    private TextView tvScanInterval;
    private TextView tvBluetoothStatus;
    private TextView tvPermissionStatus;
    private TextView tvDiagnosticInfo;
    private Button btnRequestPermissions;
    private Button btnEnableBluetooth;
    private Button btnTestScan;
    private Button btnOpenBluetoothSettings;
    private ProgressBar progressScanning;
    private RecyclerView rvPairedDevices;
    private MaterialCardView cardBluetoothStatus;
    private MaterialCardView cardPermissions;
    private MaterialCardView cardAdvanced;

    // Components
    private BluetoothHelper bluetoothHelper;
    private SharedPreferences preferences;
    private PairedDevicesAdapter pairedDevicesAdapter;

    // Settings
    private boolean bluetoothMonitoringEnabled;
    private boolean autoScanEnabled;
    private boolean notificationsEnabled;
    private int scanIntervalSeconds = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_settings);

        initializeComponents();
        setupToolbar();
        initializeViews();
        loadSettings();
        setupListeners();
        updateUI();
    }

    private void initializeComponents() {
        bluetoothHelper = new BluetoothHelper(this);
        preferences = getSharedPreferences("bluetooth_settings", MODE_PRIVATE);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Bluetooth Settings");
        }
    }

    private void initializeViews() {
        // Switches
        switchBluetoothMonitoring = findViewById(R.id.switchBluetoothMonitoring);
        switchAutoScan = findViewById(R.id.switchAutoScan);
        switchNotifications = findViewById(R.id.switchNotifications);

        // Slider
        sliderScanInterval = findViewById(R.id.sliderScanInterval);
        tvScanInterval = findViewById(R.id.tvScanInterval);

        // Status TextViews
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus);
        tvPermissionStatus = findViewById(R.id.tvPermissionStatus);
        tvDiagnosticInfo = findViewById(R.id.tvDiagnosticInfo);

        // Buttons
        btnRequestPermissions = findViewById(R.id.btnRequestPermissions);
        btnEnableBluetooth = findViewById(R.id.btnEnableBluetooth);
        btnTestScan = findViewById(R.id.btnTestScan);
        btnOpenBluetoothSettings = findViewById(R.id.btnOpenBluetoothSettings);

        // Other components
        progressScanning = findViewById(R.id.progressScanning);
        rvPairedDevices = findViewById(R.id.rvPairedDevices);
        cardBluetoothStatus = findViewById(R.id.cardBluetoothStatus);
        cardPermissions = findViewById(R.id.cardPermissions);
        cardAdvanced = findViewById(R.id.cardAdvanced);

        // Setup RecyclerView
        setupPairedDevicesRecyclerView();

        // Setup slider
        setupScanIntervalSlider();
    }

    private void setupPairedDevicesRecyclerView() {
        rvPairedDevices.setLayoutManager(new LinearLayoutManager(this));
        pairedDevicesAdapter = new PairedDevicesAdapter(new ArrayList<>());
        rvPairedDevices.setAdapter(pairedDevicesAdapter);
    }

    private void setupScanIntervalSlider() {
        sliderScanInterval.setValueFrom(5);
        sliderScanInterval.setValueTo(60);
        sliderScanInterval.setStepSize(5);
        sliderScanInterval.setValue(scanIntervalSeconds);

        sliderScanInterval.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                scanIntervalSeconds = (int) value;
                updateScanIntervalText();
                saveScanInterval();
            }
        });

        updateScanIntervalText();
    }

    private void updateScanIntervalText() {
        tvScanInterval.setText(getString(R.string.scan_interval_format, scanIntervalSeconds));
    }

    private void loadSettings() {
        bluetoothMonitoringEnabled = preferences.getBoolean("bluetooth_monitoring", true);
        autoScanEnabled = preferences.getBoolean("auto_scan", true);
        notificationsEnabled = preferences.getBoolean("notifications", true);
        scanIntervalSeconds = preferences.getInt("scan_interval", 15);

        // Update UI with loaded settings
        switchBluetoothMonitoring.setChecked(bluetoothMonitoringEnabled);
        switchAutoScan.setChecked(autoScanEnabled);
        switchNotifications.setChecked(notificationsEnabled);
        sliderScanInterval.setValue(scanIntervalSeconds);
        updateScanIntervalText();
    }

    private void setupListeners() {
        // Switch listeners
        switchBluetoothMonitoring.setOnCheckedChangeListener(this::onBluetoothMonitoringChanged);
        switchAutoScan.setOnCheckedChangeListener(this::onAutoScanChanged);
        switchNotifications.setOnCheckedChangeListener(this::onNotificationsChanged);

        // Button listeners
        btnRequestPermissions.setOnClickListener(v -> requestBluetoothPermissions());
        btnEnableBluetooth.setOnClickListener(v -> enableBluetooth());
        btnTestScan.setOnClickListener(v -> performTestScan());
        btnOpenBluetoothSettings.setOnClickListener(v -> openBluetoothSettings());

        // Card click listeners for expansion
        cardAdvanced.setOnClickListener(v -> toggleAdvancedSettings());
    }

    private void onBluetoothMonitoringChanged(CompoundButton button, boolean isChecked) {
        bluetoothMonitoringEnabled = isChecked;
        preferences.edit().putBoolean("bluetooth_monitoring", isChecked).apply();

        // Enable/disable dependent settings
        switchAutoScan.setEnabled(isChecked);
        switchNotifications.setEnabled(isChecked);
        sliderScanInterval.setEnabled(isChecked && autoScanEnabled);

        if (isChecked && !bluetoothHelper.hasPermissions()) {
            requestBluetoothPermissions();
        }

        updateUI();
        showToast(isChecked ? "Bluetooth monitoring enabled" : "Bluetooth monitoring disabled");
    }

    private void onAutoScanChanged(CompoundButton button, boolean isChecked) {
        autoScanEnabled = isChecked;
        preferences.edit().putBoolean("auto_scan", isChecked).apply();

        sliderScanInterval.setEnabled(isChecked && bluetoothMonitoringEnabled);

        showToast(isChecked ? "Auto scan enabled" : "Auto scan disabled");
    }

    private void onNotificationsChanged(CompoundButton button, boolean isChecked) {
        notificationsEnabled = isChecked;
        preferences.edit().putBoolean("notifications", isChecked).apply();

        showToast(isChecked ? "Notifications enabled" : "Notifications disabled");
    }

    private void saveScanInterval() {
        preferences.edit().putInt("scan_interval", scanIntervalSeconds).apply();
    }

    private void updateUI() {
        updateBluetoothStatus();
        updatePermissionStatus();
        updateButtonStates();
        updateDiagnosticInfo();
        loadPairedDevices();
    }

    private void updateBluetoothStatus() {
        String status = bluetoothHelper.getStatusMessage();
        tvBluetoothStatus.setText(status);

        // Update card color based on status
        int colorResId;
        if (bluetoothHelper.isOperational()) {
            colorResId = R.color.green_50;
        } else if (bluetoothHelper.isBluetoothEnabled()) {
            colorResId = R.color.orange_50;
        } else {
            colorResId = R.color.red_50;
        }

        cardBluetoothStatus.setCardBackgroundColor(ContextCompat.getColor(this, colorResId));
    }

    private void updatePermissionStatus() {
        boolean hasPermissions = bluetoothHelper.hasPermissions();

        if (hasPermissions) {
            tvPermissionStatus.setText("✅ All required permissions granted");
            tvPermissionStatus.setTextColor(ContextCompat.getColor(this, R.color.green_600));
            cardPermissions.setCardBackgroundColor(ContextCompat.getColor(this, R.color.green_50));
        } else {
            tvPermissionStatus.setText("❌ Bluetooth permissions required");
            tvPermissionStatus.setTextColor(ContextCompat.getColor(this, R.color.red_600));
            cardPermissions.setCardBackgroundColor(ContextCompat.getColor(this, R.color.red_50));
        }
    }

    private void updateButtonStates() {
        btnRequestPermissions.setVisibility(bluetoothHelper.hasPermissions() ? View.GONE : View.VISIBLE);
        btnEnableBluetooth.setVisibility(bluetoothHelper.isBluetoothEnabled() ? View.GONE : View.VISIBLE);
        btnTestScan.setEnabled(bluetoothHelper.isOperational());
    }

    private void updateDiagnosticInfo() {
        String diagnosticInfo = bluetoothHelper.getDiagnosticInfo();
        tvDiagnosticInfo.setText(diagnosticInfo);
    }

    private void loadPairedDevices() {
        if (!bluetoothHelper.hasPermissions() || !bluetoothHelper.isBluetoothEnabled()) {
            pairedDevicesAdapter.updateDevices(new ArrayList<>());
            return;
        }

        try {
            Set<BluetoothDevice> pairedDevices = bluetoothHelper.getPairedDevices();
            if (pairedDevices != null) {
                List<BluetoothDevice> deviceList = new ArrayList<>(pairedDevices);
                pairedDevicesAdapter.updateDevices(deviceList);
            }
        } catch (SecurityException e) {
            showToast("Permission error loading paired devices");
        }
    }

    private void requestBluetoothPermissions() {
        if (bluetoothHelper.hasPermissions()) {
            showToast("Permissions already granted");
            return;
        }

        String[] permissions = bluetoothHelper.getRequiredPermissions();

        // Show explanation dialog first
        new AlertDialog.Builder(this)
                .setTitle("Bluetooth Permissions")
                .setMessage("This app needs Bluetooth permissions to:\n\n" +
                        "• Scan for nearby attendance sessions\n" +
                        "• Connect to teacher devices\n" +
                        "• Automatically mark attendance\n\n" +
                        "These permissions are essential for attendance functionality.")
                .setPositiveButton("Grant Permissions", (dialog, which) -> {
                    ActivityCompat.requestPermissions(this, permissions, BLUETOOTH_PERMISSION_REQUEST);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void enableBluetooth() {
        if (bluetoothHelper.isBluetoothEnabled()) {
            showToast("Bluetooth is already enabled");
            return;
        }

        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            startActivityForResult(enableBtIntent, BLUETOOTH_ENABLE_REQUEST);
        } else {
            showToast("Permission required to enable Bluetooth");
            requestBluetoothPermissions();
        }
    }

    private void performTestScan() {
        if (!bluetoothHelper.isOperational()) {
            showToast("Bluetooth not ready for scanning");
            return;
        }

        progressScanning.setVisibility(View.VISIBLE);
        btnTestScan.setEnabled(false);
        btnTestScan.setText("Scanning...");

        try {
            boolean started = bluetoothHelper.startDiscovery();
            if (started) {
                showToast("Test scan started");

                // Stop scan after 10 seconds
                new android.os.Handler().postDelayed(() -> {
                    try {
                        bluetoothHelper.cancelDiscovery();
                    } catch (SecurityException e) {
                        // Ignore
                    }
                    progressScanning.setVisibility(View.GONE);
                    btnTestScan.setEnabled(true);
                    btnTestScan.setText("Test Scan");
                    showToast("Test scan completed");
                }, 10000);

            } else {
                progressScanning.setVisibility(View.GONE);
                btnTestScan.setEnabled(true);
                btnTestScan.setText("Test Scan");
                showToast("Failed to start test scan");
            }
        } catch (SecurityException e) {
            progressScanning.setVisibility(View.GONE);
            btnTestScan.setEnabled(true);
            btnTestScan.setText("Test Scan");
            showToast("Permission error during test scan");
        }
    }

    private void openBluetoothSettings() {
        Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intent);
    }

    private boolean advancedSettingsExpanded = false;

    private void toggleAdvancedSettings() {
        View advancedContent = findViewById(R.id.layoutAdvancedContent);
        if (advancedContent != null) {
            if (advancedSettingsExpanded) {
                advancedContent.setVisibility(View.GONE);
                advancedSettingsExpanded = false;
            } else {
                advancedContent.setVisibility(View.VISIBLE);
                advancedSettingsExpanded = true;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == BLUETOOTH_PERMISSION_REQUEST) {
            boolean allGranted = grantResults.length > 0;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                showToast("Bluetooth permissions granted");
                updateUI();
            } else {
                showToast("Some permissions were denied");
                showPermissionDeniedDialog();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("Bluetooth permissions are essential for attendance functionality. " +
                        "You can grant them manually in app settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BLUETOOTH_ENABLE_REQUEST) {
            if (resultCode == RESULT_OK) {
                showToast("Bluetooth enabled");
                updateUI();
            } else {
                showToast("Bluetooth not enabled");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothHelper != null) {
            bluetoothHelper.cleanup();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Inner class for paired devices adapter
    private static class PairedDevicesAdapter extends RecyclerView.Adapter<PairedDevicesAdapter.DeviceViewHolder> {
        private List<BluetoothDevice> devices;

        public PairedDevicesAdapter(List<BluetoothDevice> devices) {
            this.devices = devices;
        }

        public void updateDevices(List<BluetoothDevice> newDevices) {
            this.devices = newDevices;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_paired_device, parent, false);
            return new DeviceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
            BluetoothDevice device = devices.get(position);
            holder.bind(device);
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        static class DeviceViewHolder extends RecyclerView.ViewHolder {
            private TextView tvDeviceName;
            private TextView tvDeviceAddress;
            private TextView tvDeviceType;

            public DeviceViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
                tvDeviceAddress = itemView.findViewById(R.id.tvDeviceAddress);
                tvDeviceType = itemView.findViewById(R.id.tvDeviceType);
            }

            public void bind(BluetoothDevice device) {
                try {
                    String name = device.getName();
                    tvDeviceName.setText(name != null ? name : "Unknown Device");
                    tvDeviceAddress.setText(device.getAddress());

                    // Determine device type
                    int deviceClass = device.getBluetoothClass().getDeviceClass();
                    String deviceType = getDeviceTypeString(deviceClass);
                    tvDeviceType.setText(deviceType);

                } catch (SecurityException e) {
                    tvDeviceName.setText("Permission Required");
                    tvDeviceAddress.setText("---");
                    tvDeviceType.setText("Unknown");
                }
            }

            private String getDeviceTypeString(int deviceClass) {
                // Simplified device type detection
                switch (deviceClass) {
                    case 0x1F00: return "Computer";
                    case 0x200: return "Phone";
                    case 0x400: return "Audio/Video";
                    case 0x500: return "Peripheral";
                    case 0x600: return "Imaging";
                    case 0x700: return "Wearable";
                    case 0x800: return "Toy";
                    case 0x900: return "Health";
                    default: return "Unknown";
                }
            }
        }
    }
}