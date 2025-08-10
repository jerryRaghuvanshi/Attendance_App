package com.example.attendanceapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.attendanceapp.Dashboards.StudentDashboard;
import com.example.attendanceapp.LoginScreen.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 15;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // Check if user is already logged in
        if (mAuth.getCurrentUser() != null) {
            redirectToDashboard();
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void redirectToDashboard() {
        // This would be replaced with your role-checking logic
        startActivity(new Intent(this, StudentDashboard.class));
        finish();
    }
    private void checkPermissions() {
        List<String> requiredPermissions = new ArrayList<>();

        // Bluetooth permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions.add(android.Manifest.permission.BLUETOOTH_SCAN);
            requiredPermissions.add(android.Manifest.permission.BLUETOOTH_CONNECT);
        }

        // Location permissions
        requiredPermissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);

        List<String> missingPermissions = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    missingPermissions.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE
            );
        }
    }
}