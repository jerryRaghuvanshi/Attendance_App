package com.example.attendanceapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.attendanceapp.LoginScreen.LoginActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TeacherRegistrationActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etSubject;
    private TextInputLayout tilName, tilEmail, tilPassword, tilSubject;
    private ProgressBar progressBar;
    private Button btnRegister;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_registration);

        // Initialize views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etSubject = findViewById(R.id.etSubjectName);
        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilSubject = findViewById(R.id.tilSubject);
        progressBar = findViewById(R.id.progressBar);
        btnRegister = findViewById(R.id.btnTeacherRegister);
        tvError = findViewById(R.id.tvError);

        btnRegister.setOnClickListener(v -> registerTeacher());
    }

    private void registerTeacher() {
        String name = Objects.requireNonNull(etName.getText()).toString().trim();
        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(etPassword.getText()).toString().trim();
        String subject = Objects.requireNonNull(etSubject.getText()).toString().trim();

        // Reset errors
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilSubject.setError(null);
        tvError.setVisibility(View.GONE);

        // Validate inputs
        if (name.isEmpty()) {
            tilName.setError("Name is required");
            return;
        }

        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            return;
        }

        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (subject.isEmpty()) {
            tilSubject.setError("Subject is required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            saveTeacherData(user.getUid(), name, email, subject);
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        tvError.setVisibility(View.VISIBLE);

                        // Get the actual error message
                        String errorMessage = "Registration failed";
                        if (task.getException() != null) {
                            errorMessage += ": " + task.getException().getMessage();

                            // Handle specific error cases
                            if (task.getException() instanceof FirebaseAuthWeakPasswordException) {
                                errorMessage = "Password too weak - use at least 6 characters";
                            } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                errorMessage = "Invalid email format";
                            } else if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                errorMessage = "Email already in use";
                            }
                        }

                        tvError.setText(errorMessage);
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveTeacherData(String uid, String name, String email, String subject) {
        Map<String, Object> teacher = new HashMap<>();
        teacher.put("id", uid);
        teacher.put("name", name);
        teacher.put("email", email);
        teacher.put("subject", subject);
        teacher.put("approved", false); // Requires admin approval
        teacher.put("role", "teacher");

        // Save to both pending_approvals and users collection
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // First save to users collection
        db.collection("users").document(uid)
                .set(teacher)
                .addOnCompleteListener(userTask -> {
                    if (userTask.isSuccessful()) {
                        // Then save to pending_approvals for admin to review
                        db.collection("teacher_approvals").document(uid)
                                .set(teacher)
                                .addOnCompleteListener(approvalTask -> {
                                    progressBar.setVisibility(View.GONE);

                                    if (approvalTask.isSuccessful()) {
                                        Toast.makeText(this,
                                                "Registration successful! Waiting for admin approval",
                                                Toast.LENGTH_LONG).show();
                                        startActivity(new Intent(this, LoginActivity.class));
                                        finish();
                                    } else {
                                        btnRegister.setEnabled(true);
                                        tvError.setVisibility(View.VISIBLE);
                                        Toast.makeText(this, "Failed to save approval request", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        tvError.setVisibility(View.VISIBLE);

                        Toast.makeText(this, "Failed to save teacher data:", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}