package com.college.dispensary.service;

import com.college.dispensary.model.Appointment;
import com.college.dispensary.model.Doctor;
import com.college.dispensary.model.Student;
import com.college.dispensary.model.Token;
import com.college.dispensary.repository.DataStore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AppointmentService {
    private static final AppointmentService INSTANCE = new AppointmentService();
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DataStore dataStore = DataStore.getInstance();

    public static AppointmentService getInstance() {
        return INSTANCE;
    }

    private AppointmentService() {}

    public synchronized Appointment bookAppointment(String rollNo, String doctorId, String date, String timeSlot, String symptoms, String priority) {
        Student student = dataStore.getStudents().get(rollNo);
        String patientName = student != null ? student.getName() : "Student (" + rollNo + ")";
        String dept = student != null ? student.getDepartment() : "General";

        Doctor doctor = dataStore.getDoctors().get(doctorId);
        String doctorName = doctor != null ? doctor.getName() : "Campus Duty Doctor";

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
        int tokenNum = dataStore.nextTokenNumber() + 1; // get next token
        String nowStr = LocalDateTime.now().format(ISO_FMT);
        String prio = (priority == null || priority.isBlank()) ? "NORMAL" : priority.toUpperCase();

        Appointment apt = new Appointment(
                aptId, tokenNum, rollNo, patientName, dept, doctorId, doctorName,
                date, timeSlot, symptoms, prio, "Pending", nowStr
        );
        dataStore.getAppointments().put(aptId, apt);

        // Also create a linked Token for live queue
        Token token = TokenService.getInstance().createToken(rollNo, symptoms, prio);
        apt.setTokenNumber(token.getTokenNumber());

        dataStore.logActivity(rollNo, "PATIENT", "APPOINTMENT_REQUESTED",
                "Appointment " + aptId + " booked with " + doctorName + " for " + date + " [" + timeSlot + "]");
        dataStore.saveAsync();
        return apt;
    }

    public List<Appointment> getAllAppointments() {
        return new ArrayList<>(dataStore.getAppointments().values()).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<Appointment> getAppointmentsForPatient(String rollNo) {
        return dataStore.getAppointments().values().stream()
                .filter(a -> a.getPatientRollNo().equalsIgnoreCase(rollNo))
                .sorted((a, b) -> b.getAppointmentDate().compareTo(a.getAppointmentDate()))
                .collect(Collectors.toList());
    }

    public List<Appointment> getAppointmentsForDoctor(String doctorId) {
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
