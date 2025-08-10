package com.example.attendanceapp;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceapp.bluetooth.BluetoothAttendanceService;
import com.example.attendanceapp.bluetooth.BluetoothDeviceAdapter;
import com.example.attendanceapp.bluetooth.BluetoothHelper;
import java.util.List;

public class BluetoothScannerActivity extends AppCompatActivity
        implements BluetoothDeviceAdapter.OnDeviceClickListener {

    private static final int BLUETOOTH_PERMISSION_CODE = 100;
    private BluetoothHelper bluetoothHelper;
    private RecyclerView rvDevices;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_scanner);

        bluetoothHelper = new BluetoothHelper(this);
        rvDevices = findViewById(R.id.rvDevices);
        rvDevices.setLayoutManager(new LinearLayoutManager(this));

        checkPermissionsAndSetup();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void checkPermissionsAndSetup() {
        if (bluetoothHelper.hasPermissions()) {
            setupBluetooth();
        } else {
            requestPermissions();
        }
    }

    private void requestPermissions() {
        String[] permissions;
        permissions = new String[]{
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        requestPermissions(permissions, BLUETOOTH_PERMISSION_CODE);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void setupBluetooth() {
        if (!bluetoothHelper.isBluetoothSupported()) {
            showError();
            return;
        }

        if (!bluetoothHelper.isBluetoothEnabled()) {
            // You might want to prompt user to enable Bluetooth
            return;
        }

        showPairedDevices();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void showPairedDevices() {
        List<BluetoothDevice> devices = (List<BluetoothDevice>) bluetoothHelper.getPairedDevices();
        BluetoothDeviceAdapter adapter = new BluetoothDeviceAdapter(devices, this);
        rvDevices.setAdapter(adapter);
    }

    @Override
    public void onDeviceClick(BluetoothDevice device) {
        // Start attendance service with selected device
        Intent serviceIntent = new Intent(this, BluetoothAttendanceService.class);
        serviceIntent.putExtra("device_address", device.getAddress());
        startService(serviceIntent);

        // Optionally, show a message or navigate back
        finish();
    }

    private void showError() {
        Toast.makeText(this, " Error in Teacher Dashboard !!!!", Toast.LENGTH_SHORT).show();
        // Implement your error display logic
    }
}