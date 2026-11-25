package com.college.dispensary.handler;

import com.college.dispensary.model.Token;
import com.college.dispensary.service.TokenService;
import com.college.dispensary.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TokenHandler implements HttpHandler {
    private final TokenService tokenService = TokenService.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpUtil.handleCors(exchange)) return;

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/api/tokens".equals(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                List<Token> list = tokenService.getAllTokens();
                HttpUtil.sendJsonResponse(exchange, 200, list);
            } else if ("POST".equalsIgnoreCase(method)) {
                handleCreateToken(exchange);
            } else {
                HttpUtil.sendError(exchange, 405, "Method not allowed");
            }
        } else if ("/api/tokens/queue".equals(path) && "GET".equalsIgnoreCase(method)) {
            Map<String, Object> queueData = new LinkedHashMap<>();
            List<Token> waiting = tokenService.getWaitingQueue();
            List<Token> awaitingArrival = tokenService.getAwaitingArrivalList();
            Token current = tokenService.getCurrentConsultationToken();
            List<Map<String, Object>> fullBoard = tokenService.getFullQueueBoard();

            queueData.put("currentConsultation", current);
            queueData.put("waitingQueue", waiting);
            queueData.put("awaitingArrival", awaitingArrival);
            queueData.put("fullQueueBoard", fullBoard);
            queueData.put("totalWaiting", waiting.size());
            queueData.put("totalAwaitingArrival", awaitingArrival.size());
            queueData.put("estimatedWaitMins", waiting.size() * 12);

            HttpUtil.sendJsonResponse(exchange, 200, queueData);
        } else if ("/api/tokens/student".equals(path) && "GET".equalsIgnoreCase(method)) {
            Map<String, String> params = HttpUtil.parseQueryParams(exchange);
            String rollNo = params.get("rollNo");
            if (rollNo == null || rollNo.isBlank()) {
                HttpUtil.sendError(exchange, 400, "rollNo parameter is required");
                return;
            }
            Token active = tokenService.getStudentActiveToken(rollNo);
            HttpUtil.sendJsonResponse(exchange, 200, active);
        } else if ("/api/tokens/confirm-arrival".equals(path) && "POST".equalsIgnoreCase(method)) {
            handleConfirmArrival(exchange);
        } else if ("/api/tokens/cancel".equals(path) && "POST".equalsIgnoreCase(method)) {
            Map<String, Object> body = HttpUtil.readJsonBody(exchange);
            String tokenId = (String) body.get("tokenId");
            String reason = (String) body.get("reason");
            boolean cancelled = tokenService.cancelToken(tokenId, reason);
            if (cancelled) {
                HttpUtil.sendSuccess(exchange, "Token cancelled", null);
            } else {
                HttpUtil.sendError(exchange, 404, "Token not found");
            }
        } else {
            HttpUtil.sendError(exchange, 404, "Endpoint not found");
        }
    }

    private void handleCreateToken(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.readJsonBody(exchange);
        String rollNo = (String) body.get("studentRollNo");
        String symptoms = (String) body.get("symptoms");
        String priority = (String) body.get("priority");
        String bookingType = (String) body.get("bookingType");
        String role = (String) body.get("role");

        if (rollNo == null || rollNo.isBlank()) {
            HttpUtil.sendError(exchange, 400, "Student Roll Number is required");
            return;
        }

        Token token = tokenService.createToken(rollNo, symptoms, priority, bookingType, null, role);
        String msg = "AWAITING_ARRIVAL".equals(token.getStatus())
                ? "Walk-in Token #" + token.getTokenNumber() + " booked! Please check in at the reception desk upon reaching the dispensary to enter the active doctor queue."
                : "Token #" + token.getTokenNumber() + " admitted directly to queue.";
        HttpUtil.sendSuccess(exchange, msg, token);
    }

    private void handleConfirmArrival(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.readJsonBody(exchange);
        String tokenId = (String) body.get("tokenId");
        String confirmedBy = (String) body.get("confirmedBy");

        if (tokenId == null || tokenId.isBlank()) {
            HttpUtil.sendError(exchange, 400, "tokenId is required");
            return;
        }

        try {
            Token confirmed = tokenService.confirmArrival(tokenId, confirmedBy);
            if (confirmed == null) {
                HttpUtil.sendError(exchange, 404, "Token not found");
            } else {
                HttpUtil.sendSuccess(exchange, "Arrival confirmed for Token #" + confirmed.getTokenNumber() + ". Admitted to consultation queue (Slot: " + confirmed.getRunningSlot() + ").", confirmed);
            }
        } catch (IllegalStateException e) {
            HttpUtil.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            HttpUtil.sendError(exchange, 500, e.getMessage());
        }
    }
}
