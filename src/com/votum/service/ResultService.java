package com.votum.service;

import com.votum.model.*;
import com.votum.storage.AuditLogger;
import com.votum.storage.FileManager;

import java.util.*;

/**
 * ============================================================================
 * ResultService (Tally, Turnout & Winner Determination Engine)
 * ============================================================================
 * 
 * Course: CSC2104 - Object Oriented Programming (Java)
 * 
 * Demonstrates:
 *  1. DATA PROCESSING WITH COLLECTIONS:
 *     - Iterates through the list of cast votes and tallies them per candidate.
 *     - Calculates student voter turnout percentage: (totalVotes / eligibleVoters) * 100.
 *  2. TIE DETECTION ALGORITHM:
 *     - Finds the highest vote count among contesting candidates.
 *     - Detects if two or more candidates have the same top score and marks isTie = true.
 *  3. AUDIT SEPARATION:
 *     - calculateResult(): Generates official results AND records an audit log entry.
 *     - previewResult(): Generates the exact same metrics WITHOUT writing to audit.log,
 *       preventing live dashboard auto-refresh timers from spamming the log file.
 */
public class ResultService {
    private final FileManager fileManager;
    private final AuditLogger auditLogger;
    private final ElectionService electionService;

    public ResultService(FileManager fileManager, AuditLogger auditLogger, ElectionService electionService) {
        this.fileManager = fileManager;
        this.auditLogger = auditLogger;
        this.electionService = electionService;
    }

    /**
     * Calculates complete election results for the given election ID and records an
     * audit entry, because the caller is declaring or displaying official results.
     */
    public ElectionResult calculateResult(String electionId) {
        return calculate(electionId, true);
    }

    /**
     * Calculates exactly the same numbers without writing an audit entry.
     * Used by live dashboard counters, which refresh often and must not flood the audit trail.
     */
    public ElectionResult previewResult(String electionId) {
        return calculate(electionId, false);
    }

    /**
     * Shared tally engine for {@link #calculateResult(String)} and {@link #previewResult(String)}.
     */
    private ElectionResult calculate(String electionId, boolean audit) {
        Election election = electionService.getElection(electionId);
        String electionName = (election != null) ? election.getName() : "Election (" + electionId + ")";
        ElectionResult result = new ElectionResult(electionId, electionName);

        // 1. Load candidates for this election
        List<Candidate> candidates = electionService.getCandidatesForElection(electionId);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Candidate c : candidates) {
            counts.put(c.getCandidateId(), 0);
        }

        // 2. Read votes and tally
        List<Vote> votes = fileManager.loadVotes();
        int totalVotes = 0;
        for (Vote v : votes) {
            if (v.getElectionId().equalsIgnoreCase(electionId)) {
                totalVotes++;
                String cId = v.getCandidateId();
                counts.put(cId, counts.getOrDefault(cId, 0) + 1);
            }
        }
        result.setTotalVotes(totalVotes);

        // Populate tallies into result object
        for (Candidate c : candidates) {
            result.addCandidateTally(c, counts.getOrDefault(c.getCandidateId(), 0));
        }

        // 3. Count eligible voters & calculate turnout
        List<Voter> allVoters = fileManager.loadVoters();
        int eligibleCount = 0;
        for (Voter v : allVoters) {
            if (v.isEligible()) {
                eligibleCount++;
            }
        }
        result.setEligibleVotersCount(eligibleCount);

        double turnout = (eligibleCount > 0) ? ((double) totalVotes / eligibleCount) * 100.0 : 0.0;
        // Round to 2 decimal places
        turnout = Math.round(turnout * 100.0) / 100.0;
        result.setTurnoutPercentage(turnout);

        // 4. Determine Winner(s) or Tie
        int maxVotes = -1;
        List<Candidate> topCandidates = new ArrayList<>();

        for (Candidate c : candidates) {
            int voteCount = counts.getOrDefault(c.getCandidateId(), 0);
            if (voteCount > maxVotes) {
                maxVotes = voteCount;
                topCandidates.clear();
                topCandidates.add(c);
            } else if (voteCount == maxVotes && maxVotes > 0) {
                topCandidates.add(c);
            }
        }

        if (totalVotes == 0 || maxVotes == 0) {
            result.setWinningCandidates(new ArrayList<>());
            result.setTie(false);
        } else if (topCandidates.size() > 1) {
            result.setWinningCandidates(topCandidates);
            result.setTie(true);
        } else {
            result.setWinningCandidates(topCandidates);
            result.setTie(false);
        }

        if (audit) {
            // getWinnerSummary() already names the outcome ('Winner: …', 'TIE between: …'),
            // so it is appended as-is instead of being labelled a second time.
            auditLogger.log("Results generated", "Election: " + electionId + " | Total Votes: " + totalVotes
                    + " | " + result.getWinnerSummary());
        }
        return result;
    }
}
