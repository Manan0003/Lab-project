package com.college.dispensary.handler;

import com.college.dispensary.model.User;
import com.college.dispensary.repository.DataStore;
import com.college.dispensary.service.AdminService;
import com.college.dispensary.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminHandler implements HttpHandler {
    private final AdminService adminService = AdminService.getInstance();
    private final DataStore dataStore = DataStore.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpUtil.handleCors(exchange)) return;

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/api/dashboard/statistics".equals(path) && "GET".equalsIgnoreCase(method)) {
            Map<String, Object> stats = adminService.getDashboardStatistics();
            HttpUtil.sendJsonResponse(exchange, 200, stats);
        } else if ("/api/admin/users".equals(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                List<User> users = new ArrayList<>(dataStore.getUsers().values());
                HttpUtil.sendJsonResponse(exchange, 200, users);
            } else if ("POST".equalsIgnoreCase(method)) {
                handleAddUser(exchange);
            } else {
                HttpUtil.sendError(exchange, 405, "Method not allowed");
            }
        } else {
            HttpUtil.sendError(exchange, 404, "Endpoint not found");
        }
    }

    private void handleAddUser(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.readJsonBody(exchange);
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String fullName = (String) body.get("fullName");
        String role = (String) body.get("role");
        String email = (String) body.get("email");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            HttpUtil.sendError(exchange, 400, "Username and password are required");
            return;
        }

        if (dataStore.getUsers().containsKey(username)) {
            HttpUtil.sendError(exchange, 409, "User with username " + username + " already exists");
            return;
        }

        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        User user = new User("U-" + System.currentTimeMillis(), username, password, fullName, role, email, nowStr);
        dataStore.getUsers().put(username, user);
        dataStore.logActivity("ADMIN", "ADMIN", "USER_CREATED", "Created user account: " + username + " (" + role + ")");
        dataStore.saveAsync();

        HttpUtil.sendSuccess(exchange, "User created successfully", user);
    }
}
