package com.votum.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the calculated outcome of an election.
 * Stores vote tallies, turnout metrics, winner(s), and tie state.
 */
public class ElectionResult {
    private String electionId;
    private String electionName;
    private Map<String, Integer> candidateVoteCounts;
    private Map<String, Candidate> candidateDetails;
    private int totalVotes;
    private int eligibleVotersCount;
    private double turnoutPercentage;
    private List<Candidate> winningCandidates;
    private boolean isTie;

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

    public String getElectionId() {
        return electionId;
    }

    public String getElectionName() {
        return electionName;
    }

    public Map<String, Integer> getCandidateVoteCounts() {
        return Collections.unmodifiableMap(candidateVoteCounts);
    }

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
