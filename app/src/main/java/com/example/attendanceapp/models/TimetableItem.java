package com.example.attendanceapp.models;

public class TimetableItem {
    private String day;
    private String startTime;
    private String endTime;
    private String subject;
    private String room;
    private String teacher;

    // Empty constructor required for Firestore
    public TimetableItem() {}

    public TimetableItem(String day, String startTime, String endTime,
                         String subject, String room, String teacher) {
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.subject = subject;
        this.room = room;
        this.teacher = teacher;
    }

    // Getters and setters
    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }
}