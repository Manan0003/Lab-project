package com.college.dispensary.service;

import com.college.dispensary.model.DispenseRecord;
import com.college.dispensary.model.Medicine;
import com.college.dispensary.model.PrescribedItem;
import com.college.dispensary.model.Prescription;
import com.college.dispensary.repository.DataStore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class InventoryService {
    private static final InventoryService INSTANCE = new InventoryService();
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DataStore dataStore = DataStore.getInstance();

    public static InventoryService getInstance() {
        return INSTANCE;
    }

    private InventoryService() {}

    public List<Medicine> getAllMedicines() {
        return new ArrayList<>(dataStore.getMedicines().values()).stream()
                .sorted(Comparator.comparing(Medicine::getName))
                .collect(Collectors.toList());
    }

    public Medicine getMedicine(String id) {
        return dataStore.getMedicines().get(id);
    }

    public synchronized Medicine addMedicine(Medicine med) {
        if (med.getId() == null || med.getId().isBlank()) {
            med.setId("MED-" + String.format("%03d", dataStore.getMedicines().size() + 1));
        }
        dataStore.getMedicines().put(med.getId(), med);
        dataStore.logActivity("PHARMACY", "PHARMACIST", "MEDICINE_ADDED",
                "Added new medicine: " + med.getName() + " (" + med.getId() + ") - Stock: " + med.getStockQuantity());
        dataStore.saveAsync();
        return med;
    }

    public synchronized Medicine updateMedicine(Medicine med) {
        Medicine existing = dataStore.getMedicines().get(med.getId());
        if (existing == null) return null;

        existing.setName(med.getName());
        existing.setGenericName(med.getGenericName());
        existing.setCategory(med.getCategory());
        existing.setStockQuantity(med.getStockQuantity());
        existing.setMinThreshold(med.getMinThreshold());
        existing.setUnit(med.getUnit());
        existing.setExpiryDate(med.getExpiryDate());
        existing.setBatchNo(med.getBatchNo());
        existing.setRackLocation(med.getRackLocation());
        existing.setDosageGuide(med.getDosageGuide());

        dataStore.logActivity("PHARMACY", "PHARMACIST", "MEDICINE_UPDATED",
                "Updated medicine: " + existing.getName() + " (" + existing.getId() + ")");
        dataStore.saveAsync();
        return existing;
    }

    public synchronized Medicine restockMedicine(String id, int addQty, String batchNo, String expiryDate, String rackLocation) {
        Medicine med = dataStore.getMedicines().get(id);
        if (med == null) return null;

        int oldStock = med.getStockQuantity();
        med.setStockQuantity(oldStock + addQty);
        if (batchNo != null && !batchNo.isBlank()) med.setBatchNo(batchNo);
        if (expiryDate != null && !expiryDate.isBlank()) med.setExpiryDate(expiryDate);
        if (rackLocation != null && !rackLocation.isBlank()) med.setRackLocation(rackLocation);

        dataStore.logActivity("PHARMACY", "PHARMACIST", "STOCK_RESTOCKED",
                "Restocked " + med.getName() + ": +" + addQty + " (New Stock: " + med.getStockQuantity() + ", Batch: " + med.getBatchNo() + ")");
        dataStore.saveAsync();
        return med;
    }

    public Map<String, Object> getAlerts() {
        LocalDate today = LocalDate.now();
        LocalDate warningThreshold = today.plusDays(60);

        List<Medicine> lowStock = new ArrayList<>();
        List<Medicine> expiringSoon = new ArrayList<>();
        List<Medicine> expired = new ArrayList<>();

        for (Medicine m : dataStore.getMedicines().values()) {
            if (m.getStockQuantity() <= m.getMinThreshold()) {
                lowStock.add(m);
            }
            if (m.getExpiryDate() != null && !m.getExpiryDate().isBlank()) {
                try {
                    LocalDate exp = LocalDate.parse(m.getExpiryDate().substring(0, 10));
                    if (exp.isBefore(today)) {
                        expired.add(m);
                    } else if (exp.isBefore(warningThreshold)) {
                        expiringSoon.add(m);
                    }
                } catch (Exception ignored) {}
            }
        }

        Map<String, Object> alerts = new LinkedHashMap<>();
        alerts.put("lowStock", lowStock);
        alerts.put("expiringSoon", expiringSoon);
        alerts.put("expired", expired);
        alerts.put("lowStockCount", lowStock.size());
        alerts.put("expiringSoonCount", expiringSoon.size());
        alerts.put("expiredCount", expired.size());
        return alerts;
    }

    public List<Prescription> getPendingPrescriptions() {
        return dataStore.getPrescriptions().values().stream()
                .filter(p -> "PENDING_DISPENSE".equals(p.getStatus()))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList());
    }

    public synchronized boolean dispensePrescription(String prescriptionId, String pharmacistName, String notes) {
        Prescription rx = dataStore.getPrescriptions().get(prescriptionId);
        if (rx == null || !"PENDING_DISPENSE".equals(rx.getStatus())) {
            return false;
        }

        String nowStr = LocalDateTime.now().format(ISO_FMT);

        // Deduct stock for all items
        for (PrescribedItem item : rx.getItems()) {
            Medicine med = dataStore.getMedicines().get(item.getMedicineId());
            if (med != null) {
                int newStock = Math.max(0, med.getStockQuantity() - item.getQuantity());
                med.setStockQuantity(newStock);

                // Create audit record
                DispenseRecord record = new DispenseRecord(
                        dataStore.nextDispenseId(),
                        rx.getId(),
                        rx.getStudentRollNo(),
                        rx.getStudentName(),
                        med.getId(),
                        med.getName(),
                        item.getQuantity(),
                        med.getBatchNo(),
                        nowStr,
                        pharmacistName != null ? pharmacistName : "Campus Pharmacist",
                        notes
                );
                dataStore.getDispenseRecords().add(0, record);
                item.setDispensed(true);
            }
        }

        rx.setStatus("DISPENSED");
        rx.setDispensedAt(nowStr);
        rx.setDispensedBy(pharmacistName != null ? pharmacistName : "Campus Pharmacist");

        dataStore.logActivity(pharmacistName != null ? pharmacistName : "PHARMACY", "PHARMACIST", "PRESCRIPTION_DISPENSED",
                "Dispensed medicines for Rx " + rx.getId() + " to " + rx.getStudentName() + " (" + rx.getStudentRollNo() + ")");

        dataStore.saveAsync();
        return true;
    }

    public synchronized DispenseRecord dispenseDirect(String studentRollNo, String medicineId, int quantity, String pharmacistName, String notes) {
        Medicine med = dataStore.getMedicines().get(medicineId);
        if (med == null || med.getStockQuantity() < quantity) {
            return null;
        }

        med.setStockQuantity(med.getStockQuantity() - quantity);
        String studentName = dataStore.getStudents().containsKey(studentRollNo)
                ? dataStore.getStudents().get(studentRollNo).getName()
                : "Walk-in (" + studentRollNo + ")";

        String nowStr = LocalDateTime.now().format(ISO_FMT);
        DispenseRecord record = new DispenseRecord(
                dataStore.nextDispenseId(),
                "DIRECT-OTC",
                studentRollNo,
                studentName,
                med.getId(),
                med.getName(),
                quantity,
                med.getBatchNo(),
                nowStr,
                pharmacistName != null ? pharmacistName : "Campus Pharmacist",
                notes
        );

        dataStore.getDispenseRecords().add(0, record);
        dataStore.logActivity(pharmacistName != null ? pharmacistName : "PHARMACY", "PHARMACIST", "DIRECT_DISPENSED",
                "OTC Dispensed " + quantity + "x " + med.getName() + " to " + studentName);

        dataStore.saveAsync();
        return record;
    }

    public List<DispenseRecord> getDispenseHistory() {
        return dataStore.getDispenseRecords();
    }
}
