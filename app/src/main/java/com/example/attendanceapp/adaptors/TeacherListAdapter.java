package com.example.attendanceapp.adaptors;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceapp.models.Teacher;
import com.example.attendanceapp.R;

import java.util.ArrayList;
import java.util.List;

public class TeacherListAdapter extends RecyclerView.Adapter<TeacherListAdapter.TeacherViewHolder> {

    private List<Teacher> teachers;

    public TeacherListAdapter() {
        // Initialize with empty list
        this.teachers = new ArrayList<>();
    }

    public void setTeachers(List<Teacher> teachers) {
        this.teachers = teachers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TeacherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_teacher, parent, false);
        return new TeacherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeacherViewHolder holder, int position) {
        Teacher teacher = teachers.get(position);
        holder.tvName.setText(teacher.getName());
        holder.tvEmail.setText(teacher.getEmail());
        holder.tvSubjects.setText(String.join(", ", teacher.getSubjects()));
    }

    @Override
    public int getItemCount() {
        return teachers.size();
    }

    static class TeacherViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvSubjects;

        public TeacherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTeacherName);
            tvEmail = itemView.findViewById(R.id.tvTeacherEmail);
            tvSubjects = itemView.findViewById(R.id.tvTeacherSubjects);
        }
    }
}