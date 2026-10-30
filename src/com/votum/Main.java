package com.votum;

import com.votum.service.*;
import com.votum.storage.*;
import com.votum.ui.LoginFrame;

import javax.swing.*;

/**
 * Main application entry point for VOTUM.
 * Initializes storage, services, Look & Feel, and launches the login window.
 */
public class Main {
    public static void main(String[] args) {
        // Set cross-platform / system Look and Feel for clean appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Falls back smoothly to default Swing look
        }

        // Initialize Storage & Audit Logger
        FileManager fileManager = new FileManager("data");
        AuditLogger auditLogger = new AuditLogger("data/audit.log");

        // Initialize Business Services
        ElectionService electionService = new ElectionService(fileManager, auditLogger);
        VotingService votingService = new VotingService(fileManager, auditLogger, electionService);
        ResultService resultService = new ResultService(fileManager, auditLogger, electionService);
        AuthenticationService authService = new AuthenticationService(fileManager, auditLogger);

        auditLogger.log("Application started", "VOTUM Desktop GUI launched successfully");

        // Launch UI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame(
                    fileManager,
                    auditLogger,
                    electionService,
                    votingService,
                    resultService,
                    authService
            );
            loginFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            loginFrame.setVisible(true);
        });
    }
}
