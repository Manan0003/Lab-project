package com.college.dispensary.model;

public class Token {
    private String id;               // e.g. "T-101"
    private int tokenNumber;         // e.g. 101
    private String studentRollNo;
    private String studentName;
    private String department;
    private String symptoms;
    private String priority;          // "NORMAL", "URGENT", "EMERGENCY"
    private String status;            // "WAITING", "IN_CONSULTATION", "COMPLETED", "CANCELLED"
    private String createdAt;
    private String calledAt;
    private String completedAt;
    private String assignedDoctor;

    public Token() {}

    public Token(String id, int tokenNumber, String studentRollNo, String studentName,
                 String department, String symptoms, String priority, String status,
                 String createdAt) {
        this.id = id;
        this.tokenNumber = tokenNumber;
        this.studentRollNo = studentRollNo;
        this.studentName = studentName;
        this.department = department;
        this.symptoms = symptoms;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(int tokenNumber) { this.tokenNumber = tokenNumber; }

    public String getStudentRollNo() { return studentRollNo; }
    public void setStudentRollNo(String studentRollNo) { this.studentRollNo = studentRollNo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getCalledAt() { return calledAt; }
    public void setCalledAt(String calledAt) { this.calledAt = calledAt; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }

    public String getAssignedDoctor() { return assignedDoctor; }
    public void setAssignedDoctor(String assignedDoctor) { this.assignedDoctor = assignedDoctor; }
}
