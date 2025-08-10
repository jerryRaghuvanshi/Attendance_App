package com.example.attendanceapp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceapp.adaptors.TimetableAdapter;
import com.example.attendanceapp.models.TimetableItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TimetableActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TimetableAdapter adapter;
    private FirebaseFirestore db;
    private TextView tvEmptyView;
    private String userRole, userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Timetable");
        }

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.rvTimetable);
        tvEmptyView = findViewById(R.id.tvEmpty);

        // Set layout manager
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TimetableAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Get current user info
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getUserRole();
    }

    private void getUserRole() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userRole = documentSnapshot.getString("role");
                        loadTimetable();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading user data", Toast.LENGTH_LONG).show();
                    tvEmptyView.setVisibility(View.VISIBLE);
                });
    }

    private void loadTimetable() {
        String collectionPath = userRole.equals("teacher") ? "teachers" : "students";

        db.collection(collectionPath)
                .document(userId)
                .collection("timetable")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        List<TimetableItem> timetableItems = new ArrayList<>();

                        // Convert documents to TimetableItem objects
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            TimetableItem item = doc.toObject(TimetableItem.class);
                            timetableItems.add(item);
                        }

                        // Sort by day and time
                        sortTimetable(timetableItems);
                        adapter.updateData(timetableItems);

                        if (timetableItems.isEmpty()) {
                            showEmptyView("No timetable data available");
                        } else {
                            hideEmptyView();
                        }
                    } else {
                        showEmptyView("No timetable data available");
                    }
                })
                .addOnFailureListener(e -> {
                    showEmptyView("Error loading timetable");
                });
    }

    private void sortTimetable(List<TimetableItem> items) {
        // Define day order (Monday to Friday)
        String[] dayOrder = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

        items.sort((item1, item2) -> {
            int dayIndex1 = getDayIndex(item1.getDay(), dayOrder);
            int dayIndex2 = getDayIndex(item2.getDay(), dayOrder);

            if (dayIndex1 != dayIndex2) {
                return Integer.compare(dayIndex1, dayIndex2);
            }

            // If same day, sort by start time
            return item1.getStartTime().compareTo(item2.getStartTime());
        });
    }

    private int getDayIndex(String day, String[] dayOrder) {
        for (int i = 0; i < dayOrder.length; i++) {
            if (dayOrder[i].equalsIgnoreCase(day)) {
                return i;
            }
        }
        return dayOrder.length; // Place unknown days at the end
    }

    private void showEmptyView(String message) {
        tvEmptyView.setText(message);
        tvEmptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyView() {
        tvEmptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}