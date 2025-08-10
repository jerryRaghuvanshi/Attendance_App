package com.example.attendanceapp.LoginScreen;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.attendanceapp.Dashboards.StudentDashboard;
import com.example.attendanceapp.Dashboards.SuperAdminDashboard;
import com.example.attendanceapp.Dashboards.TeacherDashboard;
import com.example.attendanceapp.R;
import com.example.attendanceapp.RoleSelectionActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (validateInput(email, password)) {
                progressBar.setVisibility(View.VISIBLE);
                loginUser(email, password);
            }
        });

        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RoleSelectionActivity.class)));
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            return false;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            return false;
        }
        if (password.length() < 8) {
            etPassword.setError("Password must be at least 8 characters");
            return false;
        }
        return true;
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        if (mAuth.getCurrentUser() != null) {
                            checkUserRole(mAuth.getCurrentUser().getUid());
                        } else {
                            showToast("Authentication failed: User not found");
                        }
                    } else {
                        showToast("Authentication failed: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private void checkUserRole(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        Boolean approved = documentSnapshot.getBoolean("approved");

                        if ("teacher".equalsIgnoreCase(role)) {
                            if (approved == null || !approved) {
                                showToast("Teacher account not yet approved");
                                FirebaseAuth.getInstance().signOut();
                                return;
                            }
                        }

                        if (role != null) {
                            redirectToDashboard(role);
                        } else {
                            showToast("User role not found");
                        }
                    } else {
                        showToast("User data not found");
                    }
                })
                .addOnFailureListener(e -> showToast("Error: " + e.getMessage()));
    }

    private void redirectToDashboard(String role) {
        Log.d("LoginDebug", "Redirecting to dashboard for role: " + role);
        Intent intent;
        switch (role.toLowerCase()) {
            case "student":
                intent = new Intent(this, StudentDashboard.class);
                break;
            case "teacher":
                intent = new Intent(this, TeacherDashboard.class);
                break;
            case "director":
                intent = new Intent(this, SuperAdminDashboard.class);
                break;
            default:
                showToast("Unknown user role: " + role);
                return;
        }
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            checkUserRole(userId);
        }
    }
}