package com.example.attendanceapp.Fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.attendanceapp.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AdminReportsFragment extends Fragment {

    private BarChart attendanceChart;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_reports, container, false);

        attendanceChart = view.findViewById(R.id.attendanceChart);
        db = FirebaseFirestore.getInstance();
        loadAttendanceData();

        return view;
    }

    private void loadAttendanceData() {
        db.collection("attendance")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(7)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<BarEntry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>();

                    int index = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        double present = doc.getDouble("present");
                        double total = doc.getDouble("total");
                        float percent = (float) ((present / total) * 100);

                        entries.add(new BarEntry(index++, percent));
                        labels.add(doc.getString("date"));
                    }

                    BarDataSet dataSet = new BarDataSet(entries, "Attendance %");
                    BarData barData = new BarData(dataSet);
                    attendanceChart.setData(barData);
                    attendanceChart.invalidate(); // refresh
                });
    }
}