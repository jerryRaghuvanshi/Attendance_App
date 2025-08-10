package com.example.attendanceapp.adaptors;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceapp.R;
import com.example.attendanceapp.models.DetectedStudent;

import java.util.List;

public class DetectedStudentsAdapter extends RecyclerView.Adapter<DetectedStudentsAdapter.ViewHolder> {
    private final List<DetectedStudent> students;

    public DetectedStudentsAdapter(List<DetectedStudent> students) {
        this.students = students;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_detected_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DetectedStudent student = students.get(position);
        holder.tvStudentName.setText(student.getStudentName());
        holder.tvMarkTime.setText("Marked at: " + student.getTime());
        holder.tvStudentId.setText("ID: " + student.getStudentId());
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvMarkTime, tvStudentId;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvMarkTime = itemView.findViewById(R.id.tvDetectionTime);
            tvStudentId = itemView.findViewById(R.id.tvStudentId);
        }
    }
}
