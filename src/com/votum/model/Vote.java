package com.votum.model;

import java.time.LocalDateTime;

/**
 * Represents an individual vote cast in an election.
 * Stores references to the election, chosen candidate, voter identity, and timestamp.
 *
 * Implements Identifiable for uniform ID resolution.
 * Aligned with CSC2104 Course Outcomes CSC2104.2 & CSC2104.3.
 */
public class Vote implements Identifiable {
    private String voteId;
    private String electionId;
    private String candidateId;
    private String voterId;
    private LocalDateTime timestamp;

    public Vote(String voteId, String electionId, String candidateId, String voterId, LocalDateTime timestamp) {
        this.voteId = voteId == null ? "" : voteId.trim();
        this.electionId = electionId == null ? "" : electionId.trim();
        this.candidateId = candidateId == null ? "" : candidateId.trim();
        this.voterId = voterId == null ? "" : voterId.trim();
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
    }

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
