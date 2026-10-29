package com.votum.model;

/**
 * Represents an Administrator (Election Officer) in VOTUM.
 *
 * Demonstrates:
 *  - Inheritance: extends abstract class User via super constructor chaining.
 *  - Polymorphism: implements getDisplayName() and getId().
 *
 * Aligned with CSC2104 Course Outcomes CSC2104.2 & CSC2104.3.
 */
public class Admin extends User {
    private String adminId;
    private String fullName;
    private String email;

    public Admin(String username, String password, String adminId, String fullName, String email) {
        super(username, password, "ADMIN");
        this.adminId = adminId == null ? "" : adminId.trim();
        this.fullName = fullName == null ? "" : fullName.trim();
        this.email = email == null ? "" : email.trim();
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId == null ? "" : adminId.trim();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName == null ? "" : fullName.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? "" : email.trim();
    }

    @Override
    public String getId() {
        return adminId;
    }

    @Override
    public String getDisplayName() {
        return fullName + " (Officer)";
    }

    @Override
    public String toString() {
        return "Admin{" +
                "adminId='" + adminId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", username='" + getUsername() + '\'' +
                '}';
    }
}
