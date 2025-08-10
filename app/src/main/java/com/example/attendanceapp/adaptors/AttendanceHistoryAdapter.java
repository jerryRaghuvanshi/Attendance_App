package com.example.attendanceapp.adaptors;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceapp.R;
import com.example.attendanceapp.models.AttendanceRecord;

import java.util.List;

public class AttendanceHistoryAdapter extends RecyclerView.Adapter<AttendanceHistoryAdapter.ViewHolder> {

    private List<AttendanceRecord> attendanceRecords;

    public AttendanceHistoryAdapter(List<AttendanceRecord> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
    }

    public void updateData(List<AttendanceRecord> newRecords) {
        this.attendanceRecords = newRecords;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceRecord record = attendanceRecords.get(position);
        holder.tvSubject.setText(record.getSubject());
        holder.tvDate.setText(record.getDate());
        holder.tvStatus.setText(record.getStatus());

        if ("Present".equals(record.getStatus())) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.green_700));
        } else {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.red_700));
        }
    }

    @Override
    public int getItemCount() {
        return attendanceRecords.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvSubject, tvDate, tvStatus;

        public ViewHolder(View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}