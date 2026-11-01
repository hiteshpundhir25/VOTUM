package com.votum.model;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * Election (Campus Election Entity)
 * ============================================================================
 * 
 * Course: CSC2104 - Object Oriented Programming (Java)
 * 
 * Demonstrates:
 *  1. ENCAPSULATION: All election metadata (ID, title, description, time window, status) 
 *     are private fields with getter/setter accessors.
 *  2. JAVA DATE/TIME API: Uses java.time.LocalDateTime for strict, time-zone safe 
 *     validation of the voting window (start time and end time).
 *  3. ENUM USAGE: Uses the 'ElectionStatus' enum (DRAFT, OPEN, CLOSED) to avoid 
 *     invalid arbitrary strings.
 *  4. BUSINESS LOGIC HELPER: Provides isCurrentlyActive(now) to check whether an 
 *     election is OPEN and within its active start/end voting window.
 */
public class Election implements Identifiable {

    // --- Private Attributes ---
    private String electionId;            // Unique ID like "E001"
    private String name;                   // Human-readable title (e.g. "Student Council President")
    private String description;            // Brief summary of the election purpose
    private LocalDateTime startTime;       // Date and time voting opens
    private LocalDateTime endTime;         // Date and time voting closes
    private ElectionStatus status;         // Lifecycle state: DRAFT, OPEN, or CLOSED

    /**
     * Parameterized Constructor.
     * Defaults status to DRAFT if null is passed.
     */
    public Election(String electionId, String name, String description, LocalDateTime startTime, LocalDateTime endTime, ElectionStatus status) {
        this.electionId = electionId == null ? "" : electionId.trim();
        this.name = name == null ? "" : name.trim();
        this.description = description == null ? "" : description.trim();
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status == null ? ElectionStatus.DRAFT : status;
    }

    // --- Getters and Setters ---

    public String getElectionId() {
        return electionId;
    }

    public void setElectionId(String electionId) {
        this.electionId = electionId == null ? "" : electionId.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public ElectionStatus getStatus() {
        return status;
    }

    public void setStatus(ElectionStatus status) {
        this.status = status == null ? ElectionStatus.DRAFT : status;
    }

    @Override
    public String getId() {
        return electionId;
    }

    // --- Time Window Evaluation Helpers ---

    /**
     * Checks if voting is permitted right now.
     * Returns true ONLY IF:
     *  1. Status is OPEN.
     *  2. Current time is NOT before startTime.
     *  3. Current time is NOT after endTime.
     */
    public boolean isCurrentlyActive(LocalDateTime currentTime) {
        if (status != ElectionStatus.OPEN) {
            return false;
        }
        if (startTime != null && currentTime.isBefore(startTime)) {
            return false;
        }
        if (endTime != null && currentTime.isAfter(endTime)) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if the election is scheduled in the future and hasn't started yet.
     */
    public boolean isBeforeStart(LocalDateTime currentTime) {
        return startTime != null && currentTime.isBefore(startTime);
    }

    /**
     * Returns true if the election deadline has passed.
     */
    public boolean isAfterEnd(LocalDateTime currentTime) {
        return endTime != null && currentTime.isAfter(endTime);
    }

    @Override
    public String toString() {
        return name + " [" + status + "] (ID: " + electionId + ")";
    }
}

