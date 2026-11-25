package com.college.dispensary.handler;

import com.college.dispensary.model.Appointment;
import com.college.dispensary.service.AppointmentService;
import com.college.dispensary.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AppointmentHandler implements HttpHandler {
    private final AppointmentService appointmentService = AppointmentService.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpUtil.handleCors(exchange)) return;

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/api/appointments".equals(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                Map<String, String> params = HttpUtil.parseQueryParams(exchange);
                String rollNo = params.get("rollNo");
                String doctorId = params.get("doctorId");

                List<Appointment> list;
                if (rollNo != null && !rollNo.isBlank()) {
                    list = appointmentService.getAppointmentsForPatient(rollNo);
                } else if (doctorId != null && !doctorId.isBlank()) {
                    list = appointmentService.getAppointmentsForDoctor(doctorId);
                } else {
                    list = appointmentService.getAllAppointments();
                }
                HttpUtil.sendJsonResponse(exchange, 200, list);
            } else if ("POST".equalsIgnoreCase(method)) {
                handleBookAppointment(exchange);
            } else {
                HttpUtil.sendError(exchange, 405, "Method not allowed");
            }
        } else if ("/api/appointments/status".equals(path) && "PUT".equalsIgnoreCase(method)) {
            Map<String, Object> body = HttpUtil.readJsonBody(exchange);
            String id = (String) body.get("id");
            String status = (String) body.get("status");

            boolean updated = appointmentService.updateAppointmentStatus(id, status);
            if (updated) {
                HttpUtil.sendSuccess(exchange, "Appointment status updated to " + status, null);
            } else {
                HttpUtil.sendError(exchange, 404, "Appointment not found");
            }
        } else {
            HttpUtil.sendError(exchange, 404, "Endpoint not found");
        }
    }

    private void handleBookAppointment(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.readJsonBody(exchange);
        String rollNo = (String) body.get("patientRollNo");
        String doctorId = (String) body.get("doctorId");
        String date = (String) body.get("appointmentDate");
        String timeSlot = (String) body.get("timeSlot");
        String symptoms = (String) body.get("symptoms");
        String priority = (String) body.get("priority");

        try {
            Appointment apt = appointmentService.bookAppointment(rollNo, doctorId, date, timeSlot, symptoms, priority);
            HttpUtil.sendSuccess(exchange, "Appointment booked successfully. Token #" + apt.getTokenNumber() + " generated.", apt);
        } catch (IllegalStateException e) {
            HttpUtil.sendError(exchange, 409, e.getMessage());
        } catch (Exception e) {
            HttpUtil.sendError(exchange, 400, "Error booking appointment: " + e.getMessage());
        }
    }
}
