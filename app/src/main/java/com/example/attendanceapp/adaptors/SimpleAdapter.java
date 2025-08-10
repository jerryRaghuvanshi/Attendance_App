package com.example.attendanceapp.adaptors;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceapp.R;
import com.example.attendanceapp.models.Teacher;

import java.util.List;

public class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.ViewHolder> {
    private List<Teacher> teachers;
    private OnTeacherClickListener listener;

    public SimpleAdapter(List<Teacher> teachers, OnTeacherClickListener listener) {
        this.teachers = teachers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_teacher, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Teacher teacher = teachers.get(position);
        holder.name.setText(teacher.getName());
        holder.email.setText(teacher.getEmail());
        holder.itemView.setOnClickListener(v -> listener.onClick(teacher));
    }

    @Override
    public int getItemCount() {
        return teachers.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, email;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            email = itemView.findViewById(R.id.email);
        }
    }

   public interface OnTeacherClickListener {
        void onClick(Teacher teacher);
    }
}