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
        File tokensFile = new File(AppConfig.DATA_DIR, "tokens.json");
        // Check if existing data has at least 10 tokens; if not, re-seed full data
        if (tokensFile.exists() && tokensFile.length() > 50) {
            try {
                loadFromDisk();
                if (tokens.size() >= 10) {
                    System.out.println("Thapar Dispensary DataStore loaded from disk (" + tokens.size() + " tokens).");
                    return;
                }
            } catch (Exception e) {
                System.err.println("Could not load existing data, re-seeding defaults: " + e.getMessage());
            }
        }
        seedInitialData();
        saveToDisk();
        System.out.println("Thapar Dispensary DataStore initialized with 12+ tokens and rich seed data.");
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

        // 1. Users (5 Roles)
        users.put("admin", new User("U-001", "admin", "admin123", "Dr. Vikramjit Singh (Admin)", "ADMIN", "admin.dispensary@thapar.edu", nowStr));
        users.put("doctor", new User("U-002", "doctor", "doctor123", "Dr. Ananya Sharma (MD)", "DOCTOR", "dr.ananya@thapar.edu", nowStr));
        users.put("reception", new User("U-003", "reception", "reception123", "Mr. Gurpreet Singh (Reception)", "RECEPTIONIST", "reception.health@thapar.edu", nowStr));
        users.put("pharma", new User("U-004", "pharma", "pharma123", "Mr. Rakesh Verma (B.Pharm)", "PHARMACIST", "pharmacy.dispensary@thapar.edu", nowStr));

        // 2. Doctors
        doctors.put("DOC-001", new Doctor("DOC-001", "Dr. Ananya Sharma", "Chief Medical Officer / Internal Medicine", "MBBS, MD (Medicine)", "Chamber 1", "+91 98765 11001", "dr.ananya@thapar.edu", "Mon - Sat", "09:00 AM - 01:00 PM, 03:00 PM - 06:00 PM", "AVAILABLE"));
        doctors.put("DOC-002", new Doctor("DOC-002", "Dr. Rajesh Verma", "Senior Resident Physician", "MBBS, DNB (Family Medicine)", "Chamber 2", "+91 98765 11002", "dr.rajesh@thapar.edu", "Mon - Fri", "10:00 AM - 02:00 PM, 04:00 PM - 07:00 PM", "AVAILABLE"));
        doctors.put("DOC-003", new Doctor("DOC-003", "Dr. Harpreet Kaur", "Consultant Physician & Women's Health", "MBBS, DGO", "Chamber 3", "+91 98765 11003", "dr.harpreet@thapar.edu", "Mon - Sat", "08:30 AM - 12:30 PM", "AVAILABLE"));

        // 3. Students / Patients (12 registered students across departments)
        registerStudent("102103456", "Aarav Patel", "Computer Science & Engg (CSED)", "3rd Year", "Male", 20, "B+", "Penicillin, Amoxicillin", "Asthma (Mild)", "Suresh Patel (Father)", "+91 98221 11223", "+91 99887 76655");
        registerStudent("102204123", "Priya Nair", "Electronics & Comm (ECED)", "2nd Year", "Female", 19, "O+", "Sulfa Drugs", "None", "Radha Nair (Mother)", "+91 98440 22334", "+91 91234 56789");
        registerStudent("102008765", "Rohan Gupta", "Mechanical Engg (MED)", "4th Year", "Male", 21, "A+", "None", "Allergic Rhinitis", "Deepak Gupta (Brother)", "+91 97110 33445", "+91 98765 12345");
        registerStudent("102301987", "Sneha Rao", "Chemical Engg (CHED)", "1st Year", "Female", 18, "AB+", "Aspirin, NSAIDs", "None", "Venkatesh Rao (Father)", "+91 96550 44556", "+91 99441 22334");
        registerStudent("102105678", "Karan Sharma", "Computer Science (CSED)", "3rd Year", "Male", 20, "O+", "None", "None", "Rajesh Sharma (Father)", "+91 98111 22334", "+91 97112 33445");
        registerStudent("102208912", "Ananya Verma", "Electronics & Comm (ECED)", "2nd Year", "Female", 19, "B+", "Dust, Pollen", "None", "Sunil Verma (Father)", "+91 98333 44556", "+91 91223 44556");
        registerStudent("102003411", "Vikram Malhotra", "Civil Engg (CED)", "4th Year", "Male", 21, "A-", "None", "None", "Kamal Malhotra (Father)", "+91 98444 55667", "+91 98777 66554");
        registerStudent("102307843", "Simranjit Kaur", "Biotechnology", "1st Year", "Female", 18, "O-", "Sulfa Drugs", "None", "Gurdev Singh (Father)", "+91 98555 66778", "+91 99888 77665");
        registerStudent("102109823", "Jaspreet Singh", "Mechanical Engg (MED)", "3rd Year", "Male", 20, "B-", "None", "None", "Balwinder Singh (Father)", "+91 98666 77889", "+91 97665 44332");
        registerStudent("102201944", "Mehak Preet", "Chemical Engg (CHED)", "2nd Year", "Female", 19, "AB-", "Aspirin", "Migraine", "Sukhdev Singh (Father)", "+91 98777 88990", "+91 98123 45678");
        registerStudent("102004599", "Rahul Singla", "Computer Science (CSED)", "4th Year", "Male", 21, "A+", "None", "None", "Pawan Singla (Father)", "+91 98888 99001", "+91 99112 23344");
        registerStudent("102306711", "Divya Jindal", "Electronics & Comm (ECED)", "1st Year", "Female", 18, "O+", "None", "None", "Anil Jindal (Father)", "+91 98999 00112", "+91 98776 54321");

        // 4. TOKENS - SEED AT LEAST 12 TOKENS FOR 10+ VISIBLE QUEUE BOARD
        // T-101: In consultation
        Token t101 = createSeedToken("T-101", 101, "102103456", "Aarav Patel", "CSED", "High fever (101°F), severe body ache, shivering", "URGENT", "IN_CONSULTATION", "WALK_IN", "09:30 AM - 09:45 AM", "Dr. Ananya Sharma", "Mr. Gurpreet Singh (Reception)");
        t101.setCalledAt(nowStr);

        // T-102 to T-106: WAITING (Checked In by Admin/Reception)
        createSeedToken("T-102", 102, "102204123", "Priya Nair", "ECED", "Throat irritation, dry cough and mild headache", "NORMAL", "WAITING", "WALK_IN", "09:45 AM - 10:00 AM", null, "Mr. Gurpreet Singh (Reception)");
        createSeedToken("T-103", 103, "102008765", "Rohan Gupta", "MED", "Sprained left ankle during basketball practice, acute swelling", "URGENT", "WAITING", "WALK_IN", "10:00 AM - 10:15 AM", null, "Dr. Vikramjit Singh (Admin)");
        createSeedToken("T-104", 104, "102301987", "Sneha Rao", "CHED", "Stomach ache, acid reflux and nausea after hostel dinner", "NORMAL", "WAITING", "WALK_IN", "10:15 AM - 10:30 AM", null, "Mr. Gurpreet Singh (Reception)");
        createSeedToken("T-105", 105, "102105678", "Karan Sharma", "CSED", "Allergic rash on arms, persistent itching", "NORMAL", "WAITING", "WALK_IN", "10:30 AM - 10:45 AM", null, "Front-Desk Staff");
        createSeedToken("T-106", 106, "102208912", "Ananya Verma", "ECED", "Eye redness, burning sensation and watery discharge", "NORMAL", "WAITING", "WALK_IN", "10:45 AM - 11:00 AM", null, "Front-Desk Staff");

        // T-107 to T-112: AWAITING_ARRIVAL (Booked online, awaiting physical arrival confirmation by admin)
        createSeedToken("T-107", 107, "102003411", "Vikram Malhotra", "CED", "Mild fever and lower back stiffness", "NORMAL", "AWAITING_ARRIVAL", "WALK_IN", "11:00 AM - 11:15 AM", null, null);
        createSeedToken("T-108", 108, "102307843", "Simranjit Kaur", "Biotech", "Cut on right index finger while working in laboratory", "URGENT", "AWAITING_ARRIVAL", "WALK_IN", "11:15 AM - 11:30 AM", null, null);
        createSeedToken("T-109", 109, "102109823", "Jaspreet Singh", "MED", "Follow-up consultation for seasonal asthma", "NORMAL", "AWAITING_ARRIVAL", "SCHEDULED", "11:30 AM - 11:45 AM", null, null);
        createSeedToken("T-110", 110, "102201944", "Mehak Preet", "CHED", "Throbbing migraine headache and light sensitivity", "NORMAL", "AWAITING_ARRIVAL", "WALK_IN", "11:45 AM - 12:00 PM", null, null);
        createSeedToken("T-111", 111, "102004599", "Rahul Singla", "CSED", "Severe cold, nasal blockage and sinus pressure", "NORMAL", "AWAITING_ARRIVAL", "SCHEDULED", "12:00 PM - 12:15 PM", null, null);
        createSeedToken("T-112", 112, "102306711", "Divya Jindal", "ECED", "Blister burn on palm from hostel kettle steam", "NORMAL", "AWAITING_ARRIVAL", "WALK_IN", "12:15 PM - 12:30 PM", null, null);

        tokenCounter.set(112);

        // 5. Appointments
        Appointment apt1 = new Appointment("APT-1001", 101, "102103456", "Aarav Patel", "CSED", "DOC-001", "Dr. Ananya Sharma", todayDate, "09:30 AM - 10:00 AM", "High fever (101°F), severe body ache, shivering", "URGENT", "Confirmed", nowStr);
        apt1.setTokenId("T-101");
        apt1.setConfirmedAt(nowStr);
        apt1.setConfirmedBy("Mr. Gurpreet Singh (Reception)");

        Appointment apt2 = new Appointment("APT-1002", 109, "102109823", "Jaspreet Singh", "MED", "DOC-001", "Dr. Ananya Sharma", todayDate, "11:30 AM - 12:00 PM", "Follow-up consultation for seasonal asthma", "NORMAL", "Pending", nowStr);
        apt2.setTokenId("T-109");

        Appointment apt3 = new Appointment("APT-1003", 111, "102004599", "Rahul Singla", "CSED", "DOC-002", "Dr. Rajesh Verma", todayDate, "12:00 PM - 12:30 PM", "Severe cold, nasal blockage and sinus pressure", "NORMAL", "Pending", nowStr);
        apt3.setTokenId("T-111");

        appointments.put(apt1.getId(), apt1);
        appointments.put(apt2.getId(), apt2);
        appointments.put(apt3.getId(), apt3);
        appointmentCounter.set(1003);

        // 6. Medicines Inventory
        addMedicine("MED-001", "Paracetamol 500mg", "Acetaminophen", "Tablet", 200, 30, "strips (10 tabs)", "2027-08-30", "BAT-PAR-25", "Shelf A-1", "1 tab TDS after meals");
        addMedicine("MED-002", "Amoxicillin 500mg", "Amoxicillin Trihydrate", "Capsule", 18, 25, "strips (10 caps)", "2026-12-15", "BAT-AMX-24", "Shelf B-2", "1 cap TDS for 5 days");
        addMedicine("MED-003", "Cetirizine 10mg", "Cetirizine HCl", "Tablet", 85, 20, "strips (10 tabs)", "2027-05-20", "BAT-CET-25", "Shelf A-3", "1 tab OD at night");
        addMedicine("MED-004", "Ibuprofen 400mg", "Ibuprofen", "Tablet", 60, 20, "strips (10 tabs)", "2027-10-10", "BAT-IBU-25", "Shelf A-2", "1 tab BD after food");
        addMedicine("MED-005", "ORS Hydration Powder", "Oral Rehydration Salts IP", "Sachet", 95, 30, "sachets", "2028-01-01", "BAT-ORS-26", "Shelf C-1", "Mix in 1L clean drinking water");
        addMedicine("MED-006", "Azithromycin 500mg", "Azithromycin", "Tablet", 8, 20, "strips (3 tabs)", "2027-03-15", "BAT-AZI-25", "Shelf B-1", "1 tab OD 1 hr before meal");
        addMedicine("MED-007", "Dextromethorphan Cough Syrup", "Dextromethorphan HBr", "Syrup", 35, 15, "bottles (100ml)", "2027-04-10", "BAT-CSY-25", "Shelf C-2", "10ml TDS");
        addMedicine("MED-008", "Betadine 5% Antiseptic Ointment", "Povidone-Iodine", "Ointment", 22, 10, "tubes (20g)", "2027-11-25", "BAT-BET-25", "Shelf D-1", "Apply locally BD");
        addMedicine("MED-009", "Pantoprazole 40mg", "Pantoprazole Sodium", "Tablet", 70, 25, "strips (10 tabs)", "2027-09-18", "BAT-PAN-25", "Shelf A-4", "1 tab OD empty stomach in morning");
        addMedicine("MED-010", "Sterile Crepe Bandage 10cm", "Cotton Crepe Bandage", "First-Aid", 40, 15, "rolls", "2029-12-31", "BAT-BN-25", "Drawer F-1", "Wrap firmly over sprains");
        addMedicine("MED-011", "Tetanus Toxoid 0.5ml", "TT Vaccine", "Injection", 12, 10, "vials", "2027-02-28", "BAT-TT-24", "Refrigerator R-1", "0.5ml IM single dose");
        addMedicine("MED-012", "Ciprofloxacin Eye/Ear Drops", "Ciprofloxacin 0.3%", "Drop", 14, 10, "bottles (5ml)", "2026-11-30", "BAT-CIP-24", "Shelf D-2", "2 drops in affected eye TDS");

        // 7. Prescriptions History
        Prescription p1 = new Prescription();
        p1.setId("RX-2026-1001");
        p1.setTokenId("T-098");
        p1.setStudentRollNo("102103456");
        p1.setStudentName("Aarav Patel");
        p1.setDoctorName("Dr. Ananya Sharma");
        p1.setDate("2026-11-12 10:30");
        p1.setBloodPressure("118/78 mmHg");
        p1.setTemperature("98.4 °F");
        p1.setPulse("74 bpm");
        p1.setWeight("66 kg");
        p1.setSymptoms("Seasonal allergic sneezing, mild wheezing");
        p1.setDiagnosis("Allergic Rhinitis with mild bronchospasm");
        p1.setClinicalNotes("Student has history of mild asthma. Avoid cold beverages. Caution: Allergic to Penicillin.");
        p1.setStatus("DISPENSED");
        p1.setDispensedAt("2026-11-12 10:45");
        p1.setDispensedBy("Mr. Rakesh Verma (B.Pharm)");
        p1.setSickLeaveIssued(false);

        List<PrescribedItem> p1Items = new ArrayList<>();
        p1Items.add(new PrescribedItem("MED-003", "Cetirizine 10mg", "1-0-0 (Night)", "5 Days", 5, "Take with lukewarm water", true));
        p1Items.add(new PrescribedItem("MED-009", "Pantoprazole 40mg", "1-0-0 (Morning)", "5 Days", 5, "Take empty stomach before breakfast", true));
        p1.setItems(p1Items);
        prescriptions.put(p1.getId(), p1);

        dispenseRecords.add(new DispenseRecord("DISP-5001", "RX-2026-1001", "102103456", "Aarav Patel", "MED-003", "Cetirizine 10mg", 5, "BAT-CET-25", "2026-11-12 10:45", "Mr. Rakesh Verma", "Issued 5 tablets"));
        dispenseRecords.add(new DispenseRecord("DISP-5002", "RX-2026-1001", "102103456", "Aarav Patel", "MED-009", "Pantoprazole 40mg", 5, "BAT-PAN-25", "2026-11-12 10:45", "Mr. Rakesh Verma", "Advised empty stomach"));

        logActivity("SYSTEM", "SYSTEM", "SYSTEM_INIT", "Thapar University Health & Dispensary Management System initialized with 12 active queue tokens.");
        logActivity("admin", "ADMIN", "ARRIVAL_CONFIRMED", "Confirmed physical arrival for Token #103 (Rohan Gupta). Admitted to queue.");
        logActivity("doctor", "DOCTOR", "CONSULTATION_CALLED", "Called Token #101 (Aarav Patel) into Chamber 1.");
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
