package com.example.attendanceapp.adaptors;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.attendanceapp.R;
import com.example.attendanceapp.models.Student;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class StudentListAdapter extends RecyclerView.Adapter<StudentListAdapter.StudentViewHolder> {

    private final List<Student> students;
    private final Set<String> addedStudentIds;
    private OnStudentClickListener listener;

    public interface OnStudentClickListener {
        void onStudentClick(Student student, int position);
        void onStudentLongClick(Student student, int position);
    }

    public StudentListAdapter(List<Student> students) {
        this.students = new ArrayList<>(students);
        this.addedStudentIds = new HashSet<>();

        // Add existing student IDs to the set
        for (Student student : students) {
            addedStudentIds.add(student.getId());
        }
    }

    public void setOnStudentClickListener(OnStudentClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = students.get(position);
        holder.bind(student);
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    public void addStudentIfNew(Student student) {
        if (!addedStudentIds.contains(student.getId())) {
            students.add(student);
            addedStudentIds.add(student.getId());
            notifyItemInserted(students.size() - 1);
        }
    }

    public void addStudents(List<Student> newStudents) {
        int startPosition = students.size();
        int addedCount = 0;

        for (Student student : newStudents) {
            if (!addedStudentIds.contains(student.getId())) {
                students.add(student);
                addedStudentIds.add(student.getId());
                addedCount++;
            }
        }

        if (addedCount > 0) {
            notifyItemRangeInserted(startPosition, addedCount);
        }
    }

    /**
     * Updates the adapter with real attendance data from Firebase.
     * This method replaces any existing data with the provided attendance list.
     *
     * @param attendedStudents List of students with real attendance status from Firebase
     */
    public void updateWithRealAttendance(List<Student> attendedStudents) {
        // Clear existing data
        int oldSize = students.size();
        students.clear();
        addedStudentIds.clear();

        if (oldSize > 0) {
            notifyItemRangeRemoved(0, oldSize);
        }

        // Add new attendance data
        if (attendedStudents != null && !attendedStudents.isEmpty()) {
            students.addAll(attendedStudents);

            // Update the set of added student IDs
            for (Student student : attendedStudents) {
                addedStudentIds.add(student.getId());
            }

            notifyItemRangeInserted(0, attendedStudents.size());
        }
    }

    /**
     * Updates attendance data while preserving existing entries.
     * This method adds new students or updates existing ones without clearing the list.
     *
     * @param attendedStudents List of students with updated attendance status
     */
    public void updateAttendanceData(List<Student> attendedStudents) {
        if (attendedStudents == null || attendedStudents.isEmpty()) {
            return;
        }

        for (Student newStudent : attendedStudents) {
            boolean found = false;

            // Check if student already exists and update
            for (int i = 0; i < students.size(); i++) {
                if (students.get(i).getId().equals(newStudent.getId())) {
                    students.set(i, newStudent);
                    notifyItemChanged(i);
                    found = true;
                    break;
                }
            }

            // If student doesn't exist, add them
            if (!found) {
                students.add(newStudent);
                addedStudentIds.add(newStudent.getId());
                notifyItemInserted(students.size() - 1);
            }
        }
    }

    /**
     * Syncs the adapter with real-time attendance data.
     * This method efficiently updates the list by comparing with existing data.
     *
     * @param liveAttendanceData List of students with current attendance status
     */
    public void syncWithLiveData(List<Student> liveAttendanceData) {
        if (liveAttendanceData == null) {
            liveAttendanceData = new ArrayList<>();
        }

        // Create sets for efficient comparison
        Set<String> liveStudentIds = new HashSet<>();
        for (Student student : liveAttendanceData) {
            liveStudentIds.add(student.getId());
        }

        // Remove students who are no longer present
        for (int i = students.size() - 1; i >= 0; i--) {
            if (!liveStudentIds.contains(students.get(i).getId())) {
                String removedId = students.get(i).getId();
                students.remove(i);
                addedStudentIds.remove(removedId);
                notifyItemRemoved(i);
            }
        }

        // Add or update students from live data
        for (Student liveStudent : liveAttendanceData) {
            boolean found = false;

            for (int i = 0; i < students.size(); i++) {
                if (students.get(i).getId().equals(liveStudent.getId())) {
                    // Update existing student if data has changed
                    if (!students.get(i).equals(liveStudent)) {
                        students.set(i, liveStudent);
                        notifyItemChanged(i);
                    }
                    found = true;
                    break;
                }
            }

            // Add new student
            if (!found) {
                students.add(liveStudent);
                addedStudentIds.add(liveStudent.getId());
                notifyItemInserted(students.size() - 1);
            }
        }
    }

    public void updateStudent(int position, Student updatedStudent) {
        if (position >= 0 && position < students.size()) {
            students.set(position, updatedStudent);
            notifyItemChanged(position);
        }
    }

    public void removeStudent(int position) {
        if (position >= 0 && position < students.size()) {
            Student removedStudent = students.remove(position);
            addedStudentIds.remove(removedStudent.getId());
            notifyItemRemoved(position);
        }
    }

    public void clearData() {
        int size = students.size();
        students.clear();
        addedStudentIds.clear();
        notifyItemRangeRemoved(0, size);
    }

    public Student getStudentAtPosition(int position) {
        if (position >= 0 && position < students.size()) {
            return students.get(position);
        }
        return null;
    }

    public List<Student> getAllStudents() {
        return new ArrayList<>(students);
    }

    public List<Student> getPresentStudents() {
        List<Student> presentStudents = new ArrayList<>();
        for (Student student : students) {
            if ("Present".equalsIgnoreCase(student.getStatus())) {
                presentStudents.add(student);
            }
        }
        return presentStudents;
    }

    public int getPresentCount() {
        return getPresentStudents().size();
    }

    public boolean isStudentPresent(String studentId) {
        return addedStudentIds.contains(studentId);
    }

    /**
     * Gets the total count of students currently in the adapter
     * @return Total number of students
     */
    public int getTotalStudentCount() {
        return students.size();
    }

    /**
     * Checks if the adapter has any students
     * @return true if adapter is empty, false otherwise
     */
    public boolean isEmpty() {
        return students.isEmpty();
    }

    class StudentViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivStudentAvatar;
        private final TextView tvStudentName;
        private final TextView tvStudentId;
        private final TextView tvDetectionTime;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);

            ivStudentAvatar = itemView.findViewById(R.id.ivStudentAvatar);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStudentId = itemView.findViewById(R.id.tvStudentId);
            tvDetectionTime = itemView.findViewById(R.id.tvDetectionTime);

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onStudentClick(students.get(getAdapterPosition()), getAdapterPosition());
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onStudentLongClick(students.get(getAdapterPosition()), getAdapterPosition());
                    return true;
                }
                return false;
            });
        }

        public void bind(Student student) {
            // Set student name
            tvStudentName.setText(student.getName());

            // Set student ID
            tvStudentId.setText(student.getId());

            // Set detection time (current time for now)
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            tvDetectionTime.setText("Detected: " + timeFormat.format(new Date()));

            // Set avatar based on student name
            setStudentAvatar(student.getName());

            // Update UI based on attendance status
            updateStatusUI(student.getStatus());
        }

        private void setStudentAvatar(String studentName) {
            // Set avatar background color based on first letter of name
            if (studentName != null && !studentName.isEmpty()) {
                char firstLetter = studentName.toUpperCase().charAt(0);
                int colorIndex = (firstLetter - 'A') % 6;

                int[] avatarColors = {
                        R.color.avatar_color_1,
                        R.color.avatar_color_2,
                        R.color.avatar_color_3,
                        R.color.avatar_color_4,
                        R.color.avatar_color_5,
                        R.color.avatar_color_6
                };

                ivStudentAvatar.setBackgroundTintList(
                        ContextCompat.getColorStateList(itemView.getContext(), avatarColors[colorIndex])
                );

                // You can also set a text avatar or use an image
                // For now, using a default person icon
                ivStudentAvatar.setImageResource(R.drawable.ic_student);
            }
        }

        private void updateStatusUI(String status) {
            // Update UI elements based on attendance status
            if ("Present".equalsIgnoreCase(status)) {
                // Green tint for present students
                itemView.setBackgroundTintList(
                        ContextCompat.getColorStateList(itemView.getContext(), R.color.green_50)
                );
            } else if ("Absent".equalsIgnoreCase(status)) {
                // Red tint for absent students
                itemView.setBackgroundTintList(
                        ContextCompat.getColorStateList(itemView.getContext(), R.color.red_50)
                );
            } else {
                // Default tint for other statuses
                itemView.setBackgroundTintList(
                        ContextCompat.getColorStateList(itemView.getContext(), R.color.gray_50)
                );
            }
        }
    }
}