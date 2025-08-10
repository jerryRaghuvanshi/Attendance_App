package com.example.attendanceapp.Fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceapp.adaptors.TeacherListAdapter;
import com.example.attendanceapp.R;
import com.example.attendanceapp.models.Teacher;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminTeachersFragment extends Fragment {

    private RecyclerView recyclerView;
    private TeacherListAdapter adapter;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_teachers, container, false);

        recyclerView = view.findViewById(R.id.rvTeachers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TeacherListAdapter();
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadTeachers();

        return view;
    }

    private void loadTeachers() {
        db.collection("teachers")
                .orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Convert documents to Teacher objects and update adapter
                    adapter.setTeachers(queryDocumentSnapshots.toObjects(Teacher.class));
                });
    }
}