package com.college.dispensary.service;

import com.college.dispensary.model.Prescription;
import com.college.dispensary.repository.DataStore;

import java.time.LocalDate;
import java.util.*;

public class AdminService {
    private static final AdminService INSTANCE = new AdminService();
    private final DataStore dataStore = DataStore.getInstance();

    public static AdminService getInstance() {
        return INSTANCE;
    }

    private AdminService() {}

    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        int totalPatients = dataStore.getStudents().size();
        int totalDoctors = dataStore.getDoctors().size();
        int totalMedicines = dataStore.getMedicines().size();

        String today = LocalDate.now().toString();
        long todayAppointments = dataStore.getAppointments().values().stream()
                .filter(a -> today.equals(a.getAppointmentDate()))
                .count();

        long completedConsultations = dataStore.getPrescriptions().size();

        long pendingAppointments = dataStore.getAppointments().values().stream()
                .filter(a -> "Pending".equalsIgnoreCase(a.getStatus()))
                .count();

        // Inventory alerts
        Map<String, Object> alerts = InventoryService.getInstance().getAlerts();
        int lowStockCount = (int) alerts.get("lowStockCount");
        int expiredCount = (int) alerts.get("expiredCount");
        int expiringSoonCount = (int) alerts.get("expiringSoonCount");

        // Active tokens
        int waitingTokens = TokenService.getInstance().getWaitingQueue().size();

        stats.put("totalPatients", totalPatients);
        stats.put("totalDoctors", totalDoctors);
        stats.put("totalMedicines", totalMedicines);
        stats.put("todayAppointments", todayAppointments);
        stats.put("completedConsultations", completedConsultations);
        stats.put("pendingAppointments", pendingAppointments);
        stats.put("lowStockCount", lowStockCount);
        stats.put("expiredCount", expiredCount);
        stats.put("expiringSoonCount", expiringSoonCount);
        stats.put("waitingTokens", waitingTokens);

        // Disease / Diagnosis Analytics
        Map<String, Integer> diagnosisTrends = new LinkedHashMap<>();
        for (Prescription p : dataStore.getPrescriptions().values()) {
            String diag = p.getDiagnosis();
            if (diag != null && !diag.isBlank()) {
                // simplify key
                String key = categorizeDiagnosis(diag);
                diagnosisTrends.put(key, diagnosisTrends.getOrDefault(key, 0) + 1);
            }
        }
        stats.put("diagnosisTrends", diagnosisTrends);

        // Recent Audit Activity
        stats.put("recentActivities", dataStore.getActivityLogs().stream().limit(15).toList());

        return stats;
    }

    private String categorizeDiagnosis(String raw) {
        String lower = raw.toLowerCase();
        if (lower.contains("fever") || lower.contains("viral") || lower.contains("pyrexia")) return "Viral Fever / Pyrexia";
        if (lower.contains("rhinitis") || lower.contains("allergy") || lower.contains("allergic")) return "Allergic Rhinitis / Cold";
        if (lower.contains("cough") || lower.contains("bronch") || lower.contains("throat")) return "Respiratory / Cough";
        if (lower.contains("sprain") || lower.contains("injury") || lower.contains("fracture")) return "Sports Injury / Sprain";
        if (lower.contains("gastric") || lower.contains("acidity") || lower.contains("gerd")) return "Gastritis / Acid Reflux";
        if (lower.contains("headache") || lower.contains("migraine")) return "Migraine / Headache";
        return raw;
    }
}
