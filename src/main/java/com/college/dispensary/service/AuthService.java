package com.college.dispensary.service;

import com.college.dispensary.model.Doctor;
import com.college.dispensary.model.Student;
import com.college.dispensary.model.User;
import com.college.dispensary.repository.DataStore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final AuthService INSTANCE = new AuthService();
    private final DataStore dataStore = DataStore.getInstance();
    private final Map<String, User> sessions = new ConcurrentHashMap<>();

    public static AuthService getInstance() {
        return INSTANCE;
    }

    private AuthService() {}

    public User authenticate(String username, String password) {
        if (username == null || password == null) return null;
        User user = dataStore.getUsers().get(username.trim());
        if (user != null && user.getPassword().equals(password.trim())) {
            return user;
        }
        return null;
    }

    public synchronized User registerUser(String username, String password, String fullName, String role,
                                          String email, String phone, String department, String year,
                                          String bloodGroup, String allergies, String chronic) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username/ID and Password are required.");
        }
        username = username.trim();
        if (dataStore.getUsers().containsKey(username)) {
            throw new IllegalArgumentException("An account with ID / Username '" + username + "' already exists! Please choose a different ID or sign in.");
        }

        String nowStr = LocalDateTime.now().format(ISO_FMT);
        String effectiveRole = (role == null || role.isBlank()) ? "STUDENT" : role.toUpperCase().trim();
        String name = (fullName != null && !fullName.isBlank()) ? fullName.trim() : username;
        String userEmail = (email != null && !email.isBlank()) ? email.trim() : (username + "@thapar.edu");

        User user = new User("U-" + username, username, password.trim(), name, effectiveRole, userEmail, nowStr);
        dataStore.getUsers().put(username, user);

        // Register profile according to role
        if ("STUDENT".equals(effectiveRole)) {
            Student s = new Student(
                    username,
                    name,
                    (department != null && !department.isBlank()) ? department : "Computer Science (CSED)",
                    (year != null && !year.isBlank()) ? year : "1st Year",
                    "Not Specified",
                    19,
                    (bloodGroup != null && !bloodGroup.isBlank()) ? bloodGroup : "O+",
                    (allergies != null && !allergies.isBlank()) ? allergies : "None Reported",
                    (chronic != null && !chronic.isBlank()) ? chronic : "None Reported",
                    "Guardian / Parent",
                    (phone != null && !phone.isBlank()) ? phone : "+91 98765 00000",
                    (phone != null && !phone.isBlank()) ? phone : "+91 98765 00000",
                    userEmail
            );
            dataStore.getStudents().put(username, s);
        } else if ("DOCTOR".equals(effectiveRole)) {
            int docCount = dataStore.getDoctors().size() + 1;
            Doctor d = new Doctor(
                    "DOC-" + username,
                    name,
                    (department != null && !department.isBlank()) ? department : "General Medicine & Health Officer",
                    "MBBS, MD",
                    "Chamber " + docCount,
                    (phone != null && !phone.isBlank()) ? phone : "+91 98765 1100" + docCount,
                    userEmail,
                    "Mon - Sat",
                    "09:00 AM - 05:00 PM",
                    "AVAILABLE"
            );
            dataStore.getDoctors().put(d.getId(), d);
        }

        dataStore.logActivity(username, effectiveRole, "USER_REGISTERED",
                "Created new " + effectiveRole + " account for " + name + " (ID: " + username + ")");
        dataStore.saveAsync();
        return user;
    }

    public String createSession(User user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, user);
        return token;
    }

    public User getUserBySession(String token) {
        if (token == null) return null;
        return sessions.get(token);
    }

    public void removeSession(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}
