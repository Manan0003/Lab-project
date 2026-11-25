package com.college.dispensary.handler;

import com.college.dispensary.model.DispenseRecord;
import com.college.dispensary.model.Medicine;
import com.college.dispensary.model.Prescription;
import com.college.dispensary.service.InventoryService;
import com.college.dispensary.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class InventoryHandler implements HttpHandler {
    private final InventoryService inventoryService = InventoryService.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpUtil.handleCors(exchange)) return;

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/api/medicines".equals(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                List<Medicine> list = inventoryService.getAllMedicines();
                HttpUtil.sendJsonResponse(exchange, 200, list);
            } else if ("POST".equalsIgnoreCase(method)) {
                handleAddMedicine(exchange);
            } else if ("PUT".equalsIgnoreCase(method)) {
                handleUpdateMedicine(exchange);
            } else {
                HttpUtil.sendError(exchange, 405, "Method not allowed");
            }
        } else if ("/api/medicines/restock".equals(path) && "POST".equalsIgnoreCase(method)) {
            handleRestock(exchange);
        } else if ("/api/inventory/alerts".equals(path) && "GET".equalsIgnoreCase(method)) {
            Map<String, Object> alerts = inventoryService.getAlerts();
            HttpUtil.sendJsonResponse(exchange, 200, alerts);
        } else if ("/api/inventory/pending-prescriptions".equals(path) && "GET".equalsIgnoreCase(method)) {
            List<Prescription> pending = inventoryService.getPendingPrescriptions();
            HttpUtil.sendJsonResponse(exchange, 200, pending);
        } else if ("/api/dispensing".equals(path)) {
            if ("POST".equalsIgnoreCase(method)) {
                handleDispense(exchange);
            } else if ("GET".equalsIgnoreCase(method)) {
                List<DispenseRecord> history = inventoryService.getDispenseHistory();
                HttpUtil.sendJsonResponse(exchange, 200, history);
            } else {
                HttpUtil.sendError(exchange, 405, "Method not allowed");
            }
        } else if ("/api/dispensing/direct".equals(path) && "POST".equalsIgnoreCase(method)) {
            handleDirectDispense(exchange);
        } else {
            HttpUtil.sendError(exchange, 404, "Endpoint not found");
        }
    }

    private void handleAddMedicine(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.readJsonBody(exchange);
        int stock = body.get("stockQuantity") instanceof Number n ? n.intValue() : 0;
        int min = body.get("minThreshold") instanceof Number n ? n.intValue() : 10;

        Medicine med = new Medicine(
                (String) body.get("id"),
                (String) body.get("name"),
                (String) body.get("genericName"),
                (String) body.get("category"),
                stock,
                min,
                (String) body.get("unit"),
                (String) body.get("expiryDate"),
                (String) body.get("batchNo"),
                (String) body.get("rackLocation"),
                (String) body.get("dosageGuide")
        );
        Medicine created = inventoryService.addMedicine(med);
        HttpUtil.sendSuccess(exchange, "Medicine added to inventory", created);
    }

    private void handleUpdateMedicine(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.readJsonBody(exchange);
        int stock = body.get("stockQuantity") instanceof Number n ? n.intValue() : 0;
        int min = body.get("minThreshold") instanceof Number n ? n.intValue() : 10;

        Medicine med = new Medicine(
                (String) body.get("id"),
                (String) body.get("name"),
                (String) body.get("genericName"),
                (String) body.get("category"),
                stock,
                min,
                (String) body.get("unit"),
                (String) body.get("expiryDate"),
                (String) body.get("batchNo"),
                (String) body.get("rackLocation"),
                (String) body.get("dosageGuide")
        );
        Medicine updated = inventoryService.updateMedicine(med);
        if (updated == null) {
            HttpUtil.sendError(exchange, 404, "Medicine not found");
        } else {
            HttpUtil.sendSuccess(exchange, "Medicine details updated", updated);
        }
    }

    private void handleRestock(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.readJsonBody(exchange);
        String id = (String) body.get("medicineId");
        int qty = body.get("quantity") instanceof Number n ? n.intValue() : 0;
        String batchNo = (String) body.get("batchNo");
        String expiryDate = (String) body.get("expiryDate");
        String rack = (String) body.get("rackLocation");

        Medicine updated = inventoryService.restockMedicine(id, qty, batchNo, expiryDate, rack);
        if (updated == null) {
            HttpUtil.sendError(exchange, 404, "Medicine not found");
        } else {
            HttpUtil.sendSuccess(exchange, "Stock restocked successfully", updated);
        }
    }

    private void handleDispense(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.readJsonBody(exchange);
        String prescriptionId = (String) body.get("prescriptionId");
        String pharmacistName = (String) body.get("pharmacistName");
        String notes = (String) body.get("notes");

        boolean success = inventoryService.dispensePrescription(prescriptionId, pharmacistName, notes);
        if (success) {
            HttpUtil.sendSuccess(exchange, "Medicines dispensed and inventory stock updated automatically", null);
        } else {
            HttpUtil.sendError(exchange, 400, "Could not dispense prescription. Either already dispensed or not found.");
        }
    }

    private void handleDirectDispense(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.readJsonBody(exchange);
        String rollNo = (String) body.get("studentRollNo");
        String medicineId = (String) body.get("medicineId");
        int qty = body.get("quantity") instanceof Number n ? n.intValue() : 1;
        String pharmacistName = (String) body.get("pharmacistName");
        String notes = (String) body.get("notes");

        DispenseRecord record = inventoryService.dispenseDirect(rollNo, medicineId, qty, pharmacistName, notes);
        if (record == null) {
            HttpUtil.sendError(exchange, 400, "Insufficient stock or invalid medicine");
        } else {
            HttpUtil.sendSuccess(exchange, "Direct OTC medicine dispensed and stock updated", record);
        }
    }
}
