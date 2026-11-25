package com.college.dispensary.model;

public class DispenseRecord {
    private String id;
    private String prescriptionId;
    private String studentRollNo;
    private String studentName;
    private String medicineId;
    private String medicineName;
    private int quantity;
    private String batchNo;
    private String dispensedAt;
    private String dispensedBy;
    private String notes;

    public DispenseRecord() {}

    public DispenseRecord(String id, String prescriptionId, String studentRollNo, String studentName,
                          String medicineId, String medicineName, int quantity, String batchNo,
                          String dispensedAt, String dispensedBy, String notes) {
        this.id = id;
        this.prescriptionId = prescriptionId;
        this.studentRollNo = studentRollNo;
        this.studentName = studentName;
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.quantity = quantity;
        this.batchNo = batchNo;
        this.dispensedAt = dispensedAt;
        this.dispensedBy = dispensedBy;
        this.notes = notes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(String prescriptionId) { this.prescriptionId = prescriptionId; }

    public String getStudentRollNo() { return studentRollNo; }
    public void setStudentRollNo(String studentRollNo) { this.studentRollNo = studentRollNo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public String getDispensedAt() { return dispensedAt; }
    public void setDispensedAt(String dispensedAt) { this.dispensedAt = dispensedAt; }

    public String getDispensedBy() { return dispensedBy; }
    public void setDispensedBy(String dispensedBy) { this.dispensedBy = dispensedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
