package com.example.attendanceapp.models;

public class StudentDevice {
    private String macAddress;
    private String studentId;
    private String deviceName;
    private boolean enabled;

    // Constructors, getters, setters

    public StudentDevice(String macAddress, String studentId, String deviceName, boolean enabled) {
        this.macAddress = macAddress;
        this.studentId = studentId;
        this.deviceName = deviceName;
        this.enabled = enabled;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}