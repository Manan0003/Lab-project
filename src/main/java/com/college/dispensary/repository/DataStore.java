package com.college.dispensary.repository;

import com.college.dispensary.config.AppConfig;
import com.college.dispensary.model.*;
import com.college.dispensary.util.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class DataStore {
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DataStore INSTANCE = new DataStore();

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Student> students = new ConcurrentHashMap<>(); // Patients
    private final Map<String, Doctor> doctors = new ConcurrentHashMap<>();
    private final Map<String, Appointment> appointments = new ConcurrentHashMap<>();
    private final Map<String, Token> tokens = new ConcurrentHashMap<>();
    private final Map<String, Medicine> medicines = new ConcurrentHashMap<>();
    private final Map<String, Prescription> prescriptions = new ConcurrentHashMap<>();
    private final List<DispenseRecord> dispenseRecords = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> activityLogs = new CopyOnWriteArrayList<>();

    private final AtomicInteger tokenCounter = new AtomicInteger(100);
    private final AtomicInteger appointmentCounter = new AtomicInteger(1000);
    private final AtomicInteger prescriptionCounter = new AtomicInteger(1000);
    private final AtomicInteger dispenseCounter = new AtomicInteger(5000);

    public static DataStore getInstance() {
        return INSTANCE;
    }

    private DataStore() {
        AppConfig.ensureDataDirectories();
        loadOrSeedData();
    }

    // ==================== ACCESSORS ====================

    public Map<String, User> getUsers() { return users; }
    public Map<String, Student> getStudents() { return students; }
    public Map<String, Doctor> getDoctors() { return doctors; }
    public Map<String, Appointment> getAppointments() { return appointments; }
    public Map<String, Token> getTokens() { return tokens; }
    public Map<String, Medicine> getMedicines() { return medicines; }
    public Map<String, Prescription> getPrescriptions() { return prescriptions; }
    public List<DispenseRecord> getDispenseRecords() { return dispenseRecords; }
    public List<Map<String, Object>> getActivityLogs() { return activityLogs; }

    public synchronized String nextTokenId() {
        int num = tokenCounter.incrementAndGet();
        return "T-" + num;
    }

    public synchronized int nextTokenNumber() {
        return tokenCounter.get();
    }

    public synchronized String nextAppointmentId() {
        return "APT-" + appointmentCounter.incrementAndGet();
    }

    public synchronized String nextPrescriptionId() {
        return "RX-" + LocalDateTime.now().getYear() + "-" + prescriptionCounter.incrementAndGet();
    }

    public synchronized String nextDispenseId() {
        return "DISP-" + dispenseCounter.incrementAndGet();
    }

    public void logActivity(String user, String role, String action, String details) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("id", UUID.randomUUID().toString());
        log.put("timestamp", LocalDateTime.now().format(ISO_FMT));
        log.put("user", user);
        log.put("role", role);
        log.put("action", action);
        log.put("details", details);
        activityLogs.add(0, log);
        if (activityLogs.size() > 300) {
            activityLogs.remove(activityLogs.size() - 1);
        }
        saveAsync();
    }

    // ==================== PERSISTENCE ====================

    public synchronized void saveAsync() {
        Thread.ofVirtual().start(this::saveToDisk);
    }

    public synchronized void saveToDisk() {
        try {
            saveFile("users.json", users.values());
            saveFile("students.json", students.values());
            saveFile("doctors.json", doctors.values());
            saveFile("appointments.json", appointments.values());
            saveFile("tokens.json", tokens.values());
            saveFile("medicines.json", medicines.values());
            saveFile("prescriptions.json", prescriptions.values());
            saveFile("dispense_records.json", dispenseRecords);
            saveFile("activity_logs.json", activityLogs);
        } catch (Exception e) {
            System.err.println("Error saving DataStore to disk: " + e.getMessage());
        }
    }

    private void saveFile(String filename, Object data) throws IOException {
        File file = new File(AppConfig.DATA_DIR, filename);
        String json = JsonUtil.toJson(data);
        Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
    }

    public synchronized void resetToFactoryDefaults() {
        tokenCounter.set(100);
        appointmentCounter.set(1000);
        prescriptionCounter.set(1000);
        dispenseCounter.set(5000);

        seedInitialData();
        saveToDisk();
        System.out.println("[RESET] Thapar Dispensary DataStore has been completely reset to initial starting state.");
    }

    public void loadOrSeedData() {
        File usersFile = new File(AppConfig.DATA_DIR, "users.json");
        if (usersFile.exists() && usersFile.length() > 10) {
            try {
                loadFromDisk();
                System.out.println("Thapar Dispensary DataStore loaded from disk.");
                return;
            } catch (Exception e) {
                System.err.println("Could not load existing data, re-seeding defaults: " + e.getMessage());
            }
        }
        seedInitialData();
        saveToDisk();
        System.out.println("Thapar Dispensary DataStore initialized clean. Ready for student roll numbers and registrations.");
    }

    private void loadFromDisk() throws IOException {
        loadUsers();
        loadStudents();
        loadDoctors();
        loadAppointments();
        loadMedicines();
        loadTokens();
        loadPrescriptions();
        loadDispenseRecords();
        loadActivityLogs();
    }

    @SuppressWarnings("unchecked")
    private void loadUsers() throws IOException {
        File file = new File(AppConfig.DATA_DIR, "users.json");
        if (!file.exists()) return;
        List<Object> list = JsonUtil.parseArray(Files.readString(file.toPath()));
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                User u = new User(
                        (String) m.get("id"),
                        (String) m.get("username"),
                        (String) m.get("password"),
                        (String) m.get("fullName"),
                        (String) m.get("role"),
                        (String) m.get("email"),
                        (String) m.get("createdAt")
                );
                users.put(u.getUsername(), u);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadStudents() throws IOException {
        File file = new File(AppConfig.DATA_DIR, "students.json");
        if (!file.exists()) return;
        List<Object> list = JsonUtil.parseArray(Files.readString(file.toPath()));
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                int age = m.get("age") instanceof Number num ? num.intValue() : 20;
                Student s = new Student(
                        (String) m.get("rollNo"),
                        (String) m.get("name"),
                        (String) m.get("department"),
                        (String) m.get("year"),
                        (String) m.get("gender"),
                        age,
                        (String) m.get("bloodGroup"),
                        (String) m.get("allergies"),
                        (String) m.get("chronicConditions"),
                        (String) m.get("emergencyContactName"),
                        (String) m.get("emergencyPhone"),
                        (String) m.get("phone"),
                        (String) m.get("email")
                );
                students.put(s.getRollNo(), s);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadDoctors() throws IOException {
        File file = new File(AppConfig.DATA_DIR, "doctors.json");
        if (!file.exists()) return;
        List<Object> list = JsonUtil.parseArray(Files.readString(file.toPath()));
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                Doctor d = new Doctor(
                        (String) m.get("id"),
                        (String) m.get("name"),
                        (String) m.get("specialization"),
                        (String) m.get("qualification"),
                        (String) m.get("chamber"),
                        (String) m.get("contactNumber"),
                        (String) m.get("email"),
                        (String) m.get("availableDays"),
                        (String) m.get("availableHours"),
                        (String) m.get("status")
                );
                doctors.put(d.getId(), d);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadAppointments() throws IOException {
        File file = new File(AppConfig.DATA_DIR, "appointments.json");
        if (!file.exists()) return;
        List<Object> list = JsonUtil.parseArray(Files.readString(file.toPath()));
        int maxApt = 1000;
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                int tok = m.get("tokenNumber") instanceof Number n ? n.intValue() : 100;
                Appointment a = new Appointment(
                        (String) m.get("id"),
                        tok,
                        (String) m.get("patientRollNo"),
                        (String) m.get("patientName"),
                        (String) m.get("department"),
                        (String) m.get("doctorId"),
                        (String) m.get("doctorName"),
                        (String) m.get("appointmentDate"),
                        (String) m.get("timeSlot"),
                        (String) m.get("symptoms"),
                        (String) m.get("priority"),
                        (String) m.get("status"),
                        (String) m.get("createdAt")
                );
                a.setTokenId((String) m.get("tokenId"));
                a.setExpiresAt((String) m.get("expiresAt"));
                a.setConfirmedAt((String) m.get("confirmedAt"));
                a.setConfirmedBy((String) m.get("confirmedBy"));
                a.setNotes((String) m.get("notes"));
                appointments.put(a.getId(), a);
                try {
                    int num = Integer.parseInt(a.getId().replace("APT-", ""));
                    if (num > maxApt) maxApt = num;
                } catch (Exception ignored) {}
            }
        }
        appointmentCounter.set(maxApt);
    }

    @SuppressWarnings("unchecked")
    private void loadMedicines() throws IOException {
        File file = new File(AppConfig.DATA_DIR, "medicines.json");
        if (!file.exists()) return;
        List<Object> list = JsonUtil.parseArray(Files.readString(file.toPath()));
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                int stock = m.get("stockQuantity") instanceof Number n ? n.intValue() : 0;
                int min = m.get("minThreshold") instanceof Number n ? n.intValue() : 10;
                Medicine med = new Medicine(
                        (String) m.get("id"),
                        (String) m.get("name"),
                        (String) m.get("genericName"),
                        (String) m.get("category"),
                        stock,
                        min,
                        (String) m.get("unit"),
                        (String) m.get("expiryDate"),
                        (String) m.get("batchNo"),
                        (String) m.get("rackLocation"),
                        (String) m.get("dosageGuide")
                );
                medicines.put(med.getId(), med);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadTokens() throws IOException {
        File file = new File(AppConfig.DATA_DIR, "tokens.json");
        if (!file.exists()) return;
        List<Object> list = JsonUtil.parseArray(Files.readString(file.toPath()));
        int maxToken = 100;
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                int num = m.get("tokenNumber") instanceof Number n ? n.intValue() : 100;
                if (num > maxToken) maxToken = num;
                Token t = new Token(
                        (String) m.get("id"),
                        num,
                        (String) m.get("studentRollNo"),
                        (String) m.get("studentName"),
                        (String) m.get("department"),
                        (String) m.get("symptoms"),
                        (String) m.get("priority"),
                        (String) m.get("status"),
                        (String) m.get("createdAt")
                );
                t.setBookingType((String) m.get("bookingType"));
                t.setRunningSlot((String) m.get("runningSlot"));
                t.setAppointmentId((String) m.get("appointmentId"));
                t.setConfirmedAt((String) m.get("confirmedAt"));
                t.setConfirmedBy((String) m.get("confirmedBy"));
                t.setExpiresAt((String) m.get("expiresAt"));
                t.setCalledAt((String) m.get("calledAt"));
                t.setCompletedAt((String) m.get("completedAt"));
                t.setAssignedDoctor((String) m.get("assignedDoctor"));
                tokens.put(t.getId(), t);
            }
        }
        tokenCounter.set(maxToken);
    }

    @SuppressWarnings("unchecked")
    private void loadPrescriptions() throws IOException {
        File file = new File(AppConfig.DATA_DIR, "prescriptions.json");
        if (!file.exists()) return;
        List<Object> list = JsonUtil.parseArray(Files.readString(file.toPath()));
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                Prescription p = new Prescription();
                p.setId((String) m.get("id"));
                p.setTokenId((String) m.get("tokenId"));
                p.setStudentRollNo((String) m.get("studentRollNo"));
                p.setStudentName((String) m.get("studentName"));
                p.setDoctorName((String) m.get("doctorName"));
                p.setDate((String) m.get("date"));
                p.setBloodPressure((String) m.get("bloodPressure"));
                p.setTemperature((String) m.get("temperature"));
                p.setPulse((String) m.get("pulse"));
                p.setWeight((String) m.get("weight"));
                p.setSymptoms((String) m.get("symptoms"));
                p.setDiagnosis((String) m.get("diagnosis"));
                p.setClinicalNotes((String) m.get("clinicalNotes"));
                p.setStatus((String) m.get("status"));
                p.setDispensedAt((String) m.get("dispensedAt"));
                p.setDispensedBy((String) m.get("dispensedBy"));
                if (m.get("sickLeaveIssued") instanceof Boolean b) p.setSickLeaveIssued(b);
                if (m.get("sickLeaveDays") instanceof Number num) p.setSickLeaveDays(num.intValue());
                p.setSickLeaveFrom((String) m.get("sickLeaveFrom"));
                p.setSickLeaveTo((String) m.get("sickLeaveTo"));
                p.setSickLeaveAdvice((String) m.get("sickLeaveAdvice"));

                if (m.get("items") instanceof List<?> rawItems) {
                    List<PrescribedItem> items = new ArrayList<>();
                    for (Object oi : rawItems) {
                        if (oi instanceof Map<?, ?> mi) {
                            int q = mi.get("quantity") instanceof Number n ? n.intValue() : 1;
                            boolean disp = Boolean.TRUE.equals(mi.get("dispensed"));
                            items.add(new PrescribedItem(
                                    (String) mi.get("medicineId"),
                                    (String) mi.get("medicineName"),
                                    (String) mi.get("dosage"),
                                    (String) mi.get("duration"),
                                    q,
                                    (String) mi.get("instructions"),
                                    disp
                            ));
                        }
                    }
                    p.setItems(items);
                }
                prescriptions.put(p.getId(), p);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadDispenseRecords() throws IOException {
        File file = new File(AppConfig.DATA_DIR, "dispense_records.json");
        if (!file.exists()) return;
        List<Object> list = JsonUtil.parseArray(Files.readString(file.toPath()));
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                int q = m.get("quantity") instanceof Number n ? n.intValue() : 1;
                dispenseRecords.add(new DispenseRecord(
                        (String) m.get("id"),
                        (String) m.get("prescriptionId"),
                        (String) m.get("studentRollNo"),
                        (String) m.get("studentName"),
                        (String) m.get("medicineId"),
                        (String) m.get("medicineName"),
                        q,
                        (String) m.get("batchNo"),
                        (String) m.get("dispensedAt"),
                        (String) m.get("dispensedBy"),
                        (String) m.get("notes")
                ));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadActivityLogs() throws IOException {
        File file = new File(AppConfig.DATA_DIR, "activity_logs.json");
        if (!file.exists()) return;
        List<Object> list = JsonUtil.parseArray(Files.readString(file.toPath()));
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                activityLogs.add((Map<String, Object>) m);
            }
        }
    }

    // ==================== THAPAR SEED DATA ====================

    public void seedInitialData() {
        users.clear();
        students.clear();
        doctors.clear();
        appointments.clear();
        tokens.clear();
        medicines.clear();
        prescriptions.clear();
        dispenseRecords.clear();
        activityLogs.clear();

        String nowStr = LocalDateTime.now().format(ISO_FMT);
        String todayDate = LocalDate.now().toString();

        // 1. Core Staff Users (Admin, Doctor, Receptionist, Pharmacist)
        users.put("admin", new User("U-001", "admin", "admin123", "Dr. Vikramjit Singh (Admin)", "ADMIN", "admin.dispensary@thapar.edu", nowStr));
        users.put("doctor", new User("U-002", "doctor", "doctor123", "Dr. Ananya Sharma (MD)", "DOCTOR", "dr.ananya@thapar.edu", nowStr));
        users.put("reception", new User("U-003", "reception", "reception123", "Mr. Gurpreet Singh (Reception)", "RECEPTIONIST", "reception.health@thapar.edu", nowStr));
        users.put("pharma", new User("U-004", "pharma", "pharma123", "Mr. Rakesh Verma (B.Pharm)", "PHARMACIST", "pharmacy.dispensary@thapar.edu", nowStr));

        // 2. On-Duty Doctors in Chambers
        doctors.put("DOC-001", new Doctor("DOC-001", "Dr. Ananya Sharma", "Chief Medical Officer / Internal Medicine", "MBBS, MD (Medicine)", "Chamber 1", "+91 98765 11001", "dr.ananya@thapar.edu", "Mon - Sat", "09:00 AM - 01:00 PM, 03:00 PM - 06:00 PM", "AVAILABLE"));
        doctors.put("DOC-002", new Doctor("DOC-002", "Dr. Rajesh Verma", "Senior Resident Physician", "MBBS, DNB (Family Medicine)", "Chamber 2", "+91 98765 11002", "dr.rajesh@thapar.edu", "Mon - Fri", "10:00 AM - 02:00 PM, 04:00 PM - 07:00 PM", "AVAILABLE"));
        doctors.put("DOC-003", new Doctor("DOC-003", "Dr. Harpreet Kaur", "Consultant Physician & Women's Health", "MBBS, DGO", "Chamber 3", "+91 98765 11003", "dr.harpreet@thapar.edu", "Mon - Sat", "08:30 AM - 12:30 PM", "AVAILABLE"));

        // 3. Students / Patients - Clean slate: users register their own roll numbers & passwords
        tokenCounter.set(100);
        appointmentCounter.set(1000);
        prescriptionCounter.set(1000);
        dispenseCounter.set(5000);

        // 4. Medicines Inventory (Dispensary pharmacy catalogue with fresh stock)
        addMedicine("MED-001", "Paracetamol 500mg", "Acetaminophen", "Tablet", 200, 30, "strips (10 tabs)", "2027-08-30", "BAT-PAR-25", "Shelf A-1", "1 tab TDS after meals");
        addMedicine("MED-002", "Amoxicillin 500mg", "Amoxicillin Trihydrate", "Capsule", 50, 25, "strips (10 caps)", "2026-12-15", "BAT-AMX-24", "Shelf B-2", "1 cap TDS for 5 days");
        addMedicine("MED-003", "Cetirizine 10mg", "Cetirizine HCl", "Tablet", 100, 20, "strips (10 tabs)", "2027-05-20", "BAT-CET-25", "Shelf A-3", "1 tab OD at night");
        addMedicine("MED-004", "Ibuprofen 400mg", "Ibuprofen", "Tablet", 80, 20, "strips (10 tabs)", "2027-10-10", "BAT-IBU-25", "Shelf A-2", "1 tab BD after food");
        addMedicine("MED-005", "ORS Hydration Powder", "Oral Rehydration Salts IP", "Sachet", 100, 30, "sachets", "2028-01-01", "BAT-ORS-26", "Shelf C-1", "Mix in 1L clean drinking water");
        addMedicine("MED-006", "Azithromycin 500mg", "Azithromycin", "Tablet", 40, 20, "strips (3 tabs)", "2027-03-15", "BAT-AZI-25", "Shelf B-1", "1 tab OD 1 hr before meal");
        addMedicine("MED-007", "Dextromethorphan Cough Syrup", "Dextromethorphan HBr", "Syrup", 40, 15, "bottles (100ml)", "2027-04-10", "BAT-CSY-25", "Shelf C-2", "10ml TDS");
        addMedicine("MED-008", "Betadine 5% Antiseptic Ointment", "Povidone-Iodine", "Ointment", 30, 10, "tubes (20g)", "2027-11-25", "BAT-BET-25", "Shelf D-1", "Apply locally BD");
        addMedicine("MED-009", "Pantoprazole 40mg", "Pantoprazole Sodium", "Tablet", 80, 25, "strips (10 tabs)", "2027-09-18", "BAT-PAN-25", "Shelf A-4", "1 tab OD empty stomach in morning");
        addMedicine("MED-010", "Sterile Crepe Bandage 10cm", "Cotton Crepe Bandage", "First-Aid", 40, 15, "rolls", "2029-12-31", "BAT-BN-25", "Drawer F-1", "Wrap firmly over sprains");
        addMedicine("MED-011", "Tetanus Toxoid 0.5ml", "TT Vaccine", "Injection", 20, 10, "vials", "2027-02-28", "BAT-TT-24", "Refrigerator R-1", "0.5ml IM single dose");
        addMedicine("MED-012", "Ciprofloxacin Eye/Ear Drops", "Ciprofloxacin 0.3%", "Drop", 25, 10, "bottles (5ml)", "2026-11-30", "BAT-CIP-24", "Shelf D-2", "2 drops in affected eye TDS");

        logActivity("SYSTEM", "SYSTEM", "SYSTEM_INIT", "Thapar University Health & Dispensary Management System initialized fresh. Ready for registrations and consultations.");
    }

    private void registerStudent(String rollNo, String name, String dept, String year, String gender, int age,
                                 String bloodGroup, String allergies, String chronic,
                                 String emgName, String emgPhone, String phone) {
        Student s = new Student(rollNo, name, dept, year, gender, age, bloodGroup, allergies, chronic, emgName, emgPhone, phone, rollNo + "@thapar.edu");
        students.put(rollNo, s);
        users.put(rollNo, new User("U-" + rollNo, rollNo, "student123", name, "STUDENT", s.getEmail(), LocalDateTime.now().format(ISO_FMT)));
    }

    private Token createSeedToken(String id, int num, String rollNo, String name, String dept, String symptoms,
                                  String priority, String status, String type, String slot, String doctor, String confirmedBy) {
        String nowStr = LocalDateTime.now().format(ISO_FMT);
        Token t = new Token(id, num, rollNo, name, dept, symptoms, priority, status, nowStr);
        t.setBookingType(type);
        t.setRunningSlot(slot);
        t.setAssignedDoctor(doctor);
        t.setConfirmedBy(confirmedBy);
        if (confirmedBy != null) {
            t.setConfirmedAt(nowStr);
        }
        tokens.put(id, t);
        return t;
    }

    private void addMedicine(String id, String name, String generic, String cat, int stock, int min, String unit, String exp, String batch, String rack, String guide) {
        medicines.put(id, new Medicine(id, name, generic, cat, stock, min, unit, exp, batch, rack, guide));
    }
}
