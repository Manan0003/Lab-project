package com.college.dispensary.model;

public class Medicine {
    private String id;
    private String name;               // e.g. "Paracetamol 500mg"
    private String genericName;        // e.g. "Acetaminophen"
    private String category;           // "Tablet", "Syrup", "Capsule", "Ointment", "Injection", "First-Aid", "Drop"
    private int stockQuantity;         // Units currently in stock
    private int minThreshold;          // Alert when below this quantity
    private String unit;               // "strips", "bottles", "tubes", "vials", "boxes"
    private String expiryDate;         // YYYY-MM-DD
    private String batchNo;            // e.g. "BAT-2025-09"
    private String rackLocation;       // e.g. "Shelf A-1", "Fridge 1"
    private String dosageGuide;        // e.g. "1 tab twice daily after food"

    public Medicine() {}

    public Medicine(String id, String name, String genericName, String category, int stockQuantity,
                    int minThreshold, String unit, String expiryDate, String batchNo,
                    String rackLocation, String dosageGuide) {
        this.id = id;
        this.name = name;
        this.genericName = genericName;
        this.category = category;
        this.stockQuantity = stockQuantity;
        this.minThreshold = minThreshold;
        this.unit = unit;
        this.expiryDate = expiryDate;
        this.batchNo = batchNo;
        this.rackLocation = rackLocation;
        this.dosageGuide = dosageGuide;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public int getMinThreshold() { return minThreshold; }
    public void setMinThreshold(int minThreshold) { this.minThreshold = minThreshold; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public String getRackLocation() { return rackLocation; }
    public void setRackLocation(String rackLocation) { this.rackLocation = rackLocation; }

    public String getDosageGuide() { return dosageGuide; }
    public void setDosageGuide(String dosageGuide) { this.dosageGuide = dosageGuide; }
}
