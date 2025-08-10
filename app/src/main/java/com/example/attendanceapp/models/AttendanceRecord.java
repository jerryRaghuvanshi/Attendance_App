package com.example.attendanceapp.models;


import java.sql.Time;
public class AttendanceRecord {
    private String date;
    private String subject;
    private String status;
    private long timestamp;

    // Default constructor
    public AttendanceRecord() {}

    // Constructor with parameters
    public AttendanceRecord(String date, String subject, String status) {
        this.date = date;
        this.subject = subject;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

