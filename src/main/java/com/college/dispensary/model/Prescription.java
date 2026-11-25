package com.college.dispensary.model;

import java.util.ArrayList;
import java.util.List;

public class Prescription {
    private String id;               // e.g. "RX-2026-001"
    private String tokenId;          // related token
    private String studentRollNo;
    private String studentName;
    private String doctorName;
    private String date;             // YYYY-MM-DD HH:mm
    
    // Vitals recorded by Doctor
    private String bloodPressure;    // e.g. "120/80 mmHg"
    private String temperature;      // e.g. "98.6 °F"
    private String pulse;            // e.g. "72 bpm"
    private String weight;           // e.g. "65 kg"

    // Clinical Details
    private String symptoms;
    private String diagnosis;
    private String clinicalNotes;
    private List<PrescribedItem> items = new ArrayList<>();

    // Sick Leave / Medical Rest Certificate (if recommended)
    private boolean sickLeaveIssued;
    private int sickLeaveDays;
    private String sickLeaveFrom;
    private String sickLeaveTo;
    private String sickLeaveAdvice;

    // Status: "PENDING_DISPENSE", "DISPENSED", "PARTIALLY_DISPENSED"
    private String status;
    private String dispensedAt;
    private String dispensedBy;

    public Prescription() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }

    public String getStudentRollNo() { return studentRollNo; }
    public void setStudentRollNo(String studentRollNo) { this.studentRollNo = studentRollNo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }

    public String getTemperature() { return temperature; }
    public void setTemperature(String temperature) { this.temperature = temperature; }

    public String getPulse() { return pulse; }
    public void setPulse(String pulse) { this.pulse = pulse; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getClinicalNotes() { return clinicalNotes; }
    public void setClinicalNotes(String clinicalNotes) { this.clinicalNotes = clinicalNotes; }

    public List<PrescribedItem> getItems() { return items; }
    public void setItems(List<PrescribedItem> items) { this.items = items; }

    public boolean isSickLeaveIssued() { return sickLeaveIssued; }
    public void setSickLeaveIssued(boolean sickLeaveIssued) { this.sickLeaveIssued = sickLeaveIssued; }

    public int getSickLeaveDays() { return sickLeaveDays; }
    public void setSickLeaveDays(int sickLeaveDays) { this.sickLeaveDays = sickLeaveDays; }

    public String getSickLeaveFrom() { return sickLeaveFrom; }
    public void setSickLeaveFrom(String sickLeaveFrom) { this.sickLeaveFrom = sickLeaveFrom; }

    public String getSickLeaveTo() { return sickLeaveTo; }
    public void setSickLeaveTo(String sickLeaveTo) { this.sickLeaveTo = sickLeaveTo; }

    public String getSickLeaveAdvice() { return sickLeaveAdvice; }
    public void setSickLeaveAdvice(String sickLeaveAdvice) { this.sickLeaveAdvice = sickLeaveAdvice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDispensedAt() { return dispensedAt; }
    public void setDispensedAt(String dispensedAt) { this.dispensedAt = dispensedAt; }

    public String getDispensedBy() { return dispensedBy; }
    public void setDispensedBy(String dispensedBy) { this.dispensedBy = dispensedBy; }
}
