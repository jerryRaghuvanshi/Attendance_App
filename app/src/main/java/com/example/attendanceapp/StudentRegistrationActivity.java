package com.example.attendanceapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.attendanceapp.LoginScreen.LoginActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StudentRegistrationActivity extends AppCompatActivity {

    private static final String TAG = "StudentRegistration";
    private TextInputEditText etRollNo, etName, etEmail, etPassword;
    private TextInputLayout tilRollNo, tilBranch;
    private ProgressBar progressBar;
    private TextView tvError;
    private Spinner actvBranch;
    private FirebaseFirestore db;
    private Spinner spinnerYear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_registration);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etRollNo = findViewById(R.id.etRollNo);
        tilRollNo = findViewById(R.id.tilRollNo);
        tilBranch = findViewById(R.id.tilBranch);
        actvBranch = findViewById(R.id.actvBranch);
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);

        // Setup branch dropdown
        ArrayAdapter<String> branchAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.branches)
        );
        actvBranch.setAdapter(branchAdapter);

        findViewById(R.id.btnRegister).setOnClickListener(v -> registerStudent());
        spinnerYear = findViewById(R.id.spinnerYear);
        ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.years_array,
                android.R.layout.simple_spinner_item
        );
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);
    }

    private void registerStudent() {
        String name = Objects.requireNonNull(etName.getText()).toString().trim();
        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(etPassword.getText()).toString().trim();
        String rollNo = Objects.requireNonNull(etRollNo.getText()).toString().trim().toLowerCase();
        String branch = actvBranch.getSelectedItem().toString().trim();
        String year = spinnerYear.getSelectedItem().toString().trim();

        Log.d(TAG, "Starting registration process for: " + email);

        // Clear previous errors
        tilRollNo.setError(null);
        etName.setError(null);
        etEmail.setError(null);
        etPassword.setError(null);
        tilBranch.setError(null);
        tvError.setVisibility(View.GONE);

        // Validate inputs
        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        if (rollNo.isEmpty()) {
            etRollNo.setError("Roll number is required");
            etRollNo.requestFocus();
            return;
        }

        if (branch.isEmpty() || branch.equals("Select Branch")) {
            tilBranch.setError("Please select branch");
            return;
        }

        if (year.isEmpty() || year.equals("Select Year")) {
            Toast.makeText(this, "Please select a year", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.btnRegister).setEnabled(false);

        // FIRST: Check roll number uniqueness
        checkRollNumberUniqueness(rollNo, name, email, password, branch, year);
    }

    private void checkRollNumberUniqueness(String rollNo, String name, String email,
                                           String password, String branch, String year) {
        Log.d(TAG, "Checking roll number uniqueness for: " + rollNo);

        db.collection("users")
                .whereEqualTo("rollNo", rollNo)
                .whereEqualTo("role", "student")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Roll number is unique - proceed with registration
                        createFirebaseUser(name, email, password, rollNo, branch, year);
                    } else {
                        // Roll number exists
                        Log.d(TAG, "Roll number already exists");
                        handleRollNumberError("Roll number already exists");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking roll number uniqueness", e);
                    handleRollNumberError("Error checking roll number: " + e.getMessage());
                });
    }

    private void createFirebaseUser(String name, String email, String password,
                                    String rollNo, String branch, String year) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        // Send verification email
                        user.sendEmailVerification()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "Verification email sent to " + email);
                                    }
                                });

                        // Save student data to Firestore
                        saveStudentData(user.getUid(), name, email, rollNo, branch, year);
                    } else {
                        showRegistrationError("User creation failed - please try again");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase user creation failed", e);
                    showRegistrationError("Registration failed: " + e.getMessage());
                });
    }

    private void saveStudentData(String uid, String name, String email,
                                 String rollNo, String branch, String year) {
        Map<String, Object> student = new HashMap<>();
        student.put("id", uid);
        student.put("name", name);
        student.put("email", email);
        student.put("rollNo", rollNo);
        student.put("branch", branch);
        student.put("year", year);
        student.put("role", "student");
        student.put("approved", true);
        student.put("createdAt", System.currentTimeMillis());

        db.collection("users")
                .document(uid)
                .set(student)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Student data saved successfully");
                    progressBar.setVisibility(View.GONE);

                    Toast.makeText(this, "Registration successful! Please verify your email",
                            Toast.LENGTH_LONG).show();

                    // Navigate to login
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save student data", e);

                    // Delete auth user if Firestore save fails
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        currentUser.delete();
                    }

                    showRegistrationError("Failed to complete registration: " + e.getMessage());
                });
    }

    private void handleRollNumberError(String message) {
        progressBar.setVisibility(View.GONE);
        findViewById(R.id.btnRegister).setEnabled(true);
        tilRollNo.setError(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showRegistrationError(String message) {
        progressBar.setVisibility(View.GONE);
        findViewById(R.id.btnRegister).setEnabled(true);
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}