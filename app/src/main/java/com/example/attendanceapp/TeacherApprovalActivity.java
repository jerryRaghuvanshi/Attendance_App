package com.example.attendanceapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceapp.adaptors.AttendanceHistoryAdapter;
import com.example.attendanceapp.adaptors.TeacherApprovalAdapter;
import com.example.attendanceapp.models.Teacher;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TeacherApprovalActivity extends AppCompatActivity {
    private RecyclerView rvPendingTeachers;
    private TeacherApprovalAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_approval);

        rvPendingTeachers = findViewById(R.id.rvPendingTeachers);
        rvPendingTeachers.setLayoutManager(new LinearLayoutManager(this));

        loadPendingTeachers();
    }

    private void loadPendingTeachers() {
        FirebaseFirestore.getInstance().collection("teacher_approvals")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Teacher> teachers = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult()) {
                            Teacher teacher = doc.toObject(Teacher.class);
                            if (teacher != null) {
                                teacher.setId(doc.getId());
                                teachers.add(teacher);
                            }
                        }
                        updateUI(teachers);
                    } else {
                        handleError(task.getException());
                    }
                });
    }

    private void updateUI(List<Teacher> teachers) {
        if (teachers.isEmpty()) {
            showEmptyState();
        } else {
            setupRecyclerView(teachers);
        }
    }

    private void handleError(Exception exception) {
        Log.e("FirestoreError", "Error loading teachers", exception);

        if (exception instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) exception;
            if (firestoreException.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                // Specific handling for permission denied
                Toast.makeText(this,
                        "Admin access required. Please login as admin.",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        Toast.makeText(this,
                "Failed to load teachers: " + exception.getMessage(),
                Toast.LENGTH_SHORT).show();
    }

    private void setupRecyclerView(List<Teacher> teachers) {
        adapter = new TeacherApprovalAdapter(teachers, this::handleApproval);
        rvPendingTeachers.setLayoutManager(new LinearLayoutManager(this));
        rvPendingTeachers.setAdapter(adapter);
    }



    private void showEmptyState() {
        // Hide RecyclerView and show a "no pending teachers" message
        rvPendingTeachers.setVisibility(View.GONE);
        findViewById(R.id.tvEmptyState).setVisibility(View.VISIBLE);
    }

    private void handleApproval(Teacher teacher, boolean approved) {
        DocumentReference userRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(teacher.getId());

        if (approved) {
            // Batch write for atomic update
            WriteBatch batch = FirebaseFirestore.getInstance().batch();

            // Update in users collection
            batch.update(userRef, "approved", true);

            // Remove from pending
            batch.delete(FirebaseFirestore.getInstance()
                    .collection("teacher_approvals")
                    .document(teacher.getId()));

            batch.commit().addOnCompleteListener(batchTask -> {
                if (batchTask.isSuccessful()) {
                    adapter.removeTeacher(teacher);
                    Toast.makeText(this, "Teacher approved", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Approval failed", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Reject and delete
            FirebaseFirestore.getInstance().collection("teacher_approvals")
                    .document(teacher.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        adapter.removeTeacher(teacher);
                        Toast.makeText(this, "Teacher rejected", Toast.LENGTH_SHORT).show();

                        // Optional: Delete auth account
                        FirebaseAuth.getInstance().getCurrentUser().delete();
                    });
        }
    }
}