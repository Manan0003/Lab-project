package com.college.dispensary.model;

public class Appointment {
    private String id;              // e.g. "APT-1001"
    private int tokenNumber;        // e.g. 101
    private String patientRollNo;   // Student roll number or staff ID
    private String patientName;
    private String department;
    private String doctorId;
    private String doctorName;
    private String appointmentDate; // YYYY-MM-DD
    private String timeSlot;        // e.g. "10:00 AM - 10:30 AM"
    private String symptoms;
    private String priority;        // "NORMAL", "URGENT", "EMERGENCY"
    private String status;          // "Pending", "Confirmed", "Completed", "Cancelled"
    private String createdAt;
    private String notes;

    public Appointment() {}

    public Appointment(String id, int tokenNumber, String patientRollNo, String patientName,
                       String department, String doctorId, String doctorName,
                       String appointmentDate, String timeSlot, String symptoms,
                       String priority, String status, String createdAt) {
        this.id = id;
        this.tokenNumber = tokenNumber;
        this.patientRollNo = patientRollNo;
        this.patientName = patientName;
        this.department = department;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.appointmentDate = appointmentDate;
        this.timeSlot = timeSlot;
        this.symptoms = symptoms;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(int tokenNumber) { this.tokenNumber = tokenNumber; }

    public String getPatientRollNo() { return patientRollNo; }
    public void setPatientRollNo(String patientRollNo) { this.patientRollNo = patientRollNo; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(String appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
