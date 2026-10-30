package com.votum.service;

import com.votum.exception.InvalidElectionException;
import com.votum.model.*;
import com.votum.storage.AuditLogger;
import com.votum.storage.FileManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the creation, scheduling, candidate registration, voter registration,
 * and state transitions of elections.
 */
public class ElectionService {
    private final FileManager fileManager;
    private final AuditLogger auditLogger;

    public ElectionService(FileManager fileManager, AuditLogger auditLogger) {
        this.fileManager = fileManager;
        this.auditLogger = auditLogger;
    }

    /**
     * Creates a new election with validation.
     */
    public Election createElection(String electionId, String name, String description,
                                   LocalDateTime startTime, LocalDateTime endTime) throws InvalidElectionException {
        if (electionId == null || electionId.trim().isEmpty()) {
            throw new InvalidElectionException("Election ID cannot be empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidElectionException("Election name cannot be empty.");
        }
        if (startTime == null || endTime == null) {
            throw new InvalidElectionException("Start time and End time must be specified.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new InvalidElectionException("Election End Time must be strictly after Start Time.");
        }

        List<Election> elections = fileManager.loadElections();
        for (Election e : elections) {
            if (e.getElectionId().equalsIgnoreCase(electionId.trim())) {
                throw new InvalidElectionException("An election with ID '" + electionId + "' already exists.");
            }
        }

        Election newElection = new Election(
                electionId.trim(),
                name.trim(),
                description == null ? "" : description.trim(),
                startTime,
                endTime,
                ElectionStatus.DRAFT
        );

        if (!fileManager.saveOrUpdateElection(newElection)) {
            throw new InvalidElectionException("Election '" + electionId + "' could not be written to storage. Please retry.");
        }
        auditLogger.log("Election created", "ID: " + newElection.getElectionId() + " | Name: " + newElection.getName());
        return newElection;
    }

    /**
     * Opens an election for voting.
     */
    public void openElection(String electionId) throws InvalidElectionException {
        Election election = getElection(electionId);
        if (election == null) {
            throw new InvalidElectionException("Election not found with ID: " + electionId);
        }
        election.setStatus(ElectionStatus.OPEN);
        if (!fileManager.saveOrUpdateElection(election)) {
            throw new InvalidElectionException("Election '" + electionId + "' could not be updated. Please retry.");
        }
        auditLogger.log("Election opened", "ID: " + election.getElectionId() + " | Name: " + election.getName());
    }

    /**
     * Closes an election to stop voting and finalize counts.
     */
    public void closeElection(String electionId) throws InvalidElectionException {
        Election election = getElection(electionId);
        if (election == null) {
            throw new InvalidElectionException("Election not found with ID: " + electionId);
        }
        election.setStatus(ElectionStatus.CLOSED);
        if (!fileManager.saveOrUpdateElection(election)) {
            throw new InvalidElectionException("Election '" + electionId + "' could not be updated. Please retry.");
        }
        auditLogger.log("Election closed", "ID: " + election.getElectionId() + " | Name: " + election.getName());
    }

    /**
     * Deletes an election along with its associated candidates and votes,
     * resets affected voter states, and records an audit log.
     */
    public boolean deleteElection(String electionId) throws InvalidElectionException {
        if (electionId == null || electionId.trim().isEmpty()) {
            throw new InvalidElectionException("Election ID cannot be empty.");
        }
        Election election = getElection(electionId);
        if (election == null) {
            throw new InvalidElectionException("Election not found with ID: " + electionId);
        }

        String electionName = election.getName();
        boolean deleted = fileManager.deleteElection(electionId.trim());
        if (deleted) {
            // Remove all historical logs referencing the deleted election
            auditLogger.removeLogsForElection(electionId.trim());
            auditLogger.log("Election deleted & data purged", "ID: " + electionId + " | Name: " + electionName + " | All associated candidates, ballots, and logs removed.");
        }
        return deleted;
    }

    /**
     * Resets the entire demonstration environment to the documented official baseline.
     * Restores default administrator, default election (E001 with active window),
     * default candidates, and default voters with cleared ballots.
     *
     * @return true if the demo reset succeeded, false otherwise
     */
    public boolean resetDemoEnvironment() {
        boolean success = fileManager.resetToDemoData();
        if (success) {
            auditLogger.log("Demo environment reset", "Restored official baseline dataset (E001, default candidates, default voters).");
        }
        return success;
    }

    public Election getElection(String electionId) {
        if (electionId == null) return null;
        for (Election e : fileManager.loadElections()) {
            if (e.getElectionId().equalsIgnoreCase(electionId.trim())) {
                return e;
            }
        }
        return null;
    }

    public List<Election> getAllElections() {
        return fileManager.loadElections();
    }

    /**
     * Retrieves the primary active or first available election.
     */
    public Election getActiveOrFirstElection() {
        List<Election> elections = fileManager.loadElections();
        for (Election e : elections) {
            if (e.getStatus() == ElectionStatus.OPEN) {
                return e;
            }
        }
        return elections.isEmpty() ? null : elections.get(0);
    }

    /**
     * Registers a new candidate for an election.
     */
    public Candidate addCandidate(String candidateId, String name, String department,
                                  String electionId, String slogan) throws InvalidElectionException {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new InvalidElectionException("Candidate ID cannot be blank.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidElectionException("Candidate Name cannot be blank.");
        }
        if (department == null || department.trim().isEmpty()) {
            throw new InvalidElectionException("Department cannot be blank.");
        }
        if (electionId == null || electionId.trim().isEmpty()) {
            throw new InvalidElectionException("Election ID cannot be blank.");
        }

        Election election = getElection(electionId);
        if (election == null) {
            throw new InvalidElectionException("Target election '" + electionId + "' does not exist.");
        }

        List<Candidate> candidates = fileManager.loadCandidates();
        for (Candidate c : candidates) {
            if (c.getCandidateId().equalsIgnoreCase(candidateId.trim())) {
                throw new InvalidElectionException("Candidate with ID '" + candidateId + "' already exists.");
            }
        }

        Candidate candidate = new Candidate(
                candidateId.trim(),
                name.trim(),
                department.trim(),
                electionId.trim(),
                slogan == null ? "" : slogan.trim()
        );

        if (!fileManager.addCandidate(candidate)) {
            throw new InvalidElectionException("Candidate '" + candidateId + "' could not be written to storage. Please retry.");
        }
        auditLogger.log("Candidate added", "ID: " + candidate.getCandidateId() + " | Name: " + candidate.getName()
                + " | Election: " + candidate.getElectionId());
        return candidate;
    }

    public List<Candidate> getAllCandidates() {
        return fileManager.loadCandidates();
    }

    public List<Candidate> getCandidatesForElection(String electionId) {
        List<Candidate> result = new ArrayList<>();
        if (electionId == null) return result;
        for (Candidate c : fileManager.loadCandidates()) {
            if (c.getElectionId().equalsIgnoreCase(electionId.trim())) {
                result.add(c);
            }
        }
        return result;
    }

    /**
     * Removes/disqualifies a candidate from an election, purges any cast ballots,
     * resets affected voter states, and records an audit log entry.
     */
    public boolean removeCandidate(String candidateId) throws InvalidElectionException {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new InvalidElectionException("Candidate ID cannot be blank.");
        }
        Candidate target = null;
        for (Candidate c : fileManager.loadCandidates()) {
            if (c.getCandidateId().equalsIgnoreCase(candidateId.trim())) {
                target = c;
                break;
            }
        }
        if (target == null) {
            throw new InvalidElectionException("Candidate not found with ID: " + candidateId);
        }

        boolean removed = fileManager.removeCandidate(candidateId.trim());
        if (removed) {
            auditLogger.log("Candidate removed", "ID: " + target.getCandidateId() + " | Name: "
                    + target.getName() + " | Election: " + target.getElectionId() + " | Ballots purged.");
        }
        return removed;
    }

    /**
     * Updates an existing candidate's profile (name, department, slogan) and logs the audit event.
     */
    public Candidate updateCandidate(String candidateId, String name, String department,
                                     String electionId, String slogan) throws InvalidElectionException {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new InvalidElectionException("Candidate ID cannot be blank.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidElectionException("Candidate Name cannot be blank.");
        }
        if (department == null || department.trim().isEmpty()) {
            throw new InvalidElectionException("Department cannot be blank.");
        }
        if (electionId == null || electionId.trim().isEmpty()) {
            throw new InvalidElectionException("Election ID cannot be blank.");
        }

        Candidate existing = null;
        for (Candidate c : fileManager.loadCandidates()) {
            if (c.getCandidateId().equalsIgnoreCase(candidateId.trim())) {
                existing = c;
                break;
            }
        }
        if (existing == null) {
            throw new InvalidElectionException("Candidate not found with ID: " + candidateId);
        }

        Candidate updated = new Candidate(
                candidateId.trim(),
                name.trim(),
                department.trim(),
                electionId.trim(),
                slogan == null ? "" : slogan.trim()
        );

        if (!fileManager.updateCandidate(updated)) {
            throw new InvalidElectionException("Candidate '" + candidateId + "' could not be updated in storage. Please retry.");
        }

        auditLogger.log("Candidate updated", "ID: " + updated.getCandidateId() + " | Name: " + updated.getName()
                + " | Election: " + updated.getElectionId() + " | Slogan: " + updated.getSlogan());
        return updated;
    }

    /**
     * Registers a new eligible student voter.
     */
    public Voter addVoter(String voterId, String name, String department,
                          String username, String password, boolean eligible) throws InvalidElectionException {
        if (voterId == null || voterId.trim().isEmpty()) {
            throw new InvalidElectionException("Voter ID cannot be blank.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidElectionException("Voter Name cannot be blank.");
        }
        if (department == null || department.trim().isEmpty()) {
            throw new InvalidElectionException("Department cannot be blank.");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new InvalidElectionException("Username cannot be blank.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new InvalidElectionException("Password cannot be blank.");
        }

        List<Voter> voters = fileManager.loadVoters();
        for (Voter v : voters) {
            if (v.getVoterId().equalsIgnoreCase(voterId.trim())) {
                throw new InvalidElectionException("Voter with ID '" + voterId + "' already exists.");
            }
            if (v.getUsername().equalsIgnoreCase(username.trim())) {
                throw new InvalidElectionException("Username '" + username + "' is already taken.");
            }
        }

        Voter voter = new Voter(
                voterId.trim(),
                name.trim(),
                department.trim(),
                username.trim(),
                password,
                eligible,
                false
        );

        // A student username must never collide with an administrator username, otherwise
        // the admin record would shadow the student at login and the account becomes unreachable.
        for (Admin admin : fileManager.loadAdmins()) {
            if (admin.getUsername().equalsIgnoreCase(username.trim())) {
                throw new InvalidElectionException("Username '" + username + "' is already used by an administrator account.");
            }
        }

        voters.add(voter);
        if (!fileManager.saveVoters(voters)) {
            throw new InvalidElectionException("Voter '" + voterId + "' could not be written to the voter roll. Please retry.");
        }
        auditLogger.log("Voter added", "ID: " + voter.getVoterId() + " | Name: " + voter.getName()
                + " | Eligible: " + voter.isEligible());
        return voter;
    }

    public List<Voter> getAllVoters() {
        return fileManager.loadVoters();
    }

    /**
     * Updates the eligibility of a registered student voter and logs the audit event.
     */
    public boolean setVoterEligibility(String voterId, boolean eligible) throws InvalidElectionException {
        if (voterId == null || voterId.trim().isEmpty()) {
            throw new InvalidElectionException("Voter ID cannot be blank.");
        }
        Voter target = null;
        for (Voter v : fileManager.loadVoters()) {
            if (v.getVoterId().equalsIgnoreCase(voterId.trim())) {
                target = v;
                break;
            }
        }
        if (target == null) {
            throw new InvalidElectionException("Voter not found with ID: " + voterId);
        }

        boolean updated = fileManager.setVoterEligibility(voterId.trim(), eligible);
        if (updated) {
            auditLogger.log("Voter eligibility updated", "ID: " + target.getVoterId() + " | Name: "
                    + target.getName() + " | Eligible: " + eligible);
        }
        return updated;
    }

    /**
     * Permanently deletes a voter from the voter roll, cascades ballot removal, and logs the audit event.
     */
    public boolean deleteVoter(String voterId) throws InvalidElectionException {
        if (voterId == null || voterId.trim().isEmpty()) {
            throw new InvalidElectionException("Voter ID cannot be blank.");
        }
        Voter target = null;
        for (Voter v : fileManager.loadVoters()) {
            if (v.getVoterId().equalsIgnoreCase(voterId.trim())) {
                target = v;
                break;
            }
        }
        if (target == null) {
            throw new InvalidElectionException("Voter not found with ID: " + voterId);
        }

        boolean deleted = fileManager.deleteVoter(voterId.trim());
        if (deleted) {
            auditLogger.log("Voter record deleted", "ID: " + target.getVoterId() + " | Name: "
                    + target.getName() + " | Username: " + target.getUsername() + " | Ballots purged.");
        }
        return deleted;
    }
}
