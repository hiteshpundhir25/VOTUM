package com.votum.service;

import com.votum.exception.InvalidLoginException;
import com.votum.model.Admin;
import com.votum.model.User;
import com.votum.model.Voter;
import com.votum.storage.AuditLogger;
import com.votum.storage.FileManager;

import java.util.List;

/**
 * ============================================================================
 * AuthenticationService (User Login & Credential Verification)
 * ============================================================================
 * 
 * Course: CSC2104 - Object Oriented Programming (Java)
 * 
 * Demonstrates:
 *  1. POLYMORPHIC RETURN TYPE:
 *     The login() method returns the abstract type 'User'. Depending on who logged in, 
 *     it returns either an 'Admin' or a 'Voter' instance. The UI uses polymorphism 
 *     to open either the AdminDashboard or VoterDashboard.
 *  2. EXCEPTION HANDLING:
 *     Throws a custom checked exception 'InvalidLoginException' if fields are empty 
 *     or credentials do not match any known user.
 *  3. AUDIT TRAIL:
 *     Every login attempt (both success and failure) is recorded in data/audit.log 
 *     for security and accountability.
 */
public class AuthenticationService {

    // Dependencies injected via constructor (Inversion of Control)
    private final FileManager fileManager;
    private final AuditLogger auditLogger;

    public AuthenticationService(FileManager fileManager, AuditLogger auditLogger) {
        this.fileManager = fileManager;
        this.auditLogger = auditLogger;
    }

    /**
     * Authenticates a user against stored records in text files.
     * 
     * Process:
     *  Step 1: Check for empty/blank inputs.
     *  Step 2: Check administrators list (users.txt). If match found, log & return Admin.
     *  Step 3: Check student voters list (voters.txt). If match found, log & return Voter.
     *  Step 4: If no match found anywhere, log the failed attempt & throw InvalidLoginException.
     * 
     * @param username user input username
     * @param password user input password
     * @return authenticated User object (polymorphically Admin or Voter)
     * @throws InvalidLoginException if credentials do not match or fields are blank
     */
    public User login(String username, String password) throws InvalidLoginException {
        // Step 1: Input Validation
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new InvalidLoginException("Username and password cannot be empty.");
        }

        String trimmedUser = username.trim();

        // Step 2: Check Administrator Accounts
        List<Admin> admins = fileManager.loadAdmins();
        for (Admin admin : admins) {
            // Case-insensitive username match, exact case-sensitive password match
            if (admin.getUsername().equalsIgnoreCase(trimmedUser) && admin.verifyPassword(password)) {
                auditLogger.log("Admin logged in", "Admin: " + admin.getUsername() + " (" + admin.getFullName() + ")");
                return admin;
            }
        }

        // Step 3: Check Student Voter Accounts
        List<Voter> voters = fileManager.loadVoters();
        for (Voter voter : voters) {
            if (voter.getUsername().equalsIgnoreCase(trimmedUser) && voter.verifyPassword(password)) {
                auditLogger.log("Voter logged in", "Voter: " + voter.getUsername() + " [ID: " + voter.getVoterId() + "]");
                return voter;
            }
        }

        // Step 4: Login Failed
        auditLogger.log("Login failed", "Attempted username: " + trimmedUser);
        throw new InvalidLoginException("Invalid username or password. Please check your credentials.");
    }
}

