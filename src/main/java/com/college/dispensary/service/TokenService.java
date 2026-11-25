package com.college.dispensary.service;

import com.college.dispensary.model.Appointment;
import com.college.dispensary.model.Student;
import com.college.dispensary.model.Token;
import com.college.dispensary.repository.DataStore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TokenService {
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final TokenService INSTANCE = new TokenService();
    private final DataStore dataStore = DataStore.getInstance();

    public static TokenService getInstance() {
        return INSTANCE;
    }

    private TokenService() {}

    /**
     * Book walk-in token online (by student) or at desk (by reception/admin).
     */
    public synchronized Token createToken(String studentRollNo, String symptoms, String priority) {
        return createToken(studentRollNo, symptoms, priority, "WALK_IN", null, "STUDENT");
    }

    public synchronized Token createToken(String studentRollNo, String symptoms, String priority,
                                          String bookingType, String scheduledSlot, String requestedByRole) {
        autoExpireStaleTokens();

        Student student = dataStore.getStudents().get(studentRollNo);
        String studentName = student != null ? student.getName() : "Student (" + studentRollNo + ")";
        String dept = student != null ? student.getDepartment() : "General";

        // Check if student already has an active token (AWAITING_ARRIVAL, WAITING or IN_CONSULTATION)
        for (Token t : dataStore.getTokens().values()) {
            if (t.getStudentRollNo().equalsIgnoreCase(studentRollNo) &&
                    ("WAITING".equals(t.getStatus()) || "IN_CONSULTATION".equals(t.getStatus()) || "AWAITING_ARRIVAL".equals(t.getStatus()))) {
                return t; // return existing active token
            }
        }

        String id = dataStore.nextTokenId();
        int num = dataStore.nextTokenNumber();
        String nowStr = LocalDateTime.now().format(ISO_FMT);
        String prio = (priority == null || priority.isBlank()) ? "NORMAL" : priority.toUpperCase();
        String type = (bookingType == null || bookingType.isBlank()) ? "WALK_IN" : bookingType;

        // If booked at physical front desk by Reception/Admin, student is already present
        boolean atDesk = "ADMIN".equalsIgnoreCase(requestedByRole) || "RECEPTIONIST".equalsIgnoreCase(requestedByRole);
        String initialStatus = atDesk ? "WAITING" : "AWAITING_ARRIVAL";

        Token token = new Token(id, num, studentRollNo, studentName, dept, symptoms, prio, initialStatus, nowStr);
        token.setBookingType(type);

        if (atDesk) {
            token.setConfirmedAt(nowStr);
            token.setConfirmedBy("Front-Desk Staff");
            token.setRunningSlot(calculateBestAvailableSlot());
        } else {
            if ("SCHEDULED".equalsIgnoreCase(type) && scheduledSlot != null && !scheduledSlot.isBlank()) {
                // Booked token: give slot according to preferred time
                token.setRunningSlot(scheduledSlot);
            } else {
                // Walk-in token: allocate best slot available
                token.setRunningSlot(calculateBestAvailableSlot());
            }
        }

        dataStore.getTokens().put(id, token);
        dataStore.logActivity(studentRollNo, "STUDENT", "TOKEN_BOOKED",
                "Token " + id + " (" + type + ") created for " + studentName + " with slot " + token.getRunningSlot() + ". Status: " + initialStatus);
        dataStore.saveAsync();
        return token;
    }

    /**
     * Confirm physical arrival of a student at the dispensary reception desk.
     */
    public synchronized Token confirmArrival(String tokenId, String confirmedBy) {
        autoExpireStaleTokens();

        Token token = dataStore.getTokens().get(tokenId);
        if (token == null) return null;

        if ("EXPIRED".equals(token.getStatus())) {
            throw new IllegalStateException("Token #" + token.getTokenNumber() + " has expired. Please book a new token.");
        }

        String nowStr = LocalDateTime.now().format(ISO_FMT);
        token.setStatus("WAITING");
        token.setConfirmedAt(nowStr);
        token.setConfirmedBy(confirmedBy != null ? confirmedBy : "Dispensary Staff");

        // For SCHEDULED appointments, keep their preferred booked slot
        // For WALK_IN tokens, ensure assigned best available slot
        if (!"SCHEDULED".equalsIgnoreCase(token.getBookingType())) {
            if (token.getRunningSlot() == null || token.getRunningSlot().contains("check-in") || token.getRunningSlot().contains("Running")) {
                token.setRunningSlot(calculateBestAvailableSlot());
            }
        }

        // Also check if linked appointment exists
        for (Appointment a : dataStore.getAppointments().values()) {
            if (token.getId().equals(a.getTokenId()) || token.getTokenNumber() == a.getTokenNumber() ||
                    (token.getStudentRollNo().equalsIgnoreCase(a.getPatientRollNo()) && "Pending".equalsIgnoreCase(a.getStatus()))) {
                a.setStatus("Confirmed");
                a.setConfirmedAt(nowStr);
                a.setConfirmedBy(token.getConfirmedBy());
            }
        }

        dataStore.logActivity(confirmedBy != null ? confirmedBy : "DISPENSARY", "STAFF", "ARRIVAL_CONFIRMED",
                "Confirmed arrival for Token #" + token.getTokenNumber() + " (" + token.getStudentName() + ") with slot " + token.getRunningSlot() + ". Admitted to doctor queue.");
        dataStore.saveAsync();
        return token;
    }

    /**
     * Dynamically finds the earliest best available 15-minute slot for walk-ins,
     * skipping all currently occupied or scheduled slots.
     */
    public synchronized String calculateBestAvailableSlot() {
        Set<LocalTime> occupiedStarts = new HashSet<>();

        // Collect slots occupied by active tokens
        for (Token t : dataStore.getTokens().values()) {
            if ("WAITING".equals(t.getStatus()) || "IN_CONSULTATION".equals(t.getStatus()) || "AWAITING_ARRIVAL".equals(t.getStatus())) {
                LocalTime st = parseSlotStart(t.getRunningSlot());
                if (st != null && !st.equals(LocalTime.of(23, 59))) {
                    occupiedStarts.add(st);
                }
            }
        }

        // Collect slots occupied by today's appointments
        for (Appointment a : dataStore.getAppointments().values()) {
            if ("Confirmed".equalsIgnoreCase(a.getStatus()) || "Pending".equalsIgnoreCase(a.getStatus())) {
                LocalTime st = parseSlotStart(a.getTimeSlot());
                if (st != null && !st.equals(LocalTime.of(23, 59))) {
                    occupiedStarts.add(st);
                }
            }
        }

        LocalTime now = LocalTime.now();
        LocalTime candidate;

        // If currently during active dispensary hours (09:00 - 17:00), start from next 15-min boundary
        if (now.isAfter(LocalTime.of(8, 59)) && now.isBefore(LocalTime.of(17, 0))) {
            int minute = now.getMinute();
            int rounded = ((minute / 15) + 1) * 15;
            candidate = now.withMinute(0).withSecond(0).plusMinutes(rounded);
        } else {
            // For testing/simulation, baseline dispensary opening schedule starts at 09:30 AM
            candidate = LocalTime.of(9, 30);
        }

        // Scan in 15-minute increments for the earliest free slot
        for (int i = 0; i < 96; i++) {
            if (!occupiedStarts.contains(candidate)) {
                LocalTime end = candidate.plusMinutes(15);
                return candidate.format(TIME_FMT) + " - " + end.format(TIME_FMT);
            }
            candidate = candidate.plusMinutes(15);
        }

        LocalTime end = candidate.plusMinutes(15);
        return candidate.format(TIME_FMT) + " - " + end.format(TIME_FMT);
    }

    /**
     * Parses the start time from any slot string representation (e.g. "09:30 AM - 09:45 AM" -> 09:30).
     */
    public static LocalTime parseSlotStart(String slotStr) {
        if (slotStr == null || slotStr.isBlank() ||
                slotStr.toLowerCase().contains("check-in") ||
                slotStr.toLowerCase().contains("running") ||
                slotStr.toLowerCase().contains("assigned")) {
            return LocalTime.of(23, 59);
        }
        try {
            String raw = slotStr.contains("-") ? slotStr.split("-")[0].trim() : slotStr.trim();
            if (raw.length() == 7) {
                raw = "0" + raw; // e.g. "9:30 AM" -> "09:30 AM"
            }
            return LocalTime.parse(raw.toUpperCase(Locale.ENGLISH), DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));
        } catch (Exception e) {
            try {
                // Try alternate formats
                String raw = slotStr.contains("-") ? slotStr.split("-")[0].trim() : slotStr.trim();
                return LocalTime.parse(raw, DateTimeFormatter.ofPattern("HH:mm"));
            } catch (Exception ex) {
                return LocalTime.of(23, 59);
            }
        }
    }

    /**
     * Orders tokens strictly by triage priority and assigned slot time, NOT token number!
     */
    private int compareTokensBySlotAndTime(Token a, Token b) {
        // 1. Triage priority: EMERGENCY (3) > URGENT (2) > NORMAL (1)
        int prioA = getPriorityWeight(a.getPriority());
        int prioB = getPriorityWeight(b.getPriority());
        if (prioA != prioB) {
            return Integer.compare(prioB, prioA);
        }

        // 2. Chronological slot start time (earliest assigned slot first)
        LocalTime slotA = parseSlotStart(a.getRunningSlot());
        LocalTime slotB = parseSlotStart(b.getRunningSlot());
        int slotComp = slotA.compareTo(slotB);
        if (slotComp != 0) {
            return slotComp;
        }

        // 3. Same slot tie-breaker: first arrived/confirmed gets priority
        String timeA = a.getConfirmedAt() != null ? a.getConfirmedAt() : a.getCreatedAt();
        String timeB = b.getConfirmedAt() != null ? b.getConfirmedAt() : b.getCreatedAt();
        if (timeA != null && timeB != null) {
            int timeComp = timeA.compareTo(timeB);
            if (timeComp != 0) return timeComp;
        }

        return a.getId().compareTo(b.getId());
    }

    /**
     * Checks scheduled tokens and auto-expires them if 30 mins past slot without check-in.
     */
    public synchronized void autoExpireStaleTokens() {
        LocalDateTime now = LocalDateTime.now();
        for (Token t : dataStore.getTokens().values()) {
            if ("AWAITING_ARRIVAL".equals(t.getStatus()) && t.getExpiresAt() != null) {
                try {
                    LocalDateTime exp = LocalDateTime.parse(t.getExpiresAt(), ISO_FMT);
                    if (now.isAfter(exp)) {
                        t.setStatus("EXPIRED");
                        dataStore.logActivity(t.getStudentRollNo(), "SYSTEM", "TOKEN_EXPIRED",
                                "Scheduled Token #" + t.getTokenNumber() + " expired: check-in was not completed within 30 mins of slot.");
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    public List<Token> getAllTokens() {
        autoExpireStaleTokens();
        return new ArrayList<>(dataStore.getTokens().values()).stream()
                .sorted(this::compareTokensBySlotAndTime)
                .collect(Collectors.toList());
    }

    public List<Token> getWaitingQueue() {
        autoExpireStaleTokens();
        return dataStore.getTokens().values().stream()
                .filter(t -> "WAITING".equals(t.getStatus()))
                .sorted(this::compareTokensBySlotAndTime)
                .collect(Collectors.toList());
    }

    public List<Token> getAwaitingArrivalList() {
        autoExpireStaleTokens();
        return dataStore.getTokens().values().stream()
                .filter(t -> "AWAITING_ARRIVAL".equals(t.getStatus()))
                .sorted(this::compareTokensBySlotAndTime)
                .collect(Collectors.toList());
    }

    public Token getCurrentConsultationToken() {
        return dataStore.getTokens().values().stream()
                .filter(t -> "IN_CONSULTATION".equals(t.getStatus()))
                .findFirst()
                .orElse(null);
    }

    public Token getStudentActiveToken(String studentRollNo) {
        autoExpireStaleTokens();
        return dataStore.getTokens().values().stream()
                .filter(t -> t.getStudentRollNo().equalsIgnoreCase(studentRollNo) &&
                        ("WAITING".equals(t.getStatus()) || "IN_CONSULTATION".equals(t.getStatus()) || "AWAITING_ARRIVAL".equals(t.getStatus())))
                .findFirst()
                .orElse(null);
    }

    /**
     * Builds full visible queue of at least 10+ upcoming tokens with running slots.
     */
    public List<Map<String, Object>> getFullQueueBoard() {
        autoExpireStaleTokens();
        List<Map<String, Object>> board = new ArrayList<>();

        // 1. Current in consultation
        Token current = getCurrentConsultationToken();
        if (current != null) {
            Map<String, Object> map = tokenToBoardMap(current, 1, true);
            board.add(map);
        }

        // 2. Confirmed waiting queue
        List<Token> waiting = getWaitingQueue();
        int pos = board.size() + 1;
        for (Token t : waiting) {
            board.add(tokenToBoardMap(t, pos++, false));
        }

        // 3. Awaiting arrival tokens
        List<Token> awaiting = getAwaitingArrivalList();
        for (Token t : awaiting) {
            board.add(tokenToBoardMap(t, pos++, false));
        }

        return board;
    }

    private Map<String, Object> tokenToBoardMap(Token t, int position, boolean isCurrent) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("position", position);
        m.put("id", t.getId());
        m.put("tokenNumber", t.getTokenNumber());
        m.put("studentRollNo", t.getStudentRollNo());
        m.put("studentName", t.getStudentName());
        m.put("department", t.getDepartment());
        m.put("symptoms", t.getSymptoms());
        m.put("priority", t.getPriority());
        m.put("status", t.getStatus());
        m.put("bookingType", t.getBookingType());
        m.put("runningSlot", t.getRunningSlot() != null ? t.getRunningSlot() : "Running Slot");
        m.put("confirmedAt", t.getConfirmedAt());
        m.put("isCurrent", isCurrent);
        return m;
    }

    public synchronized Token callNextToken(String doctorName) {
        autoExpireStaleTokens();
        String nowStr = LocalDateTime.now().format(ISO_FMT);

        List<Token> waiting = getWaitingQueue();
        if (waiting.isEmpty()) {
            return null;
        }

        Token next = waiting.get(0);
        next.setStatus("IN_CONSULTATION");
        next.setCalledAt(nowStr);
        next.setAssignedDoctor(doctorName != null ? doctorName : "Campus Duty Doctor");

        dataStore.logActivity(doctorName != null ? doctorName : "DOCTOR", "DOCTOR", "TOKEN_CALLED",
                "Calling Token #" + next.getTokenNumber() + " - " + next.getStudentName() + " (" + next.getStudentRollNo() + ") into chamber.");
        dataStore.saveAsync();
        return next;
    }

    public synchronized Token callSpecificToken(String tokenId, String doctorName) {
        autoExpireStaleTokens();
        Token token = dataStore.getTokens().get(tokenId);
        if (token == null) return null;

        String nowStr = LocalDateTime.now().format(ISO_FMT);
        token.setStatus("IN_CONSULTATION");
        token.setCalledAt(nowStr);
        token.setAssignedDoctor(doctorName != null ? doctorName : "Campus Duty Doctor");

        dataStore.logActivity(doctorName != null ? doctorName : "DOCTOR", "DOCTOR", "TOKEN_CALLED",
                "Calling Token #" + token.getTokenNumber() + " - " + token.getStudentName() + " (" + token.getStudentRollNo() + ") into chamber.");
        dataStore.saveAsync();
        return token;
    }

    public synchronized boolean cancelToken(String tokenId, String reason) {
        Token t = dataStore.getTokens().get(tokenId);
        if (t != null) {
            t.setStatus("CANCELLED");
            dataStore.logActivity(t.getStudentRollNo(), "STUDENT", "TOKEN_CANCELLED",
                    "Token #" + t.getTokenNumber() + " cancelled. Reason: " + reason);
            dataStore.saveAsync();
            return true;
        }
        return false;
    }

    private int getPriorityWeight(String prio) {
        if ("EMERGENCY".equalsIgnoreCase(prio)) return 3;
        if ("URGENT".equalsIgnoreCase(prio)) return 2;
        return 1;
    }
}
