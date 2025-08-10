package com.example.attendanceapp.models;


import java.util.List;

public class Teacher {
    private String id;
    private String name;
    private String email;
    private String Subject;
    private List<String> subjects;
    private boolean active;

    // Empty constructor for Firestore
    public Teacher() {}

    public Teacher(String id, String name, String email, List<String> subjects, boolean active,String Subject) {
        this.id = id;
        this.Subject=Subject;
        this.name = name;
        this.email = email;
        this.subjects = subjects;
        this.active = active;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public String getSubject() { return Subject; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public void setSubject(String Subject) { this.Subject = Subject; }
    public List<String> getSubjects() { return subjects; }
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
