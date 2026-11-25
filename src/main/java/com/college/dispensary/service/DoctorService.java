package com.college.dispensary.service;

import com.college.dispensary.model.Prescription;
import com.college.dispensary.model.Student;
import com.college.dispensary.model.Token;
import com.college.dispensary.repository.DataStore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DoctorService {
    private static final DoctorService INSTANCE = new DoctorService();
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DataStore dataStore = DataStore.getInstance();

    public static DoctorService getInstance() {
        return INSTANCE;
    }

    private DoctorService() {}

    public Map<String, Object> getPatientConsultationDetails(String rollNo) {
        Map<String, Object> details = new LinkedHashMap<>();
        Student student = dataStore.getStudents().get(rollNo);
        if (student == null) {
            return null;
        }

        details.put("student", student);

        // Fetch past prescriptions for this student
        List<Prescription> pastHistory = dataStore.getPrescriptions().values().stream()
                .filter(p -> p.getStudentRollNo().equalsIgnoreCase(rollNo))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList());

        details.put("pastPrescriptions", pastHistory);

        // Fetch any currently active token
        Token activeToken = TokenService.getInstance().getStudentActiveToken(rollNo);
        details.put("activeToken", activeToken);

        return details;
    }

    public synchronized Prescription completeConsultation(Prescription rx) {
        String nowStr = LocalDateTime.now().format(ISO_FMT);
        rx.setId(dataStore.nextPrescriptionId());
        rx.setDate(nowStr);
        rx.setStatus("PENDING_DISPENSE");

        // Mark corresponding token as COMPLETED
        if (rx.getTokenId() != null && !rx.getTokenId().isBlank()) {
            Token token = dataStore.getTokens().get(rx.getTokenId());
            if (token != null) {
                token.setStatus("COMPLETED");
                token.setCompletedAt(nowStr);
            }
        }

        dataStore.getPrescriptions().put(rx.getId(), rx);
        dataStore.logActivity(rx.getDoctorName(), "DOCTOR", "CONSULTATION_COMPLETED",
                "Completed consultation for " + rx.getStudentName() + " (" + rx.getStudentRollNo() + "). Rx: " + rx.getId() + " - Diagnosis: " + rx.getDiagnosis());

        dataStore.saveAsync();
        return rx;
    }

    public List<Prescription> getAllPrescriptions() {
        return new ArrayList<>(dataStore.getPrescriptions().values()).stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList());
    }

    public Prescription getPrescriptionById(String rxId) {
        return dataStore.getPrescriptions().get(rxId);
    }
}
