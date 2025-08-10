package com.example.attendanceapp.models;

public class WhitelistedDevice {
    private String deviceId;
    private String deviceName;
    private String macAddress;
    private String deviceType; // "phone" or "tablet"
    private String studentId;
    private String studentName;
    private boolean enabled;
    private long registeredAt;

    // Constructors
    public WhitelistedDevice() {}

    public WhitelistedDevice(String deviceId, String deviceName, String macAddress,
                             String deviceType, String studentId, String studentName) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.macAddress = macAddress;
        this.deviceType = deviceType;
        this.studentId = studentId;
        this.studentName = studentName;
        this.enabled = true;
        this.registeredAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getDeviceId() { return deviceId; }
    public String getDeviceName() { return deviceName; }
    public String getMacAddress() { return macAddress; }
    public String getDeviceType() { return deviceType; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public boolean isEnabled() { return enabled; }
    public long getRegisteredAt() { return registeredAt; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    // ... other setters as needed
}