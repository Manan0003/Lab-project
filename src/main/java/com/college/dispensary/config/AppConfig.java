package com.college.dispensary.config;

import java.io.File;

public class AppConfig {
    public static final int DEFAULT_PORT = 8080;
    public static final String DATA_DIR = "data";
    public static final String UNIVERSITY_NAME = "Thapar Institute of Engineering and Technology";
    public static final String SHORT_NAME = "Thapar University";
    public static final String DISPENSARY_NAME = "Thapar University Health & Dispensary Center";
    public static final String CAMPUS_LOCATION = "Dispensary Building, Opp. Hostel PG, TIET Campus, Patiala - 147004, Punjab";
    public static final String EMERGENCY_PHONE = "+91 175 2393000 / Ext 3099";
    public static final String AMBULANCE_CONTACT = "+91 98150 99999 (24x7 Campus Emergency)";
    public static final String CLINIC_TIMINGS = "Mon - Sat: 8:00 AM - 8:00 PM (Emergency Services: 24 Hours)";

    public static void ensureDataDirectories() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }
}
