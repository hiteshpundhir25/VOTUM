package com.votum.service;

import com.votum.exception.InvalidLoginException;
import com.votum.model.Admin;
import com.votum.model.User;
import com.votum.model.Voter;
import com.votum.storage.AuditLogger;
import com.votum.storage.FileManager;

import java.util.List;

/**
 * Handles user authentication for both Administrators and Student Voters.
 * Enforces credential verification and logs all login attempts.
 */
public class AuthenticationService {
    private final FileManager fileManager;
    private final AuditLogger auditLogger;

    public AuthenticationService(FileManager fileManager, AuditLogger auditLogger) {
        this.fileManager = fileManager;
        this.auditLogger = auditLogger;
    }

    /**
     * Authenticates a user against stored records.
     * Checks administrators first, then student voters.
     * 
     * @param username user input username
     * @param password user input password
     * @return authenticated User object (Admin or Voter)
     * @throws InvalidLoginException if credentials do not match or fields are blank
     */
    public User login(String username, String password) throws InvalidLoginException {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new InvalidLoginException("Username and password cannot be empty.");
        }

        String trimmedUser = username.trim();

        // 1. Check Administrators
        List<Admin> admins = fileManager.loadAdmins();
        for (Admin admin : admins) {
            if (admin.getUsername().equalsIgnoreCase(trimmedUser) && admin.getPassword().equals(password)) {
                auditLogger.log("Admin logged in", "Admin: " + admin.getUsername() + " (" + admin.getFullName() + ")");
                return admin;
            }
        }

        // 2. Check Student Voters
        List<Voter> voters = fileManager.loadVoters();
        for (Voter voter : voters) {
            if (voter.getUsername().equalsIgnoreCase(trimmedUser) && voter.getPassword().equals(password)) {
                auditLogger.log("Voter logged in", "Voter: " + voter.getUsername() + " [ID: " + voter.getVoterId() + "]");
                return voter;
            }
        }

        auditLogger.log("Login failed", "Attempted username: " + trimmedUser);
        throw new InvalidLoginException("Invalid username or password. Please check your credentials.");
    }
}
