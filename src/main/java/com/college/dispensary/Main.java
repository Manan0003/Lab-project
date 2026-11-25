package com.college.dispensary;

import com.college.dispensary.config.AppConfig;
import com.college.dispensary.handler.*;
import com.college.dispensary.repository.DataStore;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        int port = AppConfig.DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        // Initialize persistent DataStore
        DataStore.getInstance();

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // Handlers
            AuthHandler authHandler = new AuthHandler();
            PatientHandler patientHandler = new PatientHandler();
            DoctorHandler doctorHandler = new DoctorHandler();
            AppointmentHandler appointmentHandler = new AppointmentHandler();
            TokenHandler tokenHandler = new TokenHandler();
            PrescriptionHandler prescriptionHandler = new PrescriptionHandler();
            InventoryHandler inventoryHandler = new InventoryHandler();
            AdminHandler adminHandler = new AdminHandler();

            // Locate web resource directory
            String webDir = findWebDirectory();
            StaticFileHandler staticHandler = new StaticFileHandler(webDir);

            // Register API endpoints
            server.createContext("/api/auth", authHandler);
            server.createContext("/api/patients", patientHandler);
            server.createContext("/api/doctors", doctorHandler);
            server.createContext("/api/doctor", doctorHandler);
            server.createContext("/api/consultations", doctorHandler);
            server.createContext("/api/appointments", appointmentHandler);
            server.createContext("/api/tokens", tokenHandler);
            server.createContext("/api/prescriptions", prescriptionHandler);
            server.createContext("/api/medicines", inventoryHandler);
            server.createContext("/api/inventory", inventoryHandler);
            server.createContext("/api/dispensing", inventoryHandler);
            server.createContext("/api/dashboard", adminHandler);
            server.createContext("/api/admin", adminHandler);

            // Root context for Static UI assets
            server.createContext("/", staticHandler);

            // High concurrency with Virtual Threads
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();

            printBanner(port);

            // Graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[SHUTDOWN] Saving database records to disk...");
                DataStore.getInstance().saveToDisk();
                server.stop(1);
                System.out.println("[SHUTDOWN] Server stopped safely.");
            }));

        } catch (IOException e) {
            System.err.println("Failed to start Dispensary Server on port " + port + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String findWebDirectory() {
        String[] possiblePaths = {
                "src/main/resources/web",
                "resources/web",
                "web",
                "./src/main/resources/web"
        };
        for (String p : possiblePaths) {
            File f = new File(p);
            if (f.exists() && f.isDirectory()) {
                return f.getAbsolutePath();
            }
        }
        return new File("src/main/resources/web").getAbsolutePath();
    }

    private static void printBanner(int port) {
        System.out.println("=======================================================================");
        System.out.println("  " + AppConfig.UNIVERSITY_NAME);
        System.out.println("  " + AppConfig.DISPENSARY_NAME);
        System.out.println("  Software Engineering Project · Java 25 · Built-in HTTP REST Server");
        System.out.println("=======================================================================");
        System.out.println("  Server is LIVE at: http://localhost:" + port);
        System.out.println();
        System.out.println("  Available Portals:");
        System.out.println("  - Landing & Portal Hub:    http://localhost:" + port + "/");
        System.out.println("  - Student / Patient:       http://localhost:" + port + "/student.html");
        System.out.println("  - Doctor Consultation:     http://localhost:" + port + "/doctor.html");
        System.out.println("  - Receptionist Front-Desk: http://localhost:" + port + "/receptionist.html");
        System.out.println("  - Medicine Inventory:      http://localhost:" + port + "/inventory.html");
        System.out.println("  - Administrator Dashboard: http://localhost:" + port + "/admin.html");
        System.out.println();
        System.out.println("  Pre-configured Demo Logins (Password: <username>123 or student123):");
        System.out.println("  * Admin:        admin     / admin123");
        System.out.println("  * Doctor:       doctor    / doctor123");
        System.out.println("  * Receptionist: reception / reception123");
        System.out.println("  * Pharmacist:   pharma    / pharma123");
        System.out.println("  * Students:     102103456, 102204123, 102008765, 102301987 (pass: student123)");
        System.out.println("=======================================================================");
    }
}
