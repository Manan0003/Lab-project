package com.college.dispensary.service;

import com.college.dispensary.model.Student;
import com.college.dispensary.model.Token;
import com.college.dispensary.repository.DataStore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TokenService {
    private static final TokenService INSTANCE = new TokenService();
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DataStore dataStore = DataStore.getInstance();

    public static TokenService getInstance() {
        return INSTANCE;
    }

    private TokenService() {}

    public synchronized Token createToken(String studentRollNo, String symptoms, String priority) {
        Student student = dataStore.getStudents().get(studentRollNo);
        String studentName = student != null ? student.getName() : "Student (" + studentRollNo + ")";
        String dept = student != null ? student.getDepartment() : "General";

        // Check if student already has an active token (WAITING or IN_CONSULTATION)
        for (Token t : dataStore.getTokens().values()) {
            if (t.getStudentRollNo().equalsIgnoreCase(studentRollNo) &&
                    ("WAITING".equals(t.getStatus()) || "IN_CONSULTATION".equals(t.getStatus()))) {
                return t; // return existing active token
            }
        }

        String id = dataStore.nextTokenId();
        int num = dataStore.nextTokenNumber();
        String nowStr = LocalDateTime.now().format(ISO_FMT);
        String prio = (priority == null || priority.isBlank()) ? "NORMAL" : priority.toUpperCase();

        Token token = new Token(id, num, studentRollNo, studentName, dept, symptoms, prio, "WAITING", nowStr);
        dataStore.getTokens().put(id, token);
        dataStore.logActivity(studentRollNo, "STUDENT", "TOKEN_BOOKED", "Token " + id + " booked for " + studentName + ". Symptoms: " + symptoms);
        dataStore.saveAsync();
        return token;
    }

    public List<Token> getAllTokens() {
        return new ArrayList<>(dataStore.getTokens().values()).stream()
                .sorted(Comparator.comparingInt(Token::getTokenNumber).reversed())
                .collect(Collectors.toList());
    }

    public List<Token> getWaitingQueue() {
        return dataStore.getTokens().values().stream()
                .filter(t -> "WAITING".equals(t.getStatus()))
                .sorted((a, b) -> {
                    // Urgent / Emergency comes first, then by token number
                    int prioA = getPriorityWeight(a.getPriority());
                    int prioB = getPriorityWeight(b.getPriority());
                    if (prioA != prioB) return Integer.compare(prioB, prioA);
                    return Integer.compare(a.getTokenNumber(), b.getTokenNumber());
                })
                .collect(Collectors.toList());
    }

    public Token getCurrentConsultationToken() {
        return dataStore.getTokens().values().stream()
                .filter(t -> "IN_CONSULTATION".equals(t.getStatus()))
                .findFirst()
                .orElse(null);
    }

    public Token getStudentActiveToken(String studentRollNo) {
        return dataStore.getTokens().values().stream()
                .filter(t -> t.getStudentRollNo().equalsIgnoreCase(studentRollNo) &&
                        ("WAITING".equals(t.getStatus()) || "IN_CONSULTATION".equals(t.getStatus())))
                .findFirst()
                .orElse(null);
    }

    public synchronized Token callNextToken(String doctorName) {
        String nowStr = LocalDateTime.now().format(ISO_FMT);

        // If there's an existing token in consultation, keep it or mark completed if doctor called next
        List<Token> waiting = getWaitingQueue();
        if (waiting.isEmpty()) {
            return null;
        }

        Token next = waiting.get(0);
        next.setStatus("IN_CONSULTATION");
        next.setCalledAt(nowStr);
        next.setAssignedDoctor(doctorName != null ? doctorName : "Campus Duty Doctor");

        dataStore.logActivity(doctorName != null ? doctorName : "DOCTOR", "DOCTOR", "TOKEN_CALLED",
                "Calling Token " + next.getId() + " - " + next.getStudentName() + " (" + next.getStudentRollNo() + ") into chamber.");
        dataStore.saveAsync();
        return next;
    }

    public synchronized Token callSpecificToken(String tokenId, String doctorName) {
        Token token = dataStore.getTokens().get(tokenId);
        if (token == null) return null;

        String nowStr = LocalDateTime.now().format(ISO_FMT);
        token.setStatus("IN_CONSULTATION");
        token.setCalledAt(nowStr);
        token.setAssignedDoctor(doctorName != null ? doctorName : "Campus Duty Doctor");

        dataStore.logActivity(doctorName != null ? doctorName : "DOCTOR", "DOCTOR", "TOKEN_CALLED",
                "Calling Token " + token.getId() + " - " + token.getStudentName() + " (" + token.getStudentRollNo() + ") into chamber.");
        dataStore.saveAsync();
        return token;
    }

    public synchronized boolean cancelToken(String tokenId, String reason) {
        Token t = dataStore.getTokens().get(tokenId);
        if (t != null) {
            t.setStatus("CANCELLED");
            dataStore.logActivity(t.getStudentRollNo(), "STUDENT", "TOKEN_CANCELLED", "Token " + tokenId + " cancelled. Reason: " + reason);
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
