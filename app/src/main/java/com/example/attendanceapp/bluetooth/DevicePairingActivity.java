package com.example.attendanceapp.bluetooth;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.attendanceapp.R;
import com.example.attendanceapp.adaptors.DevicePairingAdapter;
import com.example.attendanceapp.models.StudentDevice;
import com.example.attendanceapp.models.WhitelistedDevice;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class DevicePairingActivity extends AppCompatActivity {

    private RecyclerView rvDevices;
    private DevicePairingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_pairing);

        rvDevices = findViewById(R.id.rvDevices);
        rvDevices.setLayoutManager(new LinearLayoutManager(this));

        // Initialize with empty list
        adapter = new DevicePairingAdapter(new ArrayList<>(), this::onDeviceStatusChanged);
        rvDevices.setAdapter(adapter);

        loadWhitelistedDevices();
    }

    private void loadWhitelistedDevices() {
        FirebaseFirestore.getInstance()
                .collection("whitelisted_devices")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<WhitelistedDevice> devices = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        WhitelistedDevice device = doc.toObject(WhitelistedDevice.class);
                        devices.add(device);
                    }
                    adapter.updateDevices(devices);
                });
    }
    public void onDeviceStatusChanged(WhitelistedDevice device, boolean enabled) {
        // Update in Firestore
        FirebaseFirestore.getInstance()
                .collection("whitelisted_devices")
                .document(device.getDeviceId())
                .update("enabled", enabled);
    }

}