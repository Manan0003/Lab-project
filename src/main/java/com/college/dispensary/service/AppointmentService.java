package com.college.dispensary.service;

import com.college.dispensary.model.Appointment;
import com.college.dispensary.model.Doctor;
import com.college.dispensary.model.Student;
import com.college.dispensary.model.Token;
import com.college.dispensary.repository.DataStore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AppointmentService {
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final AppointmentService INSTANCE = new AppointmentService();
    private final DataStore dataStore = DataStore.getInstance();

    public static AppointmentService getInstance() {
        return INSTANCE;
    }

    private AppointmentService() {}

    public synchronized Appointment bookAppointment(String rollNo, String doctorId, String date, String timeSlot, String symptoms, String priority) {
        autoExpireStaleAppointments();

        Student student = dataStore.getStudents().get(rollNo);
        String patientName = student != null ? student.getName() : "Student (" + rollNo + ")";
        String dept = student != null ? student.getDepartment() : "General";

        Doctor doctor = dataStore.getDoctors().get(doctorId);
        String doctorName = doctor != null ? doctor.getName() : "Campus Duty Doctor";

        // Parse slot start time to enforce 30-minute prior booking rule
        LocalDateTime slotStart = parseSlotDateTime(date, timeSlot);
        LocalDateTime now = LocalDateTime.now();

        if (slotStart.isBefore(now)) {
            throw new IllegalArgumentException("Cannot book an appointment for a past slot.");
        }

        if (slotStart.isBefore(now.plusMinutes(30))) {
            throw new IllegalArgumentException("Scheduled appointments must be booked at least 30 minutes prior to the slot time (" + timeSlot + "). For immediate medical assistance, please book a Walk-in Queue Token.");
        }

        // Check for double booking conflict
        for (Appointment a : dataStore.getAppointments().values()) {
            if ("Confirmed".equalsIgnoreCase(a.getStatus()) || "Pending".equalsIgnoreCase(a.getStatus())) {
                if (doctorId != null && doctorId.equals(a.getDoctorId()) &&
                        date.equals(a.getAppointmentDate()) &&
                        timeSlot.equalsIgnoreCase(a.getTimeSlot())) {
                    throw new IllegalStateException("Time slot " + timeSlot + " is already booked for " + doctorName);
                }
            }
        }

        String aptId = dataStore.nextAppointmentId();
        String nowStr = now.format(ISO_FMT);
        String prio = (priority == null || priority.isBlank()) ? "NORMAL" : priority.toUpperCase();

        // 30 mins after slot start time is the expiration threshold
        LocalDateTime expiresTime = slotStart.plusMinutes(30);
        String expiresAtStr = expiresTime.format(ISO_FMT);

        // Create linked Token with SCHEDULED bookingType and AWAITING_ARRIVAL status
        Token token = TokenService.getInstance().createToken(rollNo, symptoms, prio, "SCHEDULED", timeSlot, "STUDENT");
        token.setAppointmentId(aptId);
        token.setExpiresAt(expiresAtStr);

        Appointment apt = new Appointment(
                aptId, token.getTokenNumber(), rollNo, patientName, dept, doctorId, doctorName,
                date, timeSlot, symptoms, prio, "Pending", nowStr
        );
        apt.setTokenId(token.getId());
        apt.setExpiresAt(expiresAtStr);

        dataStore.getAppointments().put(aptId, apt);
        dataStore.logActivity(rollNo, "PATIENT", "APPOINTMENT_REQUESTED",
                "Appointment " + aptId + " (Token #" + token.getTokenNumber() + ") booked with " + doctorName + " for " + date + " [" + timeSlot + "]. Must arrive before " + expiresAtStr);
        dataStore.saveAsync();
        return apt;
    }

    public synchronized void autoExpireStaleAppointments() {
        LocalDateTime now = LocalDateTime.now();
        for (Appointment a : dataStore.getAppointments().values()) {
            if ("Pending".equalsIgnoreCase(a.getStatus()) && a.getExpiresAt() != null) {
                try {
                    LocalDateTime exp = LocalDateTime.parse(a.getExpiresAt(), ISO_FMT);
                    if (now.isAfter(exp)) {
                        a.setStatus("Expired");
                        dataStore.logActivity(a.getPatientRollNo(), "SYSTEM", "APPOINTMENT_EXPIRED",
                                "Appointment " + a.getId() + " expired: patient did not check in within 30 minutes of scheduled slot.");
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private LocalDateTime parseSlotDateTime(String dateStr, String timeSlot) {
        LocalDate d = LocalDate.parse(dateStr.trim());
        String rawStart = timeSlot.contains("-") ? timeSlot.split("-")[0].trim() : timeSlot.trim();
        LocalTime t;
        try {
            t = LocalTime.parse(rawStart.toUpperCase(), TIME_FMT);
        } catch (Exception e) {
            // fallback simple hour parsing
            try {
                t = LocalTime.parse(rawStart);
            } catch (Exception e2) {
                t = LocalTime.of(10, 0);
            }
        }
        return LocalDateTime.of(d, t);
    }

    public List<Appointment> getAllAppointments() {
        autoExpireStaleAppointments();
        return new ArrayList<>(dataStore.getAppointments().values()).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<Appointment> getAppointmentsForPatient(String rollNo) {
        autoExpireStaleAppointments();
        return dataStore.getAppointments().values().stream()
                .filter(a -> a.getPatientRollNo().equalsIgnoreCase(rollNo))
                .sorted((a, b) -> b.getAppointmentDate().compareTo(a.getAppointmentDate()))
                .collect(Collectors.toList());
    }

    public List<Appointment> getAppointmentsForDoctor(String doctorId) {
        autoExpireStaleAppointments();
        return dataStore.getAppointments().values().stream()
                .filter(a -> doctorId == null || doctorId.equals(a.getDoctorId()))
                .sorted((a, b) -> b.getAppointmentDate().compareTo(a.getAppointmentDate()))
                .collect(Collectors.toList());
    }

    public synchronized boolean updateAppointmentStatus(String aptId, String status) {
        Appointment apt = dataStore.getAppointments().get(aptId);
        if (apt != null) {
            apt.setStatus(status);
            dataStore.logActivity("DISPENSARY", "STAFF", "APPOINTMENT_STATUS_UPDATE",
                    "Appointment " + aptId + " marked as " + status);
            dataStore.saveAsync();
            return true;
        }
        return false;
    }
}
