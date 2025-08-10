package com.example.attendanceapp.models;

public class DetectedStudent {
    private String studentId;
    private String studentName;
    private long markedAt;
    private String time;
    private String branch;
    private String year;

    public DetectedStudent() {}

    // Getters and setters
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public long getMarkedAt() { return markedAt; }
    public void setMarkedAt(long markedAt) { this.markedAt = markedAt; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
}