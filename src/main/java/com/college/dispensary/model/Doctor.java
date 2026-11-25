package com.college.dispensary.model;

public class Doctor {
    private String id;              // e.g. "DOC-001"
    private String name;            // e.g. "Dr. Ananya Sharma"
    private String specialization;  // e.g. "Chief Medical Officer / General Medicine (MD)"
    private String qualification;   // e.g. "MBBS, MD (Medicine)"
    private String chamber;         // e.g. "Chamber 1 - Ground Floor"
    private String contactNumber;   // e.g. "+91 98765 11223"
    private String email;
    private String availableDays;   // e.g. "Mon - Sat"
    private String availableHours;  // e.g. "09:00 AM - 01:00 PM, 03:00 PM - 06:00 PM"
    private String status;          // "AVAILABLE", "IN_CONSULTATION", "ON_LEAVE"

    public Doctor() {}

    public Doctor(String id, String name, String specialization, String qualification,
                  String chamber, String contactNumber, String email,
                  String availableDays, String availableHours, String status) {
        this.id = id;
        this.name = name;
        this.specialization = specialization;
        this.qualification = qualification;
        this.chamber = chamber;
        this.contactNumber = contactNumber;
        this.email = email;
        this.availableDays = availableDays;
        this.availableHours = availableHours;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public String getChamber() { return chamber; }
    public void setChamber(String chamber) { this.chamber = chamber; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvailableDays() { return availableDays; }
    public void setAvailableDays(String availableDays) { this.availableDays = availableDays; }

    public String getAvailableHours() { return availableHours; }
    public void setAvailableHours(String availableHours) { this.availableHours = availableHours; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
