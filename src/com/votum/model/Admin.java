package com.votum.model;

/**
 * ============================================================================
 * Admin (Election Officer Model)
 * ============================================================================
 * 
 * Course: CSC2104 - Object Oriented Programming (Java)
 * 
 * Demonstrates Core OOP Principles:
 *  1. INHERITANCE: 'Admin extends User' - Admin inherits username, password, and role 
 *     functionality from the User superclass.
 *  2. CONSTRUCTOR CHAINING: Uses 'super(username, password, "ADMIN")' to pass 
 *     credentials up to the User parent constructor.
 *  3. POLYMORPHISM: Overrides getId() to return adminId, and getDisplayName() to 
 *     return "fullName (Officer)".
 *  4. ENCAPSULATION: adminId, fullName, and email are private fields with public getters/setters.
 */
public class Admin extends User {

    // --- Private Specific Attributes ---
    private String adminId;   // Unique ID like "ADM001"
    private String fullName;  // Full name of the election officer
    private String email;     // Official university email

    /**
     * Parameterized Constructor.
     * Calls super() to initialize base user credentials with the role "ADMIN".
     */
    public Admin(String username, String password, String adminId, String fullName, String email) {
        super(username, password, "ADMIN");
        this.adminId = adminId == null ? "" : adminId.trim();
        this.fullName = fullName == null ? "" : fullName.trim();
        this.email = email == null ? "" : email.trim();
    }

    // --- Getters and Setters ---

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

    // --- Polymorphic Method Implementations ---

    /**
     * Implements the abstract getId() method from User / Identifiable.
     * Returns the unique admin identifier (e.g., ADM001).
     */
    @Override
    public String getId() {
        return adminId;
    }

    /**
     * Implements the abstract getDisplayName() method from User.
     * Returns a formatted title showing officer name and designation.
     */
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

