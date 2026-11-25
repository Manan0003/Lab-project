package com.college.dispensary.handler;

import com.college.dispensary.model.Student;
import com.college.dispensary.service.DoctorService;
import com.college.dispensary.service.PatientService;
import com.college.dispensary.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PatientHandler implements HttpHandler {
    private final PatientService patientService = PatientService.getInstance();
    private final DoctorService doctorService = DoctorService.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpUtil.handleCors(exchange)) return;

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/api/patients".equals(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                Map<String, String> query = HttpUtil.parseQueryParams(exchange);
                String q = query.get("q");
                List<Student> list = patientService.searchPatients(q);
                HttpUtil.sendJsonResponse(exchange, 200, list);
            } else if ("POST".equalsIgnoreCase(method)) {
                handleRegister(exchange);
            } else {
                HttpUtil.sendError(exchange, 405, "Method not allowed");
            }
        } else if (path.startsWith("/api/patients/")) {
            String rollNo = path.substring("/api/patients/".length());
            if ("GET".equalsIgnoreCase(method)) {
                Student s = patientService.getPatient(rollNo);
                if (s == null) {
                    HttpUtil.sendError(exchange, 404, "Patient not found");
                } else {
                    // return comprehensive record including history
                    Map<String, Object> details = doctorService.getPatientConsultationDetails(rollNo);
                    HttpUtil.sendJsonResponse(exchange, 200, details != null ? details : s);
                }
            } else if ("PUT".equalsIgnoreCase(method)) {
                handleUpdate(exchange, rollNo);
            } else {
                HttpUtil.sendError(exchange, 405, "Method not allowed");
            }
        } else {
            HttpUtil.sendError(exchange, 404, "Endpoint not found");
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.readJsonBody(exchange);
        int age = body.get("age") instanceof Number n ? n.intValue() : 20;

        Student s = new Student(
                (String) body.get("rollNo"),
                (String) body.get("name"),
                (String) body.get("department"),
                (String) body.get("year"),
                (String) body.get("gender"),
                age,
                (String) body.get("bloodGroup"),
                (String) body.get("allergies"),
                (String) body.get("chronicConditions"),
                (String) body.get("emergencyContactName"),
                (String) body.get("emergencyPhone"),
                (String) body.get("phone"),
                (String) body.get("email")
        );

        try {
            Student registered = patientService.registerPatient(s);
            HttpUtil.sendSuccess(exchange, "Patient registered successfully", registered);
        } catch (Exception e) {
            HttpUtil.sendError(exchange, 400, e.getMessage());
        }
    }

    private void handleUpdate(HttpExchange exchange, String rollNo) throws IOException {
        Map<String, Object> body = HttpUtil.readJsonBody(exchange);
        Student existing = patientService.getPatient(rollNo);
        if (existing == null) {
            HttpUtil.sendError(exchange, 404, "Patient not found");
            return;
        }

        if (body.containsKey("name")) existing.setName((String) body.get("name"));
        if (body.containsKey("department")) existing.setDepartment((String) body.get("department"));
        if (body.containsKey("year")) existing.setYear((String) body.get("year"));
        if (body.containsKey("gender")) existing.setGender((String) body.get("gender"));
        if (body.containsKey("age") && body.get("age") instanceof Number n) existing.setAge(n.intValue());
        if (body.containsKey("bloodGroup")) existing.setBloodGroup((String) body.get("bloodGroup"));
        if (body.containsKey("allergies")) existing.setAllergies((String) body.get("allergies"));
        if (body.containsKey("chronicConditions")) existing.setChronicConditions((String) body.get("chronicConditions"));
        if (body.containsKey("emergencyContactName")) existing.setEmergencyContactName((String) body.get("emergencyContactName"));
        if (body.containsKey("emergencyPhone")) existing.setEmergencyPhone((String) body.get("emergencyPhone"));
        if (body.containsKey("phone")) existing.setPhone((String) body.get("phone"));
        if (body.containsKey("email")) existing.setEmail((String) body.get("email"));

        patientService.updatePatient(existing);
        HttpUtil.sendSuccess(exchange, "Patient details updated", existing);
    }
}
