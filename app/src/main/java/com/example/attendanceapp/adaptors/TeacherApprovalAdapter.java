package com.example.attendanceapp.adaptors;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceapp.R;
import com.example.attendanceapp.models.Teacher;

import java.util.List;

public class TeacherApprovalAdapter extends RecyclerView.Adapter<TeacherApprovalAdapter.TeacherViewHolder> {

    private final List<Teacher> teachers;
    private final ApprovalListener approvalListener;

    public interface ApprovalListener {
        void onApprovalAction(Teacher teacher, boolean approved);
    }

    public TeacherApprovalAdapter(List<Teacher> teachers, ApprovalListener approvalListener) {
        this.teachers = teachers;
        this.approvalListener = approvalListener;
    }

    @NonNull
    @Override
    public TeacherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_teacher_approval, parent, false);
        return new TeacherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeacherViewHolder holder, int position) {
        Teacher teacher = teachers.get(position);
        holder.bind(teacher);
    }

    @Override
    public int getItemCount() {
        return teachers.size();
    }
    // Inside your TeacherApprovalAdapter class
    public void removeTeacher(Teacher teacher) {
        // Find the position of the teacher in the list
        int position = teachers.indexOf(teacher);

        if (position != -1) {
            // Remove from the list
            teachers.remove(position);

            // Notify adapter about the removal
            notifyItemRemoved(position);

            // Optional: Notify any range changes if needed
            notifyItemRangeChanged(position, teachers.size());
        }
    }

    class TeacherViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName, tvEmail, tvSubject;
        private final Button btnApprove, btnReject;

        public TeacherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTeacherName);
            tvEmail = itemView.findViewById(R.id.tvTeacherEmail);
            tvSubject = itemView.findViewById(R.id.tvTeacherSubject);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }

        public void bind(Teacher teacher) {
            tvName.setText(teacher.getName());
            tvEmail.setText(teacher.getEmail());
            tvSubject.setText(teacher.getSubject());

            btnApprove.setOnClickListener(v -> approvalListener.onApprovalAction(teacher, true));
            btnReject.setOnClickListener(v -> approvalListener.onApprovalAction(teacher, false));
        }
    }
}