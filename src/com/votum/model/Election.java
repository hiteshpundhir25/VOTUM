package com.votum.model;

import java.time.LocalDateTime;

/**
 * Represents a campus election in VOTUM.
 * Uses Java Date/Time API (LocalDateTime) for strict time-window enforcement
 * and implements Identifiable for polymorphic entity lookup.
 *
 * Aligned with CSC2104 Course Outcomes CSC2104.2 & CSC2104.3.
 */
public class Election implements Identifiable {
    private String electionId;
    private String name;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ElectionStatus status;

    public Election(String electionId, String name, String description, LocalDateTime startTime, LocalDateTime endTime, ElectionStatus status) {
        this.electionId = electionId == null ? "" : electionId.trim();
        this.name = name == null ? "" : name.trim();
        this.description = description == null ? "" : description.trim();
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status == null ? ElectionStatus.DRAFT : status;
    }

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

    /**
     * Checks if voting is currently permitted based on status and time boundaries.
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

    public boolean isBeforeStart(LocalDateTime currentTime) {
        return startTime != null && currentTime.isBefore(startTime);
    }

    public boolean isAfterEnd(LocalDateTime currentTime) {
        return endTime != null && currentTime.isAfter(endTime);
    }

    @Override
    public String toString() {
        return name + " [" + status + "] (ID: " + electionId + ")";
    }
}
