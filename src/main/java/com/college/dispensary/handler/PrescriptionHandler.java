package com.college.dispensary.handler;

import com.college.dispensary.model.Prescription;
import com.college.dispensary.repository.DataStore;
import com.college.dispensary.service.DoctorService;
import com.college.dispensary.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PrescriptionHandler implements HttpHandler {
    private final DoctorService doctorService = DoctorService.getInstance();
    private final DataStore dataStore = DataStore.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpUtil.handleCors(exchange)) return;

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/api/prescriptions".equals(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                Map<String, String> params = HttpUtil.parseQueryParams(exchange);
                String rollNo = params.get("rollNo");
                List<Prescription> list;
                if (rollNo != null && !rollNo.isBlank()) {
                    list = dataStore.getPrescriptions().values().stream()
                            .filter(p -> p.getStudentRollNo().equalsIgnoreCase(rollNo))
                            .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                            .collect(Collectors.toList());
                } else {
                    list = doctorService.getAllPrescriptions();
                }
                HttpUtil.sendJsonResponse(exchange, 200, list);
            } else {
                HttpUtil.sendError(exchange, 405, "Method not allowed");
            }
        } else if (path.startsWith("/api/prescriptions/")) {
            String id = path.substring("/api/prescriptions/".length());
            if ("GET".equalsIgnoreCase(method)) {
                Prescription p = doctorService.getPrescriptionById(id);
                if (p == null) {
                    HttpUtil.sendError(exchange, 404, "Prescription not found");
                } else {
                    HttpUtil.sendJsonResponse(exchange, 200, p);
                }
            } else {
                HttpUtil.sendError(exchange, 405, "Method not allowed");
            }
        } else {
            HttpUtil.sendError(exchange, 404, "Endpoint not found");
        }
    }
}
