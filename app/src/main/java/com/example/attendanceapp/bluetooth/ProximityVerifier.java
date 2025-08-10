package com.example.attendanceapp.bluetooth;

import android.bluetooth.BluetoothDevice;
import java.lang.reflect.Method;

public class ProximityVerifier {

    public static boolean isWithinRange(BluetoothDevice device, double maxDistance) {
        try {
            Method getRssiMethod = BluetoothDevice.class.getMethod("getRssi");
            int rssi = (int) getRssiMethod.invoke(device);
            double distance = calculateDistance(rssi);
            return distance <= maxDistance;
        } catch (Exception e) {
            return false;
        }
    }

    private static double calculateDistance(int rssi) {
        // Distance approximation formula (in meters)
        double txPower = -59; // RSSI at 1 meter - should be calibrated per device
        return Math.pow(10, (txPower - rssi) / (10 * 2)); // 2 = path loss exponent
    }
}