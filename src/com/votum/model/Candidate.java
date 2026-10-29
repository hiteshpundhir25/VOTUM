package com.votum.model;

/**
 * Represents a student candidate contesting in an election.
 * Demonstrates encapsulation with private fields, constructor overloading, and Identifiable.
 *
 * Aligned with CSC2104 Course Outcomes CSC2104.2 & CSC2104.3.
 */
public class Candidate implements Identifiable {
    private String candidateId;
    private String name;
    private String department;
    private String electionId;
    private String slogan;

    public Candidate(String candidateId, String name, String department, String electionId) {
        this(candidateId, name, department, electionId, "");
    }

    public Candidate(String candidateId, String name, String department, String electionId, String slogan) {
        this.candidateId = candidateId == null ? "" : candidateId.trim();
        this.name = name == null ? "" : name.trim();
        this.department = department == null ? "" : department.trim();
        this.electionId = electionId == null ? "" : electionId.trim();
        this.slogan = slogan == null ? "" : slogan;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId == null ? "" : candidateId.trim();
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

    public String getElectionId() {
        return electionId;
    }

    public void setElectionId(String electionId) {
        this.electionId = electionId == null ? "" : electionId.trim();
    }

    public String getSlogan() {
        return slogan;
    }

    public void setSlogan(String slogan) {
        this.slogan = slogan == null ? "" : slogan;
    }

    @Override
    public String getId() {
        return candidateId;
    }

    @Override
    public String toString() {
        return name + " (" + candidateId + " - " + department + ")";
    }
}
