package com.votum.model;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * Vote (Individual Cast Ballot Record)
 * ============================================================================
 * 
 * Course: CSC2104 - Object Oriented Programming (Java)
 * 
 * Demonstrates:
 *  1. ENCAPSULATION: Maintains the primary key voteId, electionId, candidateId, 
 *     voterId, and timestamp as private fields with getters and setters.
 *  2. SINGLE-VOTE AUDITING: Each vote links exactly ONE voter to ONE candidate in ONE election.
 *     VotingService checks this model to prevent double-voting.
 *  3. IMMUTABILITY CONVENTION: Once a vote is cast and written to votes.txt, it acts 
 *     as a sealed ballot record that cannot be tampered with.
 */
public class Vote implements Identifiable {

    // --- Private Attributes ---
    private String voteId;            // Unique Ballot ID like "VOTE-1234ABCD"
    private String electionId;        // The election where the vote was cast (e.g. "E001")
    private String candidateId;       // The candidate receiving the vote (e.g. "C001")
    private String voterId;           // The voter who cast the ballot (e.g. "V001")
    private LocalDateTime timestamp;  // Exact date & time when the ballot was submitted

    /**
     * Parameterized Constructor.
     * If timestamp is null, defaults to LocalDateTime.now().
     */
    public Vote(String voteId, String electionId, String candidateId, String voterId, LocalDateTime timestamp) {
        this.voteId = voteId == null ? "" : voteId.trim();
        this.electionId = electionId == null ? "" : electionId.trim();
        this.candidateId = candidateId == null ? "" : candidateId.trim();
        this.voterId = voterId == null ? "" : voterId.trim();
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
    }

    // --- Getters and Setters (Encapsulation) ---

    public String getVoteId() {
        return voteId;
    }

    public void setVoteId(String voteId) {
        this.voteId = voteId == null ? "" : voteId.trim();
    }

    public String getElectionId() {
        return electionId;
    }

    public void setElectionId(String electionId) {
        this.electionId = electionId == null ? "" : electionId.trim();
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId == null ? "" : candidateId.trim();
    }

    public String getVoterId() {
        return voterId;
    }

    public void setVoterId(String voterId) {
        this.voterId = voterId == null ? "" : voterId.trim();
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
    }

    @Override
    public String getId() {
        return voteId;
    }

    @Override
    public String toString() {
        return "Vote{" +
                "voteId='" + voteId + '\'' +
                ", electionId='" + electionId + '\'' +
                ", candidateId='" + candidateId + '\'' +
                ", voterId='" + voterId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

