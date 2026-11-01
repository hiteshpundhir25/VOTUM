package com.votum.model;

/**
 * ============================================================================
 * Candidate (Contesting Candidate Model)
 * ============================================================================
 * 
 * Course: CSC2104 - Object Oriented Programming (Java)
 * 
 * Demonstrates:
 *  1. ENCAPSULATION: All candidate details (ID, name, department, electionId, slogan) 
 *     are private fields with public getters and setters.
 *  2. CONSTRUCTOR OVERLOADING: Provides both a 4-argument constructor and a 5-argument 
 *     constructor (with campaign slogan).
 *  3. CONSTRUCTOR CHAINING: Uses 'this(..., "")' to chain between constructors.
 *  4. INTERFACE IMPLEMENTATION: Implements Identifiable so it can be handled 
 *     polymorphically across lookup maps and tables.
 */
public class Candidate implements Identifiable {

    // --- Private Attributes ---
    private String candidateId;  // Unique ID like "C001"
    private String name;         // Candidate's full name
    private String department;   // Department (e.g. "Computer Science")
    private String electionId;   // The election this candidate is contesting (e.g. "E001")
    private String slogan;       // Campaign slogan or manifesto summary

    /**
     * Overloaded Constructor 1: Initializes a candidate without an initial slogan.
     * Delegates to Constructor 2 with an empty string slogan.
     */
    public Candidate(String candidateId, String name, String department, String electionId) {
        this(candidateId, name, department, electionId, "");
    }

    /**
     * Overloaded Constructor 2: Full constructor with campaign slogan.
     */
    public Candidate(String candidateId, String name, String department, String electionId, String slogan) {
        this.candidateId = candidateId == null ? "" : candidateId.trim();
        this.name = name == null ? "" : name.trim();
        this.department = department == null ? "" : department.trim();
        this.electionId = electionId == null ? "" : electionId.trim();
        this.slogan = slogan == null ? "" : slogan;
    }

    // --- Getters and Setters (Encapsulation) ---

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

    // --- Identifiable Interface Implementation ---

    @Override
    public String getId() {
        return candidateId;
    }

    /**
     * Overridden toString method.
     * Formats candidate information neatly for display in dropdowns and lists.
     */
    @Override
    public String toString() {
        return name + " (" + candidateId + " - " + department + ")";
    }
}

