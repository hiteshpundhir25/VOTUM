package com.votum.model;

/**
 * Abstract base class representing any authenticated user in the VOTUM system.
 *
 * Demonstrates core OOP concepts:
 *  - Encapsulation: private credential fields with validation accessors.
 *  - Abstraction: cannot be instantiated directly; defines abstract methods.
 *  - Interface Implementation: implements AuthenticatableUser and Identifiable.
 *
 * Aligned with CSC2104 Course Outcomes CSC2104.2 & CSC2104.3.
 */
public abstract class User implements AuthenticatableUser {
    private String username;
    private String password;
    private String role; // "ADMIN" or "VOTER"

    public User(String username, String password, String role) {
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password;
        this.role = role == null ? "" : role.trim().toUpperCase();
    }

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

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(this.role);
    }

    public boolean isVoter() {
        return "VOTER".equalsIgnoreCase(this.role);
    }

    /**
     * Abstract method returning a polymorphic display title for presentation.
     * Subclasses must provide appropriate role-specific details.
     *
     * @return display title
     */
    @Override
    public abstract String getDisplayName();

    /**
     * Abstract method returning the unique identifier of the user (adminId or voterId).
     *
     * @return user ID
     */
    @Override
    public abstract String getId();

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
