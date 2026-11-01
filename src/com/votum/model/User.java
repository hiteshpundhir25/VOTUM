package com.votum.model;

/**
 * ============================================================================
 * User (Abstract Base Class)
 * ============================================================================
 * 
 * Course: CSC2104 - Object Oriented Programming (Java)
 * 
 * Demonstrates Core OOP Principles:
 *  1. ABSTRACTION: Declared as an 'abstract class' because a generic "User" cannot 
 *     exist on its own. In our system, a user must be either an Admin or a Voter.
 *  2. ENCAPSULATION: All data fields (username, password, role) are 'private'. 
 *     They can only be accessed or modified via public getter and setter methods.
 *  3. INTERFACE IMPLEMENTATION: Implements 'AuthenticatableUser' (which in turn 
 *     extends 'Identifiable'), fulfilling the interface contract.
 *  4. POLYMORPHISM: Defines abstract methods getId() and getDisplayName(). Subclasses 
 *     (Admin and Voter) provide their own unique implementation.
 */
public abstract class User implements AuthenticatableUser {

    // --- Private Attributes (Encapsulation) ---
    private String username;
    private String password;
    private String role; // Stores either "ADMIN" or "VOTER"

    /**
     * Parameterized Constructor to initialize common user fields.
     * Uses defensive null-checks so the fields never become null.
     */
    public User(String username, String password, String role) {
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password;
        this.role = role == null ? "" : role.trim().toUpperCase();
    }

    // --- Getters and Setters (Encapsulation) ---

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? "" : username.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password == null ? "" : password;
    }

    /**
     * Checks if the supplied plain-text password matches this user's password.
     * Used by AuthenticationService during sign-in.
     */
    @Override
    public boolean verifyPassword(String plainPassword) {
        return this.password != null && this.password.equals(plainPassword);
    }

    @Override
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role == null ? "" : role.trim().toUpperCase();
    }

    /**
     * Helper methods to check user roles easily without hardcoding strings elsewhere.
     */
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(this.role);
    }

    public boolean isVoter() {
        return "VOTER".equalsIgnoreCase(this.role);
    }

    // --- Abstract Methods (Polymorphism) ---

    /**
     * Subclasses MUST implement this method to return a user-friendly display string.
     * For Admin: returns "Officer FullName (Officer)"
     * For Voter: returns "Student Name (Department)"
     */
    @Override
    public abstract String getDisplayName();

    /**
     * Subclasses MUST implement this method to return their unique primary key.
     * For Admin: returns adminId (e.g., "ADM001")
     * For Voter: returns voterId (e.g., "V001")
     */
    @Override
    public abstract String getId();

    /**
     * Overridden toString method.
     * Notice: Password is intentionally excluded for security so credentials 
     * are never printed to console logs by accident.
     */
    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}

