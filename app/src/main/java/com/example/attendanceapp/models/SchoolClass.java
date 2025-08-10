package com.example.attendanceapp.models;

public class SchoolClass {
    private String id;
    private String className;
    private String classTeacher;
    private int studentCount;

    // Empty constructor for Firestore
    public SchoolClass() {}

    public SchoolClass(String id, String className, String classTeacher, int studentCount) {
        this.id = id;
        this.className = className;
        this.classTeacher = classTeacher;
        this.studentCount = studentCount;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getClassTeacher() { return classTeacher; }
    public void setClassTeacher(String classTeacher) { this.classTeacher = classTeacher; }
    public int getStudentCount() { return studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
}
