package com.college.dispensary.handler;

import com.college.dispensary.model.Doctor;
import com.college.dispensary.model.PrescribedItem;
import com.college.dispensary.model.Prescription;
import com.college.dispensary.model.Token;
import com.college.dispensary.repository.DataStore;
import com.college.dispensary.service.DoctorService;
import com.college.dispensary.service.TokenService;
import com.college.dispensary.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.*;

public class DoctorHandler implements HttpHandler {
    private final DoctorService doctorService = DoctorService.getInstance();
    private final TokenService tokenService = TokenService.getInstance();
    private final DataStore dataStore = DataStore.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpUtil.handleCors(exchange)) return;

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/api/doctors".equals(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                List<Doctor> list = new ArrayList<>(dataStore.getDoctors().values());
                HttpUtil.sendJsonResponse(exchange, 200, list);
            } else if ("POST".equalsIgnoreCase(method)) {
                handleAddDoctor(exchange);
            } else {
                HttpUtil.sendError(exchange, 405, "Method not allowed");
            }
        } else if ("/api/doctor/patient-details".equals(path) && "GET".equalsIgnoreCase(method)) {
            Map<String, String> params = HttpUtil.parseQueryParams(exchange);
            String rollNo = params.get("rollNo");
            if (rollNo == null || rollNo.isBlank()) {
                HttpUtil.sendError(exchange, 400, "rollNo parameter is required");
                return;
            }
            Map<String, Object> details = doctorService.getPatientConsultationDetails(rollNo);
            if (details == null) {
                HttpUtil.sendError(exchange, 404, "Patient not found");
                return;
            }
            HttpUtil.sendJsonResponse(exchange, 200, details);
        } else if ("/api/doctor/call-token".equals(path) && "POST".equalsIgnoreCase(method)) {
            Map<String, Object> body = HttpUtil.readJsonBody(exchange);
            String doctorName = (String) body.get("doctorName");
            String tokenId = (String) body.get("tokenId");

            Token called;
            if (tokenId != null && !tokenId.isBlank()) {
                called = tokenService.callSpecificToken(tokenId, doctorName);
            } else {
                called = tokenService.callNextToken(doctorName);
            }

            if (called == null) {
                HttpUtil.sendError(exchange, 400, "No waiting patients in queue");
                return;
            }
            HttpUtil.sendSuccess(exchange, "Patient called to chamber", called);
        } else if (("/api/doctor/consultations".equals(path) || "/api/consultations".equals(path)) && "POST".equalsIgnoreCase(method)) {
            handleConsultation(exchange);
        } else {
            HttpUtil.sendError(exchange, 404, "Endpoint not found");
        }
    }

    private void handleAddDoctor(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.readJsonBody(exchange);
        String id = (String) body.get("id");
        if (id == null || id.isBlank()) {
            id = "DOC-" + String.format("%03d", dataStore.getDoctors().size() + 1);
        }

        Doctor d = new Doctor(
                id,
                (String) body.get("name"),
                (String) body.get("specialization"),
                (String) body.get("qualification"),
                (String) body.get("chamber"),
                (String) body.get("contactNumber"),
                (String) body.get("email"),
                (String) body.get("availableDays"),
                (String) body.get("availableHours"),
                (String) body.get("status")
        );
        dataStore.getDoctors().put(d.getId(), d);
        dataStore.saveAsync();
        HttpUtil.sendSuccess(exchange, "Doctor added successfully", d);
    }

    private void handleConsultation(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.readJsonBody(exchange);

        Prescription rx = new Prescription();
        rx.setTokenId((String) body.get("tokenId"));
        rx.setStudentRollNo((String) body.get("studentRollNo"));
        rx.setStudentName((String) body.get("studentName"));
        rx.setDoctorName((String) body.get("doctorName"));
        rx.setBloodPressure((String) body.get("bloodPressure"));
        rx.setTemperature((String) body.get("temperature"));
        rx.setPulse((String) body.get("pulse"));
        rx.setWeight((String) body.get("weight"));
        rx.setSymptoms((String) body.get("symptoms"));
        rx.setDiagnosis((String) body.get("diagnosis"));
        rx.setClinicalNotes((String) body.get("clinicalNotes"));

        // Sick leave details
        if (body.get("sickLeaveIssued") instanceof Boolean b && b) {
            rx.setSickLeaveIssued(true);
            if (body.get("sickLeaveDays") instanceof Number n) rx.setSickLeaveDays(n.intValue());
            rx.setSickLeaveFrom((String) body.get("sickLeaveFrom"));
            rx.setSickLeaveTo((String) body.get("sickLeaveTo"));
            rx.setSickLeaveAdvice((String) body.get("sickLeaveAdvice"));
        }

        // Parse items
        if (body.get("items") instanceof List<?> rawList) {
            List<PrescribedItem> items = new ArrayList<>();
            for (Object obj : rawList) {
                if (obj instanceof Map<?, ?> m) {
                    int qty = m.get("quantity") instanceof Number num ? num.intValue() : 1;
                    items.add(new PrescribedItem(
                            (String) m.get("medicineId"),
                            (String) m.get("medicineName"),
                            (String) m.get("dosage"),
                            (String) m.get("duration"),
                            qty,
                            (String) m.get("instructions"),
                            false
                    ));
                }
            }
            rx.setItems(items);
        }

        Prescription saved = doctorService.completeConsultation(rx);
        HttpUtil.sendSuccess(exchange, "Consultation completed & prescription sent to Pharmacy", saved);
    }
}
