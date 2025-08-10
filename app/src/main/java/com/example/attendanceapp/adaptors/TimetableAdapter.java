package com.example.attendanceapp.adaptors;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceapp.models.TimetableItem;
import com.example.attendanceapp.R;

import java.util.List;

public class TimetableAdapter extends RecyclerView.Adapter<TimetableAdapter.TimetableViewHolder> {

    private List<TimetableItem> timetableItems;

    public TimetableAdapter(List<TimetableItem> timetableItems) {
        this.timetableItems = timetableItems;
    }

    public void updateData(List<TimetableItem> newItems) {
        this.timetableItems = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimetableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timetable, parent, false);
        return new TimetableViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimetableViewHolder holder, int position) {
        TimetableItem item = timetableItems.get(position);

        holder.tvDay.setText(item.getDay());
        holder.tvTime.setText(String.format("%s - %s", item.getStartTime(), item.getEndTime()));
        holder.tvSubject.setText(item.getSubject());
        holder.tvRoom.setText(item.getRoom());
        holder.tvTeacher.setText(item.getTeacher());
    }

    @Override
    public int getItemCount() {
        return timetableItems.size();
    }

    static class TimetableViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvTime, tvSubject, tvRoom, tvTeacher;

        public TimetableViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            tvTeacher = itemView.findViewById(R.id.tvTeacher);
        }
    }
}