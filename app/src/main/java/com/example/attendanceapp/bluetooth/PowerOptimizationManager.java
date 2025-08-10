package com.example.attendanceapp.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PowerOptimizationManager {
    private static final String TAG = "PowerOptimizationManager";
    private static final long SCAN_TIMEOUT = 120000; // 2 minutes active scanning
    private static final long SCAN_INTERVAL = 30000; // 30 seconds between scans

    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner leScanner;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isScanning = false;

    public interface ScanCallback {
        void onDeviceDetected(BluetoothDevice device, int rssi);
        void onScanFailed(int errorCode);
    }

    private ScanCallback externalCallback;

    public PowerOptimizationManager(BluetoothAdapter adapter) {
        this.bluetoothAdapter = adapter;
        if (bluetoothAdapter != null) {
            this.leScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void startOptimizedScanning(ScanCallback callback) {
        if (isScanning) {
            Log.w(TAG, "Scan already in progress");
            return;
        }

        this.externalCallback = callback;
        isScanning = true;
        startScanCycle();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startScanCycle() {
        if (!isScanning || leScanner == null) {
            return;
        }

        // Configure scan settings for low power
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setReportDelay(1000) // Batch results to save power
                .build();

        // Optional: Add filters if you only want specific devices
        List<ScanFilter> filters = new ArrayList<>();
        // filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(SERVICE_UUID)).build());

        // Start the scan
        leScanner.startScan(filters, settings, leScanCallback);
        Log.d(TAG, "BLE scanning started");

        // Schedule scan timeout
        handler.postDelayed(this::stopScanPhase, SCAN_TIMEOUT);
    }

    private final android.bluetooth.le.ScanCallback leScanCallback = new android.bluetooth.le.ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (externalCallback != null) {
                externalCallback.onDeviceDetected(result.getDevice(), result.getRssi());
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                if (externalCallback != null) {
                    externalCallback.onDeviceDetected(result.getDevice(), result.getRssi());
                }
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE Scan failed with error: " + errorCode);
            if (externalCallback != null) {
                externalCallback.onScanFailed(errorCode);
            }
            // Auto-restart after short delay
            handler.postDelayed(() -> startScanCycle(), 5000);
        }
    };

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void stopScanPhase() {
        if (leScanner != null) {
            try {
                leScanner.stopScan(leScanCallback);
                Log.d(TAG, "BLE scanning stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping BLE scan", e);
            }
        }

        // Schedule next scan cycle if still active
        if (isScanning) {
            handler.postDelayed(this::startScanCycle, SCAN_INTERVAL);
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void stopOptimizedScanning() {
        isScanning = false;
        handler.removeCallbacksAndMessages(null);
        stopScanPhase();
    }

    public boolean isScanning() {
        return isScanning;
    }
}