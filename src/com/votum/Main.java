package com.votum;

import com.votum.service.AuthenticationService;
import com.votum.service.ElectionService;
import com.votum.service.ResultService;
import com.votum.service.VotingService;
import com.votum.storage.AuditLogger;
import com.votum.storage.FileManager;
import com.votum.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * ============================================================================
 * VOTUM — Digital Campus Election Management System
 * ============================================================================
 * 
 * Course: CSC2104 - Object Oriented Programming (Java)
 * 
 * This is the Main entry point of the application.
 * It is responsible for:
 *  1. Setting up the native Operating System Look & Feel (so the UI looks clean).
 *  2. Initializing the persistence layer (FileManager and AuditLogger).
 *  3. Initializing the business service layer (ElectionService, VotingService, etc.).
 *  4. Logging the application start event in data/audit.log.
 *  5. Launching the main Login window on the Swing Event Dispatch Thread (EDT).
 * 
 * Architecture Flow:
 *  UI (LoginFrame) -> Service Layer -> Storage Layer (FileManager) -> Text Files
 */
public class Main {

    public static void main(String[] args) {
        // --- Step 1: Set Native System Look & Feel ---
        // This makes Swing buttons and windows look modern on Windows, Mac, and Linux.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // If the native theme cannot be loaded, Swing safely falls back to its default look.
            System.err.println("Note: Running with default Look & Feel: " + e.getMessage());
        }

        // --- Step 2: Initialize File Storage & Audit Logger ---
        // FileManager handles text file operations in the "data/" directory.
        // AuditLogger maintains an immutable log of critical election actions in data/audit.log.
        FileManager fileManager = new FileManager("data");
        AuditLogger auditLogger = new AuditLogger("data/audit.log");

        // --- Step 3: Initialize Business Services ---
        // Following layered architecture: UI never accesses files directly.
        // Services contain all election rules, single-vote verification, and result calculations.
        ElectionService electionService = new ElectionService(fileManager, auditLogger);
        VotingService votingService = new VotingService(fileManager, auditLogger, electionService);
        ResultService resultService = new ResultService(fileManager, auditLogger, electionService);
        AuthenticationService authService = new AuthenticationService(fileManager, auditLogger);

        // Record the application launch in the audit log
        auditLogger.log("Application started", "VOTUM Desktop GUI launched successfully");

        // --- Step 4: Launch the GUI on the Event Dispatch Thread (EDT) ---
        // Swing GUI operations must always run on the EDT to avoid thread concurrency glitches.
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame(
                    fileManager,
                    auditLogger,
                    electionService,
                    votingService,
                    resultService,
                    authService
            );
            // Open the application in full-screen mode for the best kiosk / election desk experience
            loginFrame.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
            loginFrame.setVisible(true);
        });
    }
}

