package com.example.attendanceapp.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BluetoothStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Toast.makeText(context, "Bluetooth is disabled", Toast.LENGTH_SHORT).show();
                    // Notify user Bluetooth is disabled
                    break;
                case BluetoothAdapter.STATE_ON:
                    // Re-enable Bluetooth features
                    break;

            }
        }
    }
}