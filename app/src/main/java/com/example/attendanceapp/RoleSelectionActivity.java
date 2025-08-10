package com.example.attendanceapp;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        RadioGroup radioRole = findViewById(R.id.radioRole);
        Button btnProceed = findViewById(R.id.btnProceed);

        btnProceed.setOnClickListener(v -> {
            int selectedId = radioRole.getCheckedRadioButtonId();

            if (selectedId == R.id.radioDirector) {
                startActivity(new Intent(this, DirectorSetupActivity.class));
            } else if (selectedId == R.id.radioTeacher) {
                startActivity(new Intent(this, TeacherRegistrationActivity.class));
            } else if (selectedId == R.id.radioStudent) {
                startActivity(new Intent(this, StudentRegistrationActivity.class));
            } else {
                Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            }
        });
    }
}