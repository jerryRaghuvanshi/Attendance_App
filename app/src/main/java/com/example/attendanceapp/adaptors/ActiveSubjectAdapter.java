package com.example.attendanceapp.adaptors;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.attendanceapp.Dashboards.StudentDashboard;
import com.example.attendanceapp.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActiveSubjectAdapter extends RecyclerView.Adapter<ActiveSubjectAdapter.SessionViewHolder> {

    private List<StudentDashboard.ActiveSession> sessions;
    private OnSessionClickListener listener;
    private Context context;

    public interface OnSessionClickListener {
        void onSessionClick(StudentDashboard.ActiveSession session);
        void onMarkPresentClick(StudentDashboard.ActiveSession session);
        void onMarkAttendanceClick(StudentDashboard.ActiveSession session);
    }

    public ActiveSubjectAdapter(List<StudentDashboard.ActiveSession> sessions, OnSessionClickListener listener) {
        this.sessions = sessions;
        this.listener = listener;
    }

    public ActiveSubjectAdapter(Context context, List<StudentDashboard.ActiveSession> sessions) {
        this.context = context;
        this.sessions = sessions;
    }

    public void setOnSessionClickListener(OnSessionClickListener listener) {
        this.listener = listener;
    }

    public void setSessions(List<StudentDashboard.ActiveSession> sessions) {
        this.sessions = sessions;
        notifyDataSetChanged();
    }
    public List<StudentDashboard.ActiveSession> getSessions() {
        return sessions;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
        }
        View view = LayoutInflater.from(context).inflate(R.layout.item_active_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        StudentDashboard.ActiveSession session = sessions.get(position);

        // Set subject name
        holder.tvSubjectName.setText(session.getSubject() != null ? session.getSubject() : "Unknown Subject");

        // Set teacher name
        holder.tvTeacherName.setText(session.getTeacherName() != null ? session.getTeacherName() : "Unknown Teacher");

        // Format and set start time
        if (session.getStartTime() > 0) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String startTime = timeFormat.format(new Date(session.getStartTime()));
            holder.tvStartTime.setText("Started at " + startTime);
        } else {
            holder.tvStartTime.setText("Start time unknown");
        }

        // Set attendance count
        int attendanceCount = session.getAttendanceCount();
        holder.tvAttendanceCount.setText(attendanceCount + " students present");

        // Update session status indicator
        updateSessionStatusIndicator(holder, session);

        // Update Bluetooth status
        updateBluetoothStatus(holder, session);

        // Update action button
        updateActionButton(holder, session);

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSessionClick(session);
            }
        });
    }

    private void updateSessionStatusIndicator(SessionViewHolder holder, StudentDashboard.ActiveSession session) {
        if (session.isActive()) {
            // Active session - green indicator
            holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.green_600));
            holder.tvLiveIndicator.setVisibility(View.VISIBLE);
            holder.tvLiveIndicator.setText("LIVE");
            holder.tvLiveIndicator.setTextColor(ContextCompat.getColor(context, R.color.red_600));

            // Session status icon
            holder.ivSessionStatus.setImageResource(R.drawable.ic_session_active);
            holder.ivSessionStatus.setColorFilter(ContextCompat.getColor(context, R.color.green_600));
        } else {
            // Inactive session - gray indicator
            holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.gray_400));
            holder.tvLiveIndicator.setVisibility(View.GONE);

            // Session status icon
            holder.ivSessionStatus.setImageResource(R.drawable.ic_session_inactive);
            holder.ivSessionStatus.setColorFilter(ContextCompat.getColor(context, R.color.gray_400));
        }
    }

    private void updateBluetoothStatus(SessionViewHolder holder, StudentDashboard.ActiveSession session) {
        if (session.isInBluetoothRange()) {
            // In Bluetooth range
            holder.tvBluetoothStatus.setText("In range - Ready to mark attendance");
            holder.tvBluetoothStatus.setTextColor(ContextCompat.getColor(context, R.color.green_700));

            holder.ivBluetoothStatus.setVisibility(View.VISIBLE);
            holder.ivBluetoothStatus.setImageResource(R.drawable.ic_bluetooth);
            holder.ivBluetoothStatus.setColorFilter(ContextCompat.getColor(context, R.color.green_600));
        } else {
            // Not in range
            holder.tvBluetoothStatus.setText("Not in range - Move closer to teacher");
            holder.tvBluetoothStatus.setTextColor(ContextCompat.getColor(context, R.color.orange_600));

            holder.ivBluetoothStatus.setVisibility(View.VISIBLE);
            holder.ivBluetoothStatus.setImageResource(R.drawable.ic_bluetooth);
            holder.ivBluetoothStatus.setColorFilter(ContextCompat.getColor(context, R.color.gray_400));
        }
    }

    private void updateActionButton(SessionViewHolder holder, StudentDashboard.ActiveSession session) {
        if (!session.isActive()) {
            // Inactive session
            holder.btnJoinSession.setText("Session Ended");
            holder.btnJoinSession.setEnabled(false);
            holder.btnJoinSession.setBackgroundTintList(
                    ContextCompat.getColorStateList(context, R.color.gray_400));
            holder.btnJoinSession.setOnClickListener(null);
        } else if (session.isInBluetoothRange()) {
            // Active and in range - can mark attendance
            holder.btnJoinSession.setText("Mark Present");
            holder.btnJoinSession.setEnabled(true);
            holder.btnJoinSession.setBackgroundTintList(
                    ContextCompat.getColorStateList(context, R.color.green_700));
            holder.btnJoinSession.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMarkAttendanceClick(session);
                }
            });
        } else {
            // Active but not in range
            holder.btnJoinSession.setText("Move Closer");
            holder.btnJoinSession.setEnabled(false);
            holder.btnJoinSession.setBackgroundTintList(
                    ContextCompat.getColorStateList(context, R.color.orange_600));
            holder.btnJoinSession.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return sessions != null ? sessions.size() : 0;
    }

    public static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubjectName;
        TextView tvTeacherName;
        TextView tvStartTime;
        TextView tvAttendanceCount;
        TextView tvBluetoothStatus;
        TextView tvLiveIndicator;
        View statusIndicator;
        ImageView ivSessionStatus;
        ImageView ivBluetoothStatus;
        Button btnJoinSession;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);

            tvSubjectName = itemView.findViewById(R.id.tvSubjectName);
            tvTeacherName = itemView.findViewById(R.id.tvTeacherName);
            tvStartTime = itemView.findViewById(R.id.tvStartTime);
            tvAttendanceCount = itemView.findViewById(R.id.tvAttendanceCount);
            tvBluetoothStatus = itemView.findViewById(R.id.tvBluetoothStatus);
            tvLiveIndicator = itemView.findViewById(R.id.tvLiveIndicator);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            ivSessionStatus = itemView.findViewById(R.id.ivSessionStatus);
            ivBluetoothStatus = itemView.findViewById(R.id.ivBluetoothStatus);
            btnJoinSession = itemView.findViewById(R.id.btnJoinSession);
        }
    }
}