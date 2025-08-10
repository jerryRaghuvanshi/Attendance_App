package com.example.attendanceapp.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.annotation.RequiresPermission;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Enhanced BluetoothHelper with improved error handling, power management,
 * and better compatibility across Android versions
 */
public class BluetoothHelper {
    private static final String TAG = "BluetoothHelper";

    // Constants
    private static final long MIN_DISCOVERY_INTERVAL = 5000; // 5 seconds minimum between discoveries
    private static final int DEFAULT_DISCOVERABLE_DURATION = 300; // 5 minutes
    private static final int MAX_DEVICE_NAME_LENGTH = 248; // Bluetooth name limit
    private static final String DEFAULT_SESSION_PREFIX = "ATT_";
    private static final String DEFAULT_STUDENT_PREFIX = "Student-";

    // Instance variables
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private boolean isDiscovering = false;
    private long lastDiscoveryStart = 0;
    private String originalBluetoothName = null;

    public BluetoothHelper(Context context) {
        this.context = context;

        // Get BluetoothAdapter using the modern approach
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;

        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported on this device");
        } else {
            // Store original name when helper is created
            try {
                if (hasConnectPermission()) {
                    originalBluetoothName = bluetoothAdapter.getName();
                }
            } catch (SecurityException e) {
                Log.w(TAG, "Cannot get original Bluetooth name due to permissions");
            }
        }
    }

    // =========================== BASIC BLUETOOTH OPERATIONS ===========================

    /**
     * Check if Bluetooth is supported on this device
     */
    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    /**
     * Check if Bluetooth is currently enabled
     */
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * Check if BLE is supported on this device
     */
    public boolean isBleSupported() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * Check if the app has the necessary Bluetooth permissions
     */
    public boolean hasPermissions() {
        boolean bluetoothPerms = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;

        boolean locationPerm = hasLocationPermission();

        Log.d(TAG, "Bluetooth permissions: " + bluetoothPerms + ", Location permission: " + locationPerm);

        return bluetoothPerms && locationPerm;
    }
    /**
     * Check for BLUETOOTH_CONNECT permission specifically
     */
    private boolean hasConnectPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check for BLUETOOTH_SCAN permission specifically
     */
    private boolean hasScanPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Get a list of required permissions based on Android version
     */
    public String[] getRequiredPermissions() {
        return new String[]{
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
    }
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    public boolean forceRestartDiscovery() {
        Log.d(TAG, "🔄 Force restarting Bluetooth discovery...");

        try {
            // Step 1: Cancel any existing discovery
            if (bluetoothAdapter.isDiscovering()) {
                Log.d(TAG, "Cancelling existing discovery...");
                boolean cancelled = bluetoothAdapter.cancelDiscovery();
                Log.d(TAG, "Cancel result: " + cancelled);

                // Wait longer for proper cancellation
                Thread.sleep(1000);
            }

            // Step 2: Check adapter state
            int state = bluetoothAdapter.getState();
            Log.d(TAG, "Bluetooth adapter state: " + state + " (" + getBluetoothStateString() + ")");

            if (state != BluetoothAdapter.STATE_ON) {
                Log.e(TAG, "Bluetooth adapter not in ON state");
                return false;
            }

            // Step 3: Reset internal flags
            isDiscovering = false;
            lastDiscoveryStart = 0;

            // Step 4: Wait a bit more
            Thread.sleep(500);

            // Step 5: Start discovery
            boolean started = bluetoothAdapter.startDiscovery();
            Log.d(TAG, "Discovery start result: " + started);

            if (started) {
                isDiscovering = true;
                lastDiscoveryStart = System.currentTimeMillis();
                Log.d(TAG, "✅ Force restart successful");
            } else {
                Log.e(TAG, "❌ Force restart failed - startDiscovery returned false");
            }

            return started;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Thread interrupted during force restart", e);
            return false;
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception during force restart", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during force restart", e);
            return false;
        }
    }
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    public String getDiscoveryDiagnostic() {
        StringBuilder info = new StringBuilder();
        info.append("=== Discovery Diagnostic ===\n");

        try {
            info.append("Bluetooth Enabled: ").append(isBluetoothEnabled()).append("\n");
            info.append("Adapter State: ").append(getBluetoothState()).append(" (").append(getBluetoothStateString()).append(")\n");
            info.append("Has BLUETOOTH_SCAN: ").append(hasScanPermission()).append("\n");
            info.append("Has BLUETOOTH_CONNECT: ").append(hasConnectPermission()).append("\n");
            info.append("Has Location Permission: ").append(hasLocationPermission()).append("\n");
            info.append("Currently Discovering: ").append(bluetoothAdapter.isDiscovering()).append("\n");
            info.append("Internal Discovering Flag: ").append(isDiscovering).append("\n");
            info.append("Last Discovery Start: ").append(lastDiscoveryStart).append("\n");
            info.append("Time Since Last Discovery: ").append(System.currentTimeMillis() - lastDiscoveryStart).append("ms\n");

            // Test discovery capability
            info.append("--- Discovery Test ---\n");
            if (bluetoothAdapter.isDiscovering()) {
                info.append("Discovery already in progress\n");
            } else {
                boolean testStart = bluetoothAdapter.startDiscovery();
                info.append("Test Start Discovery: ").append(testStart).append("\n");
                if (testStart) {
                    Thread.sleep(100);
                    boolean testCancel = bluetoothAdapter.cancelDiscovery();
                    info.append("Test Cancel Discovery: ").append(testCancel).append("\n");
                }
            }

        } catch (SecurityException e) {
            info.append("Security Exception: ").append(e.getMessage()).append("\n");
        } catch (Exception e) {
            info.append("Exception: ").append(e.getMessage()).append("\n");
        }

        info.append("=========================");
        return info.toString();
    }
    public static boolean isValidTeacherDeviceName(String deviceName) {
        if (deviceName == null || deviceName.trim().isEmpty()) {
            Log.e(TAG, "Device name is null or empty");
            return false;
        }

        // Check for expected patterns
        boolean isValid = deviceName.startsWith("AttendanceSession-") ||
                deviceName.startsWith("ATT_") ||
                deviceName.startsWith("Session-") ||
                deviceName.startsWith("TeacherDevice-");

        Log.d(TAG, "Device name validation - Name: '" + deviceName + "', Valid: " + isValid);

        return isValid;
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public String testDeviceNameDiscovery(String testName) {
        StringBuilder result = new StringBuilder();
        result.append("=== Device Name Test ===\n");

        try {
            String currentName = bluetoothAdapter.getName();
            result.append("Current Name: ").append(currentName).append("\n");
            result.append("Test Name: ").append(testName).append("\n");
            result.append("Name Length: ").append(testName != null ? testName.length() : 0).append("\n");
            result.append("Within Limit: ").append(testName != null && testName.length() <= MAX_DEVICE_NAME_LENGTH).append("\n");
            result.append("Valid Teacher Pattern: ").append(isValidTeacherDeviceName(testName)).append("\n");

            // Test setting the name
            if (testName != null && !testName.equals(currentName)) {
                boolean nameSet = bluetoothAdapter.setName(testName);
                result.append("Name Set Success: ").append(nameSet).append("\n");

                if (nameSet) {
                    // Wait and verify
                    Thread.sleep(500);
                    String verifyName = bluetoothAdapter.getName();
                    result.append("Verified Name: ").append(verifyName).append("\n");
                    result.append("Names Match: ").append(testName.equals(verifyName)).append("\n");

                    // Restore original name
                    bluetoothAdapter.setName(currentName);
                }
            }

        } catch (SecurityException e) {
            result.append("Security Exception: ").append(e.getMessage()).append("\n");
        } catch (Exception e) {
            result.append("Exception: ").append(e.getMessage()).append("\n");
        }

        result.append("=======================");
        return result.toString();
    }
    // =========================== DISCOVERY OPERATIONS ===========================

    /**
     * Check if discovery is currently in progress
     */
    public boolean isDiscovering() {
        if (bluetoothAdapter == null) return false;

        try {
            if (hasConnectPermission()) {
                return bluetoothAdapter.isDiscovering();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied checking discovery status", e);
        }
        return isDiscovering;
    }

    /**
     * Start device discovery with enhanced error handling
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public boolean startDiscovery() {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth adapter is null");
            return false;
        }

        if (!isBluetoothEnabled()) {
            Log.w(TAG, "Bluetooth is not enabled");
            return false;
        }

        if (!hasPermissions()) {
            Log.w(TAG, "Missing required Bluetooth permissions");
            return false;
        }

        // Prevent rapid consecutive discovery attempts
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDiscoveryStart < MIN_DISCOVERY_INTERVAL) {
            Log.d(TAG, "Discovery attempt too soon, skipping");
            return false;
        }

        try {
            // Cancel any ongoing discovery first
            if (bluetoothAdapter.isDiscovering()) {
                Log.d(TAG, "Canceling ongoing discovery before starting new one");
                boolean cancelled = bluetoothAdapter.cancelDiscovery();
                Log.d(TAG, "Discovery cancellation result: " + cancelled);

                // Wait for cancellation to complete
                try {
                    Thread.sleep(500); // Increased wait time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.w(TAG, "Thread interrupted during discovery wait");
                }
            }

            // Start fresh discovery
            boolean started = bluetoothAdapter.startDiscovery();
            if (started) {
                isDiscovering = true;
                lastDiscoveryStart = currentTime;
                Log.d(TAG, "✅ Bluetooth discovery started successfully at " + formatTimestamp(currentTime));
            } else {
                Log.w(TAG, "❌ Failed to start Bluetooth discovery - system returned false");
            }

            return started;

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception starting discovery - missing permissions", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error starting discovery", e);
            return false;
        }
    }
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Cancel ongoing discovery with proper error handling
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public boolean cancelDiscovery() {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth adapter is null");
            return false;
        }

        try {
            if (bluetoothAdapter.isDiscovering()) {
                boolean cancelled = bluetoothAdapter.cancelDiscovery();
                if (cancelled) {
                    isDiscovering = false;
                    Log.d(TAG, "✅ Bluetooth discovery cancelled successfully");
                } else {
                    Log.w(TAG, "❌ Failed to cancel Bluetooth discovery");
                }
                return cancelled;
            } else {
                Log.d(TAG, "No discovery in progress to cancel");
                isDiscovering = false;
                return true;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception canceling discovery", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error canceling discovery", e);
            return false;
        }
    }

    /**
     * Restart Bluetooth discovery with proper cleanup
     */
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    public boolean restartDiscovery() {
        Log.d(TAG, "Restarting Bluetooth discovery...");

        // Cancel current discovery
        boolean cancelled = cancelDiscovery();

        // Wait a moment for cancellation to complete
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Start new discovery
        return startDiscovery();
    }

    // =========================== DEVICE MANAGEMENT ===========================

    /**
     * Get paired devices (bonded devices)
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public Set<BluetoothDevice> getPairedDevices() {
        if (bluetoothAdapter == null || !isBluetoothEnabled()) {
            return null;
        }

        try {
            return bluetoothAdapter.getBondedDevices();
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting paired devices", e);
            return null;
        }
    }

    /**
     * Get current Bluetooth adapter name
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public String getBluetoothName() {
        if (bluetoothAdapter == null || !isBluetoothEnabled()) {
            return "Unknown";
        }

        try {
            String name = bluetoothAdapter.getName();
            return name != null ? name : "Unknown";
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting Bluetooth name", e);
            return "Permission Denied";
        }
    }

    /**
     * Get Bluetooth adapter name (alias for compatibility)
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public String getAdapterName() {
        return getBluetoothName();
    }

    /**
     * Set Bluetooth adapter name for attendance sessions
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public boolean setBluetoothName(String name) {
        if (bluetoothAdapter == null || !isBluetoothEnabled()) {
            Log.e(TAG, "Cannot set Bluetooth name - adapter null or disabled");
            return false;
        }

        if (name == null || name.trim().isEmpty()) {
            Log.e(TAG, "Cannot set empty Bluetooth name");
            return false;
        }

        if (name.length() > MAX_DEVICE_NAME_LENGTH) {
            Log.e(TAG, "Bluetooth name too long: " + name.length() + " > " + MAX_DEVICE_NAME_LENGTH);
            return false;
        }

        try {
            // Store original name if not already stored
            if (originalBluetoothName == null) {
                originalBluetoothName = bluetoothAdapter.getName();
            }

            boolean success = bluetoothAdapter.setName(name);
            if (success) {
                Log.d(TAG, "✅ Bluetooth name set to: " + name);
            } else {
                Log.w(TAG, "❌ Failed to set Bluetooth name to: " + name);
            }
            return success;

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception setting Bluetooth name", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error setting Bluetooth name", e);
            return false;
        }
    }

    /**
     * Restore original Bluetooth name
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void restoreOriginalBluetoothName() {
        if (originalBluetoothName != null && !originalBluetoothName.trim().isEmpty()) {
            boolean success = setBluetoothName(originalBluetoothName);
            if (success) {
                Log.d(TAG, "✅ Restored original Bluetooth name: " + originalBluetoothName);
            }
        } else {
            Log.w(TAG, "No original Bluetooth name to restore");
        }
    }

    // =========================== DISCOVERABILITY OPERATIONS ===========================

    /**
     * Make device discoverable (request discoverability)
     * Note: This returns an Intent that should be launched via Activity
     */
    public android.content.Intent getDiscoverableIntent(int duration) {
        android.content.Intent discoverableIntent = new android.content.Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
        return discoverableIntent;
    }

    /**
     * Get default discoverable intent with standard duration
     */
    public android.content.Intent getDiscoverableIntent() {
        return getDiscoverableIntent(DEFAULT_DISCOVERABLE_DURATION);
    }

    /**
     * Check if device is currently discoverable
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public boolean isDiscoverable() {
        if (bluetoothAdapter == null || !isBluetoothEnabled()) {
            return false;
        }

        try {
            int scanMode = bluetoothAdapter.getScanMode();
            return scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception checking discoverable mode", e);
            return false;
        }
    }

    /**
     * Get current scan mode
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public int getScanMode() {
        if (bluetoothAdapter == null || !isBluetoothEnabled()) {
            return BluetoothAdapter.SCAN_MODE_NONE;
        }

        try {
            return bluetoothAdapter.getScanMode();
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting scan mode", e);
            return BluetoothAdapter.SCAN_MODE_NONE;
        }
    }

    /**
     * Get scan mode as human-readable string
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public String getScanModeString() {
        int scanMode = getScanMode();
        switch (scanMode) {
            case BluetoothAdapter.SCAN_MODE_NONE:
                return "Not Discoverable";
            case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                return "Connectable";
            case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                return "Discoverable";
            default:
                return "Unknown";
        }
    }

    // =========================== STATE MANAGEMENT ===========================

    /**
     * Get Bluetooth state as human-readable string
     */
    public String getBluetoothStateString() {
        if (bluetoothAdapter == null) {
            return "Not Supported";
        }

        int state = bluetoothAdapter.getState();
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
                return "Disabled";
            case BluetoothAdapter.STATE_ON:
                return "Enabled";
            case BluetoothAdapter.STATE_TURNING_OFF:
                return "Turning Off";
            case BluetoothAdapter.STATE_TURNING_ON:
                return "Turning On";
            default:
                return "Unknown State";
        }
    }

    /**
     * Get Bluetooth state as integer
     */
    public int getBluetoothState() {
        if (bluetoothAdapter == null) {
            return BluetoothAdapter.ERROR;
        }
        return bluetoothAdapter.getState();
    }

    /**
     * Check if Bluetooth operations are currently available
     */
    public boolean isOperational() {
        return isBluetoothSupported() &&
                isBluetoothEnabled() &&
                hasPermissions();
    }

    /**
     * Get a user-friendly status message
     */
    public String getStatusMessage() {
        if (!isBluetoothSupported()) {
            return "Bluetooth not supported on this device";
        }

        if (!isBluetoothEnabled()) {
            return "Bluetooth is disabled. Please enable it in settings.";
        }

        if (!hasPermissions()) {
            return "Bluetooth permissions not granted. Please allow in app settings.";
        }

        if (isDiscovering()) {
            return "Scanning for attendance sessions...";
        }

        return "Bluetooth ready for attendance detection";
    }

    // =========================== ATTENDANCE-SPECIFIC OPERATIONS ===========================

    /**
     * SessionInfo class for parsing teacher device information
     */
    public static class SessionInfo {
        public String sessionId;
        public String subject;
        public String branch;
        public String year;
        public String teacherName;
        public boolean isValid;

        public SessionInfo() {
            this.isValid = false;
        }
    }

    /**
     * StudentInfo class for parsing student device information
     */
    public static class StudentInfo {
        public String studentId;
        public String studentName;
        public String branch;
        public String year;
        public boolean isValid;

        public StudentInfo() {
            this.isValid = false;
        }
    }

    /**
     * DeviceInfo class for general device information
     */
    public static class DeviceInfo {
        public String deviceType; // "teacher", "student", "unknown"
        public String identifier; // sessionId for teacher, studentId for student
        public String name; // subject for teacher, studentName for student
        public String branch;
        public String year;
        public boolean isValid;

        public DeviceInfo() {
            this.isValid = false;
            this.deviceType = "unknown";
        }
    }

    /**
     * Check if a device name indicates it's a teacher device
     */
    public static boolean isTeacherDevice(String deviceName) {
        if (deviceName == null) return false;

        return deviceName.startsWith("ATT_") ||
                deviceName.startsWith("Session-") ||
                deviceName.startsWith("TeacherDevice-") ||
                deviceName.startsWith("AttendanceSession-") ||
                deviceName.contains("Teacher");
    }

    /**
     * Check if a device name indicates it's a student device
     */
    public static boolean isGenericStudentDevice(String deviceName) {
        if (deviceName == null) return false;

        return deviceName.startsWith("Student-") ||
                deviceName.startsWith("ATT_Student") ||
                deviceName.contains("AttendanceApp") ||
                deviceName.contains("Student");
    }

    /**
     * Extract session information from teacher device name
     */
    public static SessionInfo parseSessionInfo(String deviceName) {
        SessionInfo info = new SessionInfo();

        if (deviceName == null) {
            return info;
        }

        try {
            // Pattern: ATT_{branch}_{year}_{subject}_{sessionId}
            if (deviceName.startsWith("ATT_")) {
                String[] parts = deviceName.substring(4).split("_"); // Remove "ATT_"
                if (parts.length >= 4) {
                    info.branch = parts[0];
                    info.year = parts[1];
                    info.subject = parts[2];
                    info.sessionId = parts[3];
                    info.isValid = true;
                }
            }
            // Pattern: Session-{branch}-{year}-{subject}-{sessionId}
            else if (deviceName.startsWith("Session-")) {
                String[] parts = deviceName.split("-");
                if (parts.length >= 5) {
                    info.branch = parts[1];
                    info.year = parts[2];
                    info.subject = parts[3];
                    info.sessionId = parts[4];
                    info.isValid = true;
                }
            }
            // Pattern: TeacherDevice-{branch}-{year}-{subject}-{sessionId}
            else if (deviceName.startsWith("TeacherDevice-")) {
                String[] parts = deviceName.split("-");
                if (parts.length >= 5) {
                    info.branch = parts[1];
                    info.year = parts[2];
                    info.subject = parts[3];
                    info.sessionId = parts[4];
                    info.isValid = true;
                }
            }

            Log.d(TAG, "Parsed session info - Valid: " + info.isValid +
                    ", Branch: " + info.branch +
                    ", Year: " + info.year +
                    ", Subject: " + info.subject +
                    ", SessionID: " + info.sessionId);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing session info from: " + deviceName, e);
            info.isValid = false;
        }

        return info;
    }

    /**
     * Extract student information from student device name
     */
    public static StudentInfo parseStudentInfo(String deviceName) {
        StudentInfo info = new StudentInfo();

        if (deviceName == null) {
            return info;
        }

        try {
            // Pattern: Student-{studentId}-{name}-{branch}-{year}
            if (deviceName.startsWith("Student-")) {
                String[] parts = deviceName.split("-");
                if (parts.length >= 3) {
                    info.studentId = parts[1];
                    info.studentName = parts[2].replace("_", " ");
                    if (parts.length >= 4) {
                        info.branch = parts[3];
                    }
                    if (parts.length >= 5) {
                        info.year = parts[4];
                    }
                    info.isValid = true;
                }
            }
            // Pattern: ATT_Student-{studentId}-{name}
            else if (deviceName.startsWith("ATT_Student-")) {
                String[] parts = deviceName.substring(12).split("-"); // Remove "ATT_Student-"
                if (parts.length >= 2) {
                    info.studentId = parts[0];
                    info.studentName = parts[1].replace("_", " ");
                    info.isValid = true;
                }
            }

            Log.d(TAG, "Parsed student info - Valid: " + info.isValid +
                    ", ID: " + info.studentId +
                    ", Name: " + info.studentName);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing student info from: " + deviceName, e);
            info.isValid = false;
        }

        return info;
    }

    /**
     * Extract basic device information from any device name
     */
    public static DeviceInfo parseDeviceInfo(String deviceName) {
        DeviceInfo info = new DeviceInfo();

        if (deviceName == null) {
            return info;
        }

        if (isTeacherDevice(deviceName)) {
            SessionInfo sessionInfo = parseSessionInfo(deviceName);
            if (sessionInfo.isValid) {
                info.deviceType = "teacher";
                info.identifier = sessionInfo.sessionId;
                info.name = sessionInfo.subject;
                info.branch = sessionInfo.branch;
                info.year = sessionInfo.year;
                info.isValid = true;
            }
        } else if (isGenericStudentDevice(deviceName)) {
            StudentInfo studentInfo = parseStudentInfo(deviceName);
            if (studentInfo.isValid) {
                info.deviceType = "student";
                info.identifier = studentInfo.studentId;
                info.name = studentInfo.studentName;
                info.branch = studentInfo.branch;
                info.year = studentInfo.year;
                info.isValid = true;
            }
        }

        return info;
    }

    // =========================== DEVICE NAME FORMATTING ===========================

    /**
     * Format device name for attendance session (teacher side)
     */
    public static String formatTeacherDeviceName(String branch, String year, String subject, String sessionId) {
        if (branch == null || year == null || subject == null || sessionId == null) {
            return null;
        }

        return DEFAULT_SESSION_PREFIX + branch + "_" + year + "_" + subject + "_" + sessionId;
    }

    /**
     * Format device name for student device
     */
    public static String formatStudentDeviceName(String studentId, String studentName, String branch, String year) {
        if (studentId == null || studentName == null) {
            return null;
        }

        String formattedName = studentName.replace(" ", "_");

        if (branch != null && year != null) {
            return DEFAULT_STUDENT_PREFIX + studentId + "-" + formattedName + "-" + branch + "-" + year;
        } else {
            return "ATT_Student-" + studentId + "-" + formattedName;
        }
    }

    // =========================== VALIDATION METHODS ===========================

    /**
     * Validate if a Bluetooth device name matches expected attendance device pattern
     */
    public boolean isValidAttendanceDevice(String deviceName, String expectedBranch, String expectedYear) {
        if (deviceName == null || expectedBranch == null || expectedYear == null) {
            return false;
        }

        // Support multiple device name patterns for flexibility
        String[] validPatterns = {
                "Attendance-" + expectedBranch + "-" + expectedYear + "-",
                "AttendanceSession-" + expectedBranch + "-" + expectedYear + "-",
                "TeacherDevice-" + expectedBranch + "-" + expectedYear + "-",
                "Session-" + expectedBranch + "-" + expectedYear + "-",
                DEFAULT_SESSION_PREFIX + expectedBranch + "_" + expectedYear + "_"
        };

        for (String pattern : validPatterns) {
            if (deviceName.startsWith(pattern)) {
                Log.d(TAG, "✅ Device name matches pattern: " + pattern);
                return true;
            }
        }

        Log.d(TAG, "❌ Device name doesn't match any pattern: " + deviceName);
        return false;
    }

    /**
     * Validate if a session device name matches expected criteria
     */
    public boolean isValidSessionDevice(String deviceName, String expectedBranch, String expectedYear, String expectedSubject) {
        if (deviceName == null || expectedBranch == null || expectedYear == null || expectedSubject == null) {
            return false;
        }

        SessionInfo sessionInfo = parseSessionInfo(deviceName);

        return sessionInfo.isValid &&
                expectedBranch.equals(sessionInfo.branch) &&
                expectedYear.equals(sessionInfo.year) &&
                expectedSubject.equals(sessionInfo.subject);
    }

    /**
     * Validate session ID format
     */
    public static boolean isValidSessionId(String sessionId) {
        return sessionId != null &&
                sessionId.length() >= 6 &&
                sessionId.length() <= 12 &&
                sessionId.matches("[A-Za-z0-9]+");
    }

    // =========================== UTILITY METHODS ===========================

    /**
     * Get device address from BluetoothDevice
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public static String getDeviceAddress(BluetoothDevice device) {
        if (device == null) {
            return null;
        }

        try {
            return device.getAddress();
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied getting device address", e);
            return null;
        }
    }

    /**
     * Get device name from BluetoothDevice
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public static String getDeviceName(BluetoothDevice device) {
        if (device == null) {
            return null;
        }

        try {
            return device.getName();
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied getting device name", e);
            return null;
        }
    }

    /**
     * Check if two devices are the same based on address
     */
    public static boolean isSameDevice(BluetoothDevice device1, BluetoothDevice device2) {
        if (device1 == null || device2 == null) {
            return false;
        }

        String address1 = device1.getAddress();
        String address2 = device2.getAddress();

        return address1 != null && address1.equals(address2);
    }

    /**
     * Get signal strength category from RSSI value
     */
    public static String getSignalStrengthCategory(int rssi) {
        if (rssi >= -30) {
            return "Excellent";
        } else if (rssi >= -50) {
            return "Good";
        } else if (rssi >= -70) {
            return "Fair";
        } else if (rssi >= -80) {
            return "Weak";
        } else {
            return "Very Weak";
        }
    }

    /**
     * Check if RSSI indicates device is in acceptable range
     */
    public static boolean isInAcceptableRange(int rssi, int minRssi) {
        return rssi >= minRssi;
    }

    /**
     * Generate a unique session ID
     */
    public static String generateSessionId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Get current timestamp for logging
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * Format timestamp to readable string
     */
    public static String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    /**
     * Format timestamp to date string
     */
    public static String formatDateTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // =========================== DIAGNOSTIC AND MAINTENANCE ===========================

    /**
     * Get diagnostic information for troubleshooting
     */
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    public String getDiagnosticInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== Bluetooth Diagnostic Info ===\n");
        info.append("Bluetooth Supported: ").append(isBluetoothSupported()).append("\n");
        info.append("Bluetooth State: ").append(getBluetoothStateString()).append("\n");
        info.append("BLE Supported: ").append(isBleSupported()).append("\n");
        info.append("Has Permissions: ").append(hasPermissions()).append("\n");
        info.append("Has Connect Permission: ").append(hasConnectPermission()).append("\n");
        info.append("Has Scan Permission: ").append(hasScanPermission()).append("\n");
        info.append("Currently Discovering: ").append(isDiscovering()).append("\n");
        info.append("Discoverable: ").append(isDiscoverable()).append("\n");
        info.append("Scan Mode: ").append(getScanModeString()).append("\n");
        info.append("Android Version: ").append(Build.VERSION.SDK_INT).append("\n");
        info.append("Last Discovery Start: ").append(lastDiscoveryStart).append("\n");

        if (isBluetoothEnabled() && hasConnectPermission()) {
            try {
                info.append("Current Name: ").append(getBluetoothName()).append("\n");
                info.append("Original Name: ").append(originalBluetoothName != null ? originalBluetoothName : "Not stored").append("\n");
                Set<BluetoothDevice> paired = getPairedDevices();
                info.append("Paired Devices: ").append(paired != null ? paired.size() : 0).append("\n");
            } catch (SecurityException e) {
                info.append("Additional info unavailable due to permissions\n");
            }
        }

        info.append("================================");
        return info.toString();
    }

    /**
     * Reset the Bluetooth helper state
     */
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    public void reset() {
        try {
            // Cancel any ongoing discovery
            if (isDiscovering()) {
                cancelDiscovery();
            }

            // Restore original name if we changed it
            if (originalBluetoothName != null) {
                restoreOriginalBluetoothName();
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied during reset", e);
        }

        // Reset internal state
        isDiscovering = false;
        lastDiscoveryStart = 0;
        Log.d(TAG, "BluetoothHelper reset completed");
    }

    /**
     * Cleanup resources when helper is no longer needed
     */
    public void cleanup() {
        try {
            if (hasPermissions()) {
                reset();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied during cleanup", e);
        }

        Log.d(TAG, "BluetoothHelper cleanup completed");
    }
}