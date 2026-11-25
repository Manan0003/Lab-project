package com.college.dispensary.service;

import com.college.dispensary.model.Student;
import com.college.dispensary.model.User;
import com.college.dispensary.repository.DataStore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class PatientService {
    private static final PatientService INSTANCE = new PatientService();
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DataStore dataStore = DataStore.getInstance();

    public static PatientService getInstance() {
        return INSTANCE;
    }

    private PatientService() {}

    public synchronized Student registerPatient(Student s) {
        if (s.getRollNo() == null || s.getRollNo().isBlank()) {
            throw new IllegalArgumentException("University Roll Number / ID is required.");
        }
        dataStore.getStudents().put(s.getRollNo(), s);

        // Also ensure user login exists
        if (!dataStore.getUsers().containsKey(s.getRollNo())) {
            String nowStr = LocalDateTime.now().format(ISO_FMT);
            User u = new User(
                    "U-" + s.getRollNo(),
                    s.getRollNo(),
                    "student123", // default password
                    s.getName(),
                    "STUDENT",
                    s.getEmail(),
                    nowStr
            );
            dataStore.getUsers().put(u.getUsername(), u);
        }

        dataStore.logActivity("RECEPTION", "RECEPTIONIST", "PATIENT_REGISTERED",
                "Registered new patient: " + s.getName() + " (" + s.getRollNo() + " - " + s.getDepartment() + ")");
        dataStore.saveAsync();
        return s;
    }

    public Student getPatient(String rollNo) {
        return dataStore.getStudents().get(rollNo);
    }

    public List<Student> getAllPatients() {
        return new ArrayList<>(dataStore.getStudents().values()).stream()
                .sorted(Comparator.comparing(Student::getName))
                .collect(Collectors.toList());
    }

    public List<Student> searchPatients(String query) {
        if (query == null || query.isBlank()) {
            return getAllPatients();
        }
        String q = query.toLowerCase().trim();
        return dataStore.getStudents().values().stream()
                .filter(s -> s.getRollNo().toLowerCase().contains(q) ||
                        s.getName().toLowerCase().contains(q) ||
                        s.getDepartment().toLowerCase().contains(q) ||
                        s.getBloodGroup().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    public synchronized Student updatePatient(Student s) {
        if (!dataStore.getStudents().containsKey(s.getRollNo())) {
            return null;
        }
        dataStore.getStudents().put(s.getRollNo(), s);
        dataStore.logActivity("ADMIN", "ADMIN", "PATIENT_UPDATED",
                "Updated details for patient: " + s.getName() + " (" + s.getRollNo() + ")");
        dataStore.saveAsync();
        return s;
    }
}
