package com.example.attendanceapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageView ivProfile;
    private TextView tvName, tvEmail, tvRole;
    private Button btnUpdate;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        ivProfile = findViewById(R.id.ivProfile);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvRole = findViewById(R.id.tvRole);
        btnUpdate = findViewById(R.id.btnUpdate);

        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Profile");
        }

        // Load user data
        loadUserProfile();

        // Set click listener for update button
        btnUpdate.setOnClickListener(v -> {
            // Handle profile update
            updateProfile();
        });
    }

    private void loadUserProfile() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Set user data to views
                        tvName.setText(documentSnapshot.getString("name"));
                        tvEmail.setText(documentSnapshot.getString("email"));
                        tvRole.setText(documentSnapshot.getString("role"));
                        if (documentSnapshot.getString("photoUrl") != null) {
                            Glide.with(this)
                                    .load(documentSnapshot.getString("photoUrl"))
                                    .circleCrop()
                                    .into(ivProfile);
                        }
                        // You can load profile picture using Glide/Picasso here
                        // Example: Glide.with(this).load(documentSnapshot.getString("photoUrl")).into(ivProfile);
                    }
                })
                .addOnFailureListener(e -> {
                    tvName.setText("Error loading profile");
                });
    }

    private void updateProfile() {
        // Implement your profile update logic here
        // This could open a new activity or dialog for editing profile
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}