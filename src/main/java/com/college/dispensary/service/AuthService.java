package com.college.dispensary.service;

import com.college.dispensary.model.User;
import com.college.dispensary.repository.DataStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
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
