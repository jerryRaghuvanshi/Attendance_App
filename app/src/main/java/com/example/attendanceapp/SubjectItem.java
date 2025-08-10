package com.example.attendanceapp;

public class SubjectItem {
    private String name;
    private boolean hasActiveSession;
    private String activeSessionId;

    public SubjectItem(String name, boolean hasActiveSession, String activeSessionId) {
        this.name = name;
        this.hasActiveSession = hasActiveSession;
        this.activeSessionId = activeSessionId;
    }

    public String getName() { return name; }
    public boolean hasActiveSession() { return hasActiveSession; }
    public String getActiveSessionId() { return activeSessionId; }
}
