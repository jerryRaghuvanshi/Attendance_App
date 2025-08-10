package com.example.attendanceapp.adaptors;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceapp.models.SchoolClass;
import com.example.attendanceapp.R;

import java.util.ArrayList;
import java.util.List;

public class ClassListAdapter extends RecyclerView.Adapter<ClassListAdapter.ClassViewHolder> {

    private List<SchoolClass> classes;

    public ClassListAdapter() {
        this.classes = new ArrayList<>();
    }

    public void setClasses(List<SchoolClass> classes) {
        this.classes = classes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_class, parent, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        SchoolClass schoolClass = classes.get(position);
        holder.tvClassName.setText(schoolClass.getClassName());
        holder.tvClassTeacher.setText(schoolClass.getClassTeacher());
        holder.tvStudentCount.setText(String.format("%d students", schoolClass.getStudentCount()));
    }

    @Override
    public int getItemCount() {
        return classes.size();
    }

    static class ClassViewHolder extends RecyclerView.ViewHolder {
        TextView tvClassName, tvClassTeacher, tvStudentCount;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClassName = itemView.findViewById(R.id.tvClassName);
            tvClassTeacher = itemView.findViewById(R.id.tvClassTeacher);
            tvStudentCount = itemView.findViewById(R.id.tvStudentCount);
        }
    }
}