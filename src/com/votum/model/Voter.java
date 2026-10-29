package com.votum.model;

/**
 * Represents a student voter in the VOTUM system.
 *
 * Demonstrates:
 *  - Inheritance: extends abstract class User via super constructor chaining.
 *  - Polymorphism: implements getDisplayName() and getId().
 *  - Encapsulation: maintains voter-roll eligibility and cached participation state.
 *
 * Aligned with CSC2104 Course Outcomes CSC2104.2 & CSC2104.3.
 */
public class Voter extends User {
    private String voterId;
    private String name;
    private String department;
    private boolean eligible;
    private boolean hasVoted;
    private String votedElectionId;

    public Voter(String voterId, String name, String department, String username, String password, boolean eligible, boolean hasVoted) {
        this(voterId, name, department, username, password, eligible, hasVoted, "");
    }

    public Voter(String voterId, String name, String department, String username, String password, boolean eligible, boolean hasVoted, String votedElectionId) {
        super(username, password, "VOTER");
        this.voterId = voterId == null ? "" : voterId.trim();
        this.name = name == null ? "" : name.trim();
        this.department = department == null ? "" : department.trim();
        this.eligible = eligible;
        this.hasVoted = hasVoted;
        this.votedElectionId = votedElectionId == null ? "" : votedElectionId.trim();
    }

    public String getVoterId() {
        return voterId;
    }

    public void setVoterId(String voterId) {
        this.voterId = voterId == null ? "" : voterId.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department == null ? "" : department.trim();
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public boolean hasVoted() {
        return hasVoted;
    }

    public void setHasVoted(boolean hasVoted) {
        this.hasVoted = hasVoted;
    }

    public String getVotedElectionId() {
        return votedElectionId;
    }

    public void setVotedElectionId(String votedElectionId) {
        this.votedElectionId = votedElectionId == null ? "" : votedElectionId.trim();
    }

    @Override
    public String getId() {
        return voterId;
    }

    @Override
    public String getDisplayName() {
        return name + " (" + department + ")";
    }

    @Override
    public String toString() {
        return "Voter{" +
                "voterId='" + voterId + '\'' +
                ", name='" + name + '\'' +
                ", department='" + department + '\'' +
                ", eligible=" + eligible +
                ", hasVoted=" + hasVoted +
                '}';
    }
}
