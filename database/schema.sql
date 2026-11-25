-- ============================================================
-- Thapar University Dispensary Management System
-- Database Schema for MySQL 8.0+
-- Domain: Healthcare / University Health Information System
-- ============================================================

CREATE DATABASE IF NOT EXISTS thapar_dispensary;
USE thapar_dispensary;

-- 1. USERS TABLE (Role-based Authentication)
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PHARMACIST', 'STUDENT') NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. PATIENTS TABLE (Students, Faculty, Staff)
CREATE TABLE IF NOT EXISTS patients (
    roll_no VARCHAR(50) PRIMARY KEY,       -- University Roll No or Staff Employee ID
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,     -- e.g. CSED, ECED, MED, CHED, Biotechnology
    year VARCHAR(20),                     -- 1st Year, 2nd Year, 3rd Year, 4th Year, Faculty
    gender ENUM('Male', 'Female', 'Other') NOT NULL,
    age INT NOT NULL,
    blood_group VARCHAR(10) NOT NULL,     -- A+, B+, O+, AB+, etc.
    allergies TEXT,                       -- Critical drug & food allergies
    chronic_conditions TEXT,              -- Asthma, Diabetes, Hypertension, etc.
    emergency_contact_name VARCHAR(100),
    emergency_phone VARCHAR(20),
    phone VARCHAR(20),
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. DOCTORS TABLE
CREATE TABLE IF NOT EXISTS doctors (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    qualification VARCHAR(100) NOT NULL,
    chamber VARCHAR(50),
    contact_number VARCHAR(20),
    email VARCHAR(100),
    available_days VARCHAR(50),
    available_hours VARCHAR(100),
    status ENUM('AVAILABLE', 'IN_CONSULTATION', 'ON_LEAVE') DEFAULT 'AVAILABLE'
);

-- 4. APPOINTMENTS & TOKENS TABLE
CREATE TABLE IF NOT EXISTS appointments (
    id VARCHAR(50) PRIMARY KEY,
    token_number INT NOT NULL,
    patient_roll_no VARCHAR(50) NOT NULL,
    patient_name VARCHAR(100) NOT NULL,
    department VARCHAR(100),
    doctor_id VARCHAR(50),
    doctor_name VARCHAR(100),
    appointment_date DATE NOT NULL,
    time_slot VARCHAR(50) NOT NULL,
    symptoms TEXT,
    priority ENUM('NORMAL', 'URGENT', 'EMERGENCY') DEFAULT 'NORMAL',
    status ENUM('Pending', 'Confirmed', 'Completed', 'Cancelled') DEFAULT 'Pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_roll_no) REFERENCES patients(roll_no) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE SET NULL
);

-- 5. MEDICINES TABLE (Inventory Master)
CREATE TABLE IF NOT EXISTS medicines (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    generic_name VARCHAR(100) NOT NULL,
    category ENUM('Tablet', 'Syrup', 'Capsule', 'Ointment', 'Injection', 'First-Aid', 'Drop', 'Sachet') NOT NULL,
    stock_quantity INT DEFAULT 0,
    min_threshold INT DEFAULT 10,
    unit VARCHAR(30) DEFAULT 'units',
    expiry_date DATE NOT NULL,
    batch_no VARCHAR(50) NOT NULL,
    rack_location VARCHAR(50),
    dosage_guide VARCHAR(255)
);

-- 6. MEDICINE BATCHES TABLE (Batch & Expiry Tracking)
CREATE TABLE IF NOT EXISTS medicine_batches (
    batch_id VARCHAR(50) PRIMARY KEY,
    medicine_id VARCHAR(50) NOT NULL,
    batch_no VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    expiry_date DATE NOT NULL,
    received_date DATE NOT NULL,
    FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE
);

-- 7. PRESCRIPTIONS TABLE
CREATE TABLE IF NOT EXISTS prescriptions (
    id VARCHAR(50) PRIMARY KEY,
    appointment_id VARCHAR(50),
    token_id VARCHAR(50),
    patient_roll_no VARCHAR(50) NOT NULL,
    patient_name VARCHAR(100) NOT NULL,
    doctor_id VARCHAR(50),
    doctor_name VARCHAR(100) NOT NULL,
    date DATETIME NOT NULL,
    blood_pressure VARCHAR(30),
    temperature VARCHAR(30),
    pulse VARCHAR(30),
    weight VARCHAR(30),
    symptoms TEXT NOT NULL,
    diagnosis TEXT NOT NULL,
    clinical_notes TEXT,
    sick_leave_issued BOOLEAN DEFAULT FALSE,
    sick_leave_days INT DEFAULT 0,
    sick_leave_from DATE,
    sick_leave_to DATE,
    sick_leave_advice TEXT,
    status ENUM('PENDING_DISPENSE', 'DISPENSED', 'PARTIALLY_DISPENSED') DEFAULT 'PENDING_DISPENSE',
    dispensed_at DATETIME,
    dispensed_by VARCHAR(100),
    FOREIGN KEY (patient_roll_no) REFERENCES patients(roll_no) ON DELETE CASCADE
);

-- 8. PRESCRIPTION ITEMS TABLE
CREATE TABLE IF NOT EXISTS prescription_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    prescription_id VARCHAR(50) NOT NULL,
    medicine_id VARCHAR(50) NOT NULL,
    medicine_name VARCHAR(100) NOT NULL,
    dosage VARCHAR(50) NOT NULL,           -- e.g. "1-0-1 (After Meals)"
    duration VARCHAR(50) NOT NULL,         -- e.g. "3 Days"
    quantity INT NOT NULL,                 -- e.g. 6
    instructions TEXT,
    dispensed BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE,
    FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE
);

-- 9. DISPENSING TRANSACTIONS TABLE
CREATE TABLE IF NOT EXISTS dispensing (
    id VARCHAR(50) PRIMARY KEY,
    prescription_id VARCHAR(50),
    patient_roll_no VARCHAR(50) NOT NULL,
    patient_name VARCHAR(100) NOT NULL,
    medicine_id VARCHAR(50) NOT NULL,
    medicine_name VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    batch_no VARCHAR(50),
    dispensed_at DATETIME NOT NULL,
    dispensed_by VARCHAR(100) NOT NULL,
    notes TEXT,
    FOREIGN KEY (patient_roll_no) REFERENCES patients(roll_no)
);

-- 10. AUDIT LOGS TABLE
CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(50) PRIMARY KEY,
    timestamp DATETIME NOT NULL,
    user VARCHAR(50) NOT NULL,
    role VARCHAR(30) NOT NULL,
    action VARCHAR(50) NOT NULL,
    details TEXT
);
