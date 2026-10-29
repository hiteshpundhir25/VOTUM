package com.votum.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the text-based audit trail for Votum.
 * Appends every critical election action to data/audit.log with a timestamp.
 * Supports filtering and clearing logs when an election is deleted.
 */
public class AuditLogger {
    private static final String DEFAULT_LOG_PATH = "data/audit.log";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String logFilePath;

    public AuditLogger() {
        this(DEFAULT_LOG_PATH);
    }

    public AuditLogger(String logFilePath) {
        this.logFilePath = logFilePath;
        ensureLogFileExists();
    }

    private void ensureLogFileExists() {
        try {
            File file = new File(logFilePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            System.err.println("Warning: Unable to initialize audit log file: " + e.getMessage());
        }
    }

    /**
     * Appends an audit event to the log file.
     * Format: YYYY-MM-DD HH:MM:SS | EVENT | DETAILS
     */
    public synchronized void log(String event, String details) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logEntry = timestamp + " | " + event + " | " + (details == null ? "" : details);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Audit logging failed: " + e.getMessage());
        }
    }

    /**
     * Reads all recorded audit logs.
     */
    public synchronized List<String> readAllLogs() {
        List<String> logs = new ArrayList<>();
        File file = new File(logFilePath);
        if (!file.exists()) {
            return logs;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    logs.add(line.trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading audit logs: " + e.getMessage());
        }
        return logs;
    }

    /**
     * Removes all log entries mentioning a specific election ID,
     * ensuring deleted election data does not linger in the logs.
     */
    public synchronized void removeLogsForElection(String electionId) {
        if (electionId == null || electionId.trim().isEmpty()) {
            return;
        }
        String target = electionId.trim();
        List<String> existingLogs = readAllLogs();
        List<String> filteredLogs = new ArrayList<>();

        for (String entry : existingLogs) {
            // Keep entries that do NOT refer to the deleted election ID.
            // Matching is token based so that purging 'E001' never erases entries
            // that merely contain 'E001' inside another identifier (e.g. 'E0010').
            if (!containsToken(entry, target)) {
                filteredLogs.add(entry);
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, false))) {
            for (String entry : filteredLogs) {
                writer.write(entry);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error purging election logs: " + e.getMessage());
        }
    }

    /**
     * Checks whether a token appears in a log entry as a whole identifier.
     * Hyphens and underscores are kept inside tokens so that compound IDs
     * ('TEST-E01', 'VOTE-A1B2C3D4') still match exactly.
     */
    private static boolean containsToken(String entry, String token) {
        if (entry == null || token == null || token.isEmpty()) {
            return false;
        }
        String[] parts = entry.split("[^A-Za-z0-9_-]+");
        for (String part : parts) {
            if (part.equalsIgnoreCase(token)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clears all log entries from the audit log file.
     */
    public synchronized void clearAllLogs() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, false))) {
            // Opening in overwrite mode truncates the trail; flush makes the truncation explicit.
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error clearing audit log: " + e.getMessage());
        }
    }
}
