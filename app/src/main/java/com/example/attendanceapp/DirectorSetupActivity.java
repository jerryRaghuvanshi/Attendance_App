package com.example.attendanceapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.attendanceapp.Dashboards.SuperAdminDashboard;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.attendanceapp.AdminManager; // Replace with your package name
import com.example.attendanceapp.R; // Replace with your package name

import java.util.HashMap;
import java.util.Map;

public class DirectorSetupActivity extends AppCompatActivity {
    private static final String TAG = "DirectorSetup";

    private EditText etEmail, etPassword, etName;
    private Button btnCreateDirector;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AdminManager adminManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_director_setup);

        initializeViews();
        initializeFirebase();

        btnCreateDirector.setOnClickListener(v -> createDirector());
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etName = findViewById(R.id.et_name);
        btnCreateDirector = findViewById(R.id.btn_create_director);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        adminManager = new AdminManager();
    }

    private void createDirector() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String name = etName.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // First check if director already exists
        db.collection("directors").document("director")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            showLoading(false);
                            Toast.makeText(this, "Director already exists!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Create director account
                        createDirectorAccount(email, password, name);
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Error checking director existence", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createDirectorAccount(String email, String password, String name) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Save director directly to Firestore (no custom claims)
                            saveDirectorToFirestore(user.getUid(), name, email);
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Failed to create director account: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Director creation failed", task.getException());
                    }
                });
    }

    private void setDirectorClaims(String uid, String name, String email) {
        adminManager.setUserRole(uid, "director")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Save director info to Firestore
                        saveDirectorToFirestore(uid, name, email);
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Failed to set director privileges", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to set custom claims", task.getException());
                    }
                });
    }

    private void saveDirectorToFirestore(String uid, String name, String email) {
        // Create director document
        Map<String, Object> directorData = new HashMap<>();
        directorData.put("uid", uid);
        directorData.put("name", name);
        directorData.put("email", email);
        directorData.put("createdAt", System.currentTimeMillis());

        // Save to directors collection
        db.collection("directors").document("director")
                .set(directorData)
                .addOnCompleteListener(directorTask -> {
                    if (directorTask.isSuccessful()) {
                        // Save to users collection
                        UserData userData = new UserData(name, email, "director", true);
                        db.collection("users").document(uid)
                                .set(userData)
                                .addOnCompleteListener(userTask -> {
                                    showLoading(false);
                                    if (userTask.isSuccessful()) {
                                        Toast.makeText(this, "Director created successfully!", Toast.LENGTH_SHORT).show();

                                        Intent intent = new Intent(this, SuperAdminDashboard.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(this, "Failed to save director data", Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Failed to save director to users", userTask.getException());
                                    }
                                });
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Failed to create director document", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to save director document", directorTask.getException());
                    }
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnCreateDirector.setEnabled(!show);
    }

    // Data class for user information
    public static class UserData {
        public String name;
        public String email;
        public String role;
        public boolean approved;

        public UserData() {} // Required for Firestore

        public UserData(String name, String email, String role, boolean approved) {
            this.name = name;
            this.email = email;
            this.role = role;
            this.approved = approved;
        }
    }
}