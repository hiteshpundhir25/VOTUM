package com.votum.model;

/**
 * Interface defining the contract for system participants that authenticate
 * against credentials in the VOTUM system.
 *
 * Demonstrates CSC2104 Course Outcome CSC2104.3 (Interface Inheritance & Contracts).
 */
public interface AuthenticatableUser extends Identifiable {
    /**
     * Returns the unique login username.
     *
     * @return username
     */
    String getUsername();

    /**
     * Verifies whether the provided plain-text password matches this user's password.
     *
     * @param plainPassword candidate password to check
     * @return true if credentials match, false otherwise
     */
    boolean verifyPassword(String plainPassword);

    /**
     * Returns the user role identifier ("ADMIN" or "VOTER").
     *
     * @return security role
     */
    String getRole();

    /**
     * Returns a polymorphic human-friendly display name for the user.
     *
     * @return display name
     */
    String getDisplayName();
}

