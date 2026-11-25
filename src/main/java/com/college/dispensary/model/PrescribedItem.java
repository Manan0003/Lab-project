package com.college.dispensary.model;

public class PrescribedItem {
    private String medicineId;
    private String medicineName;
    private String dosage;        // e.g. "1-0-1 (After Food)"
    private String duration;      // e.g. "3 Days"
    private int quantity;         // e.g. 6
    private String instructions;  // e.g. "Drink plenty of warm water"
    private boolean dispensed;    // true if already dispensed by pharmacy

    public PrescribedItem() {}

    public PrescribedItem(String medicineId, String medicineName, String dosage, String duration,
                          int quantity, String instructions, boolean dispensed) {
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.duration = duration;
        this.quantity = quantity;
        this.instructions = instructions;
        this.dispensed = dispensed;
    }

    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public boolean isDispensed() { return dispensed; }
    public void setDispensed(boolean dispensed) { this.dispensed = dispensed; }
}
