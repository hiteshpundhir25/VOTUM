package com.votum.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * ElectionResult (Calculated Election Outcome Model)
 * ============================================================================
 * 
 * Course: CSC2104 - Object Oriented Programming (Java)
 * 
 * Demonstrates:
 *  1. ENCAPSULATION & DATA TRANSFER OBJECT (DTO):
 *     Bundles all statistics for an election (vote counts, total votes, turnout %, 
 *     winner list, tie flag) into a single clean object.
 *  2. COLLECTIONS FRAMEWORK:
 *     - LinkedHashMap: Preserves candidate insertion order for predictable display.
 *     - Collections.unmodifiableMap / Collections.unmodifiableList: Returns read-only 
 *       views so external callers cannot mutate internal election tallies.
 *  3. SUMMARY GENERATOR:
 *     - getWinnerSummary(): Evaluates counts and produces a friendly string:
 *       "No votes recorded yet", "Winner: <name> (X votes)", or "TIE between: <name>, <name>".
 */
public class ElectionResult {

    // --- Private Attributes ---
    private String electionId;
    private String electionName;
    private Map<String, Integer> candidateVoteCounts;  // Map of candidateId -> vote count
    private Map<String, Candidate> candidateDetails;   // Map of candidateId -> Candidate object
    private int totalVotes;                           // Total ballots cast in this election
    private int eligibleVotersCount;                   // Total eligible voters on the roll
    private double turnoutPercentage;                 // Turnout percentage: (totalVotes / eligible) * 100
    private List<Candidate> winningCandidates;        // List of candidate(s) with highest vote count
    private boolean isTie;                            // True if two or more candidates tied for first place

    /**
     * Parameterized Constructor.
     * Initializes all collection structures to prevent NullPointerExceptions.
     */
    public ElectionResult(String electionId, String electionName) {
        this.electionId = electionId;
        this.electionName = electionName;
        this.candidateVoteCounts = new LinkedHashMap<>();
        this.candidateDetails = new LinkedHashMap<>();
        this.winningCandidates = new ArrayList<>();
        this.totalVotes = 0;
        this.eligibleVotersCount = 0;
        this.turnoutPercentage = 0.0;
        this.isTie = false;
    }

    // --- Getters and Setters ---

    public String getElectionId() {
        return electionId;
    }

    public String getElectionName() {
        return electionName;
    }

    /**
     * Returns an unmodifiable map of candidate vote counts.
     * Protects internal data structure from outside tampering.
     */
    public Map<String, Integer> getCandidateVoteCounts() {
        return Collections.unmodifiableMap(candidateVoteCounts);
    }

    /**
     * Adds or updates a candidate tally.
     * Ensures negative votes are clamped to zero.
     */
    public void addCandidateTally(Candidate candidate, int votes) {
        if (candidate == null || candidate.getCandidateId() == null) {
            return;
        }
        this.candidateDetails.put(candidate.getCandidateId(), candidate);
        this.candidateVoteCounts.put(candidate.getCandidateId(), Math.max(votes, 0));
    }

    public Candidate getCandidate(String candidateId) {
        return candidateId == null ? null : candidateDetails.get(candidateId);
    }

    public int getVotesFor(String candidateId) {
        if (candidateId == null) {
            return 0;
        }
        return candidateVoteCounts.getOrDefault(candidateId, 0);
    }

    public int getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(int totalVotes) {
        this.totalVotes = totalVotes;
    }

    public int getEligibleVotersCount() {
        return eligibleVotersCount;
    }

    public void setEligibleVotersCount(int eligibleVotersCount) {
        this.eligibleVotersCount = eligibleVotersCount;
    }

    public double getTurnoutPercentage() {
        return turnoutPercentage;
    }

    public void setTurnoutPercentage(double turnoutPercentage) {
        this.turnoutPercentage = turnoutPercentage;
    }

    /**
     * Returns an unmodifiable list of winning candidates.
     */
    public List<Candidate> getWinningCandidates() {
        return Collections.unmodifiableList(winningCandidates);
    }

    public void setWinningCandidates(List<Candidate> winningCandidates) {
        this.winningCandidates = (winningCandidates == null)
                ? new ArrayList<>()
                : new ArrayList<>(winningCandidates);
    }

    public boolean isTie() {
        return isTie;
    }

    public void setTie(boolean tie) {
        isTie = tie;
    }

    /**
     * Generates a clear, readable text summary of the election outcome.
     * Used by ResultDialog and results banner in AdminDashboard.
     */
    public String getWinnerSummary() {
        if (totalVotes == 0) {
            return "No votes recorded yet.";
        }
        if (winningCandidates.isEmpty()) {
            return "No winner determined.";
        }
        if (isTie) {
            StringBuilder sb = new StringBuilder("TIE between: ");
            for (int i = 0; i < winningCandidates.size(); i++) {
                sb.append(winningCandidates.get(i).getName());
                if (i < winningCandidates.size() - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        } else if (!winningCandidates.isEmpty()) {
            return "Winner: " + winningCandidates.get(0).getName() + 
                   " (" + getVotesFor(winningCandidates.get(0).getCandidateId()) + " votes)";
        }
        return "No winner determined.";
    }
}

