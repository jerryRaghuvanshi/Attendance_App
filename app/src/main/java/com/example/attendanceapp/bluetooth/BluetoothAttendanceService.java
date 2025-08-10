package com.example.attendanceapp.bluetooth;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.attendanceapp.Dashboards.StudentDashboard;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BluetoothAttendanceService extends Service {

    private static final String TAG = "BluetoothAttendance";
    private static final String CHANNEL_ID = "BluetoothAttendanceChannel";
    private static final int NOTIFICATION_ID = 101;
    private static final int REQUEST_BLUETOOTH_CONNECT_PERMISSION = 1001;
    private static final int REQUEST_BLUETOOTH_SCAN_PERMISSION = 1002;
    private static final int REQUEST_LOCATION_PERMISSION = 1003;
    private static final long SCAN_DURATION = 3600000; // 1 hour
    private static final long ATTENDANCE_COOLDOWN = 300000; // 5 minutes

    // UUIDs for your custom Bluetooth service
    private static final UUID SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private PowerOptimizationManager powerManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Set<String> processedDevices = new HashSet<>();
    private final Map<String, Long> attendanceTimestamps = new ConcurrentHashMap<>();
    private final Map<String, String> deviceStudentCache = new ConcurrentHashMap<>();
    private int lastRssi = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        powerManager = new PowerOptimizationManager(bluetoothAdapter);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("START_SCAN".equals(action)) {
                startOptimizedScanning();
            } else if ("CONNECT_DEVICE".equals(action) && intent.hasExtra("device_address")) {
                String deviceAddress = intent.getStringExtra("device_address");
                connectToDevice(deviceAddress);
            } else if ("STOP_SERVICE".equals(action)) {
                stopSelf();
            }
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Bluetooth Attendance Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Attendance Tracking Active")
                .setContentText("Scanning for nearby devices")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void connectToDevice(String deviceAddress) {
        if (bluetoothAdapter == null || deviceAddress == null) {
            Log.e(TAG, "BluetoothAdapter not initialized or invalid device address");
            return;
        }

        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Bluetooth connect permission not granted");
                return;
            }

            if (bluetoothGatt != null) {
                bluetoothGatt.close();
            }

            bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
            Log.d(TAG, "Attempting to connect to device: " + deviceAddress);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid device address: " + deviceAddress, e);
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to device", e);
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Connection state change error: " + status);
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to device");
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Bluetooth connect permission not granted");
                    return;
                }

                // Discover services after a small delay
                handler.postDelayed(() -> {
                    if (!gatt.discoverServices()) {
                        Log.e(TAG, "Failed to start service discovery");
                    }
                }, 500);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from device");
                closeGatt();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered");

                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service == null) {
                    Log.e(TAG, "Service not found: " + SERVICE_UUID);
                    return;
                }

                BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                if (characteristic == null) {
                    Log.e(TAG, "Characteristic not found: " + CHARACTERISTIC_UUID);
                    return;
                }

                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Bluetooth connect permission not granted");
                    return;
                }

                if (!gatt.readCharacteristic(characteristic)) {
                    Log.e(TAG, "Failed to read characteristic");
                }
            } else {
                Log.e(TAG, "Service discovery failed: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    processAttendanceData(data);
                } else {
                    Log.w(TAG, "Empty characteristic data");
                }
            } else {
                Log.e(TAG, "Characteristic read failed: " + status);
            }

            // Disconnect after reading
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED) {
                gatt.disconnect();
            }
        }
    };

    private void processAttendanceData(byte[] data) {
        try {
            String macAddress = new String(data).trim();
            Log.d(TAG, "Received attendance data from: " + macAddress);

            if (!isDeviceWhitelisted(macAddress)) {
                Log.d(TAG, "Device not whitelisted: " + macAddress);
                return;
            }

            if (isAttendanceCooldownActive(macAddress)) {
                Log.d(TAG, "Attendance cooldown active for: " + macAddress);
                return;
            }

            getStudentIdFromDevice(macAddress, new DeviceCallback() {
                @Override
                public void onSuccess(String studentId) {
                    markAttendance(studentId, macAddress);
                    attendanceTimestamps.put(macAddress, System.currentTimeMillis());
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Failed to get student ID: " + error);
                }
            }, 3); // 3 retries
        } catch (Exception e) {
            Log.e(TAG, "Error processing attendance data", e);
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startOptimizedScanning() {
        if (!checkPermissions()) {
            Log.e(TAG, "Required permissions not granted");
            stopSelf();
            return;
        }

        powerManager.startOptimizedScanning(new PowerOptimizationManager.ScanCallback() {
            @Override
            public void onDeviceDetected(BluetoothDevice device, int rssi) {
                lastRssi = rssi;
                processDetectedDevice(device);
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "Scan failed with error: " + errorCode);
                // Attempt to restart scan after delay
                handler.postDelayed(() -> startOptimizedScanning(), 5000);
            }
        });

        // Automatically stop after defined duration
        handler.postDelayed(() -> {
            powerManager.stopOptimizedScanning();
            stopSelf();
        }, SCAN_DURATION);
    }
    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isDeviceWhitelisted(String macAddress) {
        // Check cache first
        if (deviceStudentCache.containsKey(macAddress)) {
            return true;
        }

        // TODO: Implement actual whitelist check from Firestore or local database
        return true;
    }

    private boolean isWithinRange(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        try {
            Method getRssiMethod = BluetoothDevice.class.getMethod("getRssi");
            int rssi = (int) getRssiMethod.invoke(device);
            return calculateDistance(rssi) <= 10.0; // 10 meters
        } catch (Exception e) {
            Log.e(TAG, "Error getting RSSI", e);
            return false;
        }
    }

    private double calculateDistance(int rssi) {
        // Improved distance approximation formula (in meters)
        if (rssi == 0) {
            return -1.0; // cannot determine distance
        }

        double txPower = -59; // RSSI at 1 meter
        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
        }
    }

    private void processDetectedDevice(BluetoothDevice device) {
        if (device == null) return;

        String macAddress = device.getAddress();
        if (processedDevices.contains(macAddress)) {
            return; // Skip already processed devices
        }

        if (!isWithinRange(device)) {
            return; // Skip devices out of range
        }

        processedDevices.add(macAddress);
        connectToDevice(macAddress);
    }

    private boolean isAttendanceCooldownActive(String macAddress) {
        Long lastTimestamp = attendanceTimestamps.get(macAddress);
        if (lastTimestamp == null) {
            return false;
        }
        return (System.currentTimeMillis() - lastTimestamp) < ATTENDANCE_COOLDOWN;
    }

    private void markAttendance(String studentId, String macAddress) {
        if (studentId == null || studentId.isEmpty()) {
            Log.e(TAG, "Invalid student ID");
            return;
        }

        Map<String, Object> attendance = new HashMap<>();
        attendance.put("studentId", studentId);
        attendance.put("timestamp", FieldValue.serverTimestamp());
        attendance.put("macAddress", macAddress);
        attendance.put("rssi", lastRssi);
        attendance.put("location", "Classroom 101"); // TODO: Add actual location

        FirebaseFirestore.getInstance().collection("attendance")
                .add(attendance)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Attendance marked for student: " + studentId);
                    sendAttendanceBroadcast(studentId, true, "Attendance marked successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking attendance", e);
                    sendAttendanceBroadcast(studentId, false, "Failed to mark attendance");
                });
    }

    private void sendAttendanceBroadcast(String studentId, boolean success, String message) {
        Intent intent = new Intent("ATTENDANCE_UPDATE");
        intent.putExtra("studentId", studentId);
        intent.putExtra("success", success);
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void getStudentIdFromDevice(String macAddress, DeviceCallback callback, int retryCount) {
        // Check cache first
        if (deviceStudentCache.containsKey(macAddress)) {
            callback.onSuccess(deviceStudentCache.get(macAddress));
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("student_devices")
                .document(macAddress)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String studentId = document.getString("studentId");
                        if (studentId != null && !studentId.isEmpty()) {
                            deviceStudentCache.put(macAddress, studentId);
                            callback.onSuccess(studentId);
                        } else {
                            callback.onFailure("Invalid student ID in record");
                        }
                    } else {
                        callback.onFailure("Device not registered");
                    }
                })
                .addOnFailureListener(e -> {
                    if (retryCount > 0) {
                        handler.postDelayed(() ->
                                        getStudentIdFromDevice(macAddress, callback, retryCount - 1),
                                1000 // 1 second delay
                        );
                    } else {
                        callback.onFailure("Network error: " + e.getMessage());
                    }
                });
    }

    private void closeGatt() {
        if (bluetoothGatt != null) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED) {
                    bluetoothGatt.disconnect();
                    bluetoothGatt.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error closing GATT", e);
            } finally {
                bluetoothGatt = null;
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        powerManager.stopOptimizedScanning();
        closeGatt();

        // Clear caches
        processedDevices.clear();
        attendanceTimestamps.clear();
        deviceStudentCache.clear();

        Log.d(TAG, "Service destroyed");
    }

    public interface DeviceCallback {
        void onSuccess(String studentId);
        void onFailure(String error);
    }
    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            int rssi = result.getRssi();
            lastRssi = rssi;
            processDetectedDevice(device);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (android.bluetooth.le.ScanResult result : results) {
                BluetoothDevice device = result.getDevice();
                int rssi = result.getRssi();
                lastRssi = rssi;
                processDetectedDevice(device);
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "BLE Scan failed with error code: " + errorCode);
            // Handle different error codes appropriately
            switch (errorCode) {
                case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                    Log.e(TAG, "Scan already started");
                    break;
                case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    Log.e(TAG, "Application registration failed");
                    break;
                case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                    Log.e(TAG, "Feature unsupported");
                    break;
                case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                    Log.e(TAG, "Internal error");
                    break;
                default:
                    Log.e(TAG, "Unknown error");
            }

            // Attempt to restart scan after delay
            handler.postDelayed(() -> startOptimizedScanning(), 5000);
        }
    };
}