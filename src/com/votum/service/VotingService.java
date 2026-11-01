package com.votum.service;

import com.votum.exception.*;
import com.votum.model.*;
import com.votum.storage.AuditLogger;
import com.votum.storage.FileManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================================
 * VotingService (Ballot Casting & Democratic Rules Engine)
 * ============================================================================
 * 
 * Course: CSC2104 - Object Oriented Programming (Java)
 * 
 * This service is the core of VOTUM's business logic.
 * It strictly enforces all 8 democratic election rules:
 *  - Rule 1: Registration & Eligibility (Only registered, eligible voters can cast ballots).
 *  - Rule 2: Single-Vote Rule (Duplicate voting is blocked; one student, one vote).
 *  - Rule 3: Election Status (Must be OPEN, not DRAFT or CLOSED).
 *  - Rule 4: Start Window Check (Voting before start time is rejected with ElectionClosedException).
 *  - Rule 5: End Window Check (Voting after end time is rejected with ElectionClosedException).
 *  - Rule 6: Candidate Belonging (Selected candidate must belong to the contested election).
 *  - Rule 7: Admin Privileges (Handled by role-based access).
 *  - Rule 8: Closed Election Defense (Closed elections permanently reject new ballots).
 * 
 * Notice: Synchronization ('synchronized' keyword) is used on castVoteAt to prevent
 * race conditions where two simultaneous requests from the same user could slip through.
 */
public class VotingService {
    private final FileManager fileManager;
    private final AuditLogger auditLogger;
    private final ElectionService electionService;

    public VotingService(FileManager fileManager, AuditLogger auditLogger, ElectionService electionService) {
        this.fileManager = fileManager;
        this.auditLogger = auditLogger;
        this.electionService = electionService;
    }

    /**
     * Casts a vote using current system clock.
     */
    public Vote castVote(String voterId, String electionId, String candidateId) throws VotumException {
        return castVoteAt(voterId, electionId, candidateId, LocalDateTime.now());
    }

    /**
     * Casts a vote at a specific point in time (enables exact boundary testing).
     */
    public synchronized Vote castVoteAt(String voterId, String electionId, String candidateId, LocalDateTime currentTime)
            throws VotumException {

        if (voterId == null || voterId.trim().isEmpty()) {
            throw new VotumException("Voter ID is required.");
        }
        if (electionId == null || electionId.trim().isEmpty()) {
            throw new VotumException("Election ID is required.");
        }
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new InvalidCandidateException("Please select a valid candidate.");
        }

        // Rule 1: Check Voter Registration & Eligibility
        List<Voter> voters = fileManager.loadVoters();
        Voter currentVoter = null;
        for (Voter v : voters) {
            if (v.getVoterId().equalsIgnoreCase(voterId.trim())) {
                currentVoter = v;
                break;
            }
        }

        if (currentVoter == null) {
            throw new VotumException("Voter not found. You are not registered in the voting rolls.");
        }

        if (!currentVoter.isEligible()) {
            auditLogger.log("Vote rejected (Ineligible)", "Voter: " + voterId + " | Election: " + electionId);
            throw new VotumException("You are marked as ineligible to vote in this election.");
        }

        // Rule 2: Duplicate Voting is blocked per election, not globally, so that a voter
        // who has voted in one election is still able to vote in a different election.
        String targetVoterId = currentVoter.getVoterId();
        String targetElectionId = electionId.trim();

        if (currentVoter.hasVoted() && targetElectionId.equalsIgnoreCase(currentVoter.getVotedElectionId())) {
            auditLogger.log("Duplicate vote rejected", "Voter: " + targetVoterId + " | Election: " + targetElectionId);
            throw new DuplicateVoteException("Voting failed. You have already cast your vote in this election.");
        }

        // Cross-verify against the stored ballot records for this election (authoritative check)
        for (Vote v : fileManager.loadVotes()) {
            if (v.getElectionId().equalsIgnoreCase(targetElectionId) && v.getVoterId().equalsIgnoreCase(targetVoterId)) {
                auditLogger.log("Duplicate vote rejected (record exists)", "Voter: " + targetVoterId + " | Election: " + targetElectionId);
                throw new DuplicateVoteException("Voting failed. You have already voted in this election.");
            }
        }

        // Rule 3 & 8: Check Election Status
        Election election = electionService.getElection(targetElectionId);
        if (election == null) {
            throw new ElectionClosedException("Election '" + electionId + "' does not exist.");
        }

        if (election.getStatus() == ElectionStatus.DRAFT) {
            throw new ElectionClosedException("Voting is not open yet. This election is currently in DRAFT status.");
        }

        if (election.getStatus() == ElectionStatus.CLOSED) {
            auditLogger.log("Vote rejected (Closed)", "Voter: " + voterId + " | Election: " + electionId);
            throw new ElectionClosedException("Voting failed. This election is officially CLOSED.");
        }

        // Rule 4: Voting before start time
        if (election.isBeforeStart(currentTime)) {
            auditLogger.log("Vote rejected (Too early)", "Voter: " + voterId + " | Election: " + electionId);
            throw new ElectionClosedException("Voting has not started yet. Starts at: " + election.getStartTime());
        }

        // Rule 5: Voting after end time
        if (election.isAfterEnd(currentTime)) {
            auditLogger.log("Vote rejected (Too late)", "Voter: " + voterId + " | Election: " + electionId);
            throw new ElectionClosedException("Voting period has expired. Ended at: " + election.getEndTime());
        }

        // Rule 6: Candidate validation (Must exist and belong to target election)
        List<Candidate> candidates = electionService.getCandidatesForElection(targetElectionId);
        Candidate selectedCandidate = null;
        for (Candidate c : candidates) {
            if (c.getCandidateId().equalsIgnoreCase(candidateId.trim())) {
                selectedCandidate = c;
                break;
            }
        }

        if (selectedCandidate == null) {
            throw new InvalidCandidateException("Candidate ID '" + candidateId + "' is not registered for this election.");
        }

        // Generate unique Vote ID
        String voteId = "VOTE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Vote newVote = new Vote(voteId, election.getElectionId(), selectedCandidate.getCandidateId(),
                currentVoter.getVoterId(), currentTime);

        // Record the ballot first: the voter state flag must never be updated before the
        // ballot itself is safely on disk, otherwise a failed write would both lose the
        // vote and permanently block the voter from retrying.
        if (!fileManager.addVote(newVote)) {
            auditLogger.log("Vote persistence failed", "VoteID: " + voteId + " | Election: " + targetElectionId + " | Voter: " + targetVoterId);
            throw new VotumException("Your ballot could not be written to storage. Please submit again.");
        }

        // Update Voter state to prevent double voting in this election
        currentVoter.setHasVoted(true);
        currentVoter.setVotedElectionId(election.getElectionId());
        if (!fileManager.updateVoter(currentVoter)) {
            // The ballot is stored and the ballot-record check still blocks a repeat vote,
            // so the submission remains valid; only the cached voter flag is stale.
            auditLogger.log("Voter state update failed", "Voter: " + targetVoterId + " | Election: " + targetElectionId + " | Ballot: " + voteId);
        }

        auditLogger.log("Vote submitted", "VoteID: " + voteId + " | Election: " + targetElectionId + " | Voter: " + targetVoterId);
        return newVote;
    }

    /**
     * Checks if a specific voter has already voted in an election.
     */
    public boolean hasVoterCastVote(String voterId, String electionId) {
        if (voterId == null || voterId.trim().isEmpty()) {
            return false;
        }
        String targetVoterId = voterId.trim();
        String targetElectionId = (electionId == null) ? "" : electionId.trim();

        // Stored ballot records are authoritative and are always scoped to one election.
        for (Vote v : fileManager.loadVotes()) {
            if (v.getVoterId().equalsIgnoreCase(targetVoterId)
                    && (targetElectionId.isEmpty() || v.getElectionId().equalsIgnoreCase(targetElectionId))) {
                return true;
            }
        }

        // Fall back to the cached voter flag, but only for the election it was recorded against.
        for (Voter v : fileManager.loadVoters()) {
            if (v.getVoterId().equalsIgnoreCase(targetVoterId)
                    && v.hasVoted()
                    && !targetElectionId.isEmpty()
                    && targetElectionId.equalsIgnoreCase(v.getVotedElectionId())) {
                return true;
            }
        }
        return false;
    }
}
