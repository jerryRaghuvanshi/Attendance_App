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

import com.example.attendanceapp.adaptors.ClassListAdapter;
import com.example.attendanceapp.R;
import com.example.attendanceapp.models.SchoolClass;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminClassesFragment extends Fragment {

    private RecyclerView recyclerView;
    private ClassListAdapter adapter;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_classes, container, false);

        recyclerView = view.findViewById(R.id.rvClasses);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ClassListAdapter();
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadClasses();

        return view;
    }

    private void loadClasses() {
        db.collection("classes")
                .orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Convert documents to Class objects and update adapter
                    adapter.setClasses(queryDocumentSnapshots.toObjects(SchoolClass.class));
                });
    }
}