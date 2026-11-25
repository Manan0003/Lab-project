package com.college.dispensary.model;

public class Student {
    private String rollNo;
    private String name;
    private String department;
    private String year; // "1st Year", "2nd Year", "3rd Year", "4th Year"
    private String gender;
    private int age;
    private String bloodGroup; // "A+", "B+", "O+", "AB+", "A-", "B-", "O-", "AB-"
    private String allergies; // e.g. "Penicillin, Sulfa", or "None"
    private String chronicConditions; // e.g. "Asthma", or "None"
    private String emergencyContactName;
    private String emergencyPhone;
    private String phone;
    private String email;

    public Student() {}

    public Student(String rollNo, String name, String department, String year, String gender, int age,
                   String bloodGroup, String allergies, String chronicConditions,
                   String emergencyContactName, String emergencyPhone, String phone, String email) {
        this.rollNo = rollNo;
        this.name = name;
        this.department = department;
        this.year = year;
        this.gender = gender;
        this.age = age;
        this.bloodGroup = bloodGroup;
        this.allergies = allergies;
        this.chronicConditions = chronicConditions;
        this.emergencyContactName = emergencyContactName;
        this.emergencyPhone = emergencyPhone;
        this.phone = phone;
        this.email = email;
    }

    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }

    public String getChronicConditions() { return chronicConditions; }
    public void setChronicConditions(String chronicConditions) { this.chronicConditions = chronicConditions; }

    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

    public String getEmergencyPhone() { return emergencyPhone; }
    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
