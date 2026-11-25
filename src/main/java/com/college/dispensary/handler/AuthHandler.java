package com.college.dispensary.handler;

import com.college.dispensary.model.User;
import com.college.dispensary.service.AuthService;
import com.college.dispensary.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuthHandler implements HttpHandler {
    private final AuthService authService = AuthService.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpUtil.handleCors(exchange)) return;

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/api/auth/login".equals(path) && "POST".equalsIgnoreCase(method)) {
            handleLogin(exchange);
        } else if ("/api/auth/me".equals(path) && "GET".equalsIgnoreCase(method)) {
            handleMe(exchange);
        } else if ("/api/auth/logout".equals(path) && "POST".equalsIgnoreCase(method)) {
            handleLogout(exchange);
        } else {
            HttpUtil.sendError(exchange, 404, "Endpoint not found");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.readJsonBody(exchange);
        String username = (String) body.get("username");
        String password = (String) body.get("password");

        User user = authService.authenticate(username, password);
        if (user == null) {
            HttpUtil.sendError(exchange, 401, "Invalid username or password");
            return;
        }

        String sessionToken = authService.createSession(user);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("token", sessionToken);
        resp.put("user", user);

        HttpUtil.sendSuccess(exchange, "Login successful", resp);
    }

    private void handleMe(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        User user = authService.getUserBySession(token);
        if (user == null) {
            HttpUtil.sendError(exchange, 401, "Unauthorized session");
            return;
        }

        HttpUtil.sendSuccess(exchange, "Authenticated", user);
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.removeSession(authHeader.substring(7));
        }
        HttpUtil.sendSuccess(exchange, "Logged out successfully", null);
    }
}
