package com.votum.model;

/**
 * ============================================================================
 * Voter (Student Voter Model)
 * ============================================================================
 * 
 * Course: CSC2104 - Object Oriented Programming (Java)
 * 
 * Demonstrates Core OOP Principles:
 *  1. INHERITANCE: 'Voter extends User' - Student voters inherit common account fields 
 *     (username, password, role) from User.
 *  2. CONSTRUCTOR OVERLOADING: Provides two constructors:
 *     - Constructor 1: for initial registration when votedElectionId is empty.
 *     - Constructor 2: full constructor when loading existing voting history from disk.
 *  3. CONSTRUCTOR CHAINING: Uses 'this(...)' to chain between constructors, and 
 *     'super(..., "VOTER")' to initialize the parent User class.
 *  4. ENCAPSULATION: Tracks eligibility (boolean eligible) and whether the student 
 *     has cast their ballot (boolean hasVoted, String votedElectionId).
 *  5. POLYMORPHISM: Overrides getId() to return voterId, and getDisplayName() to 
 *     return "Student Name (Department)".
 */
public class Voter extends User {

    // --- Private Attributes Specific to Student Voters ---
    private String voterId;          // Unique Roll ID (e.g., "V001")
    private String name;             // Student's full name
    private String department;       // Branch / Department (e.g. "Computer Science")
    private boolean eligible;        // True if permitted to vote; false if suspended/blacklisted
    private boolean hasVoted;        // Cache flag: true if already voted in current election
    private String votedElectionId;  // Stores the specific election ID voted in

    /**
     * Overloaded Constructor 1: Used when registering a fresh student voter.
     * Delegates to the full constructor below using this(...) with empty votedElectionId.
     */
    public Voter(String voterId, String name, String department, String username, String password, boolean eligible, boolean hasVoted) {
        this(voterId, name, department, username, password, eligible, hasVoted, "");
    }

    /**
     * Overloaded Constructor 2: Full constructor used when reading records from voters.txt.
     * Uses super(...) to set the role to "VOTER".
     */
    public Voter(String voterId, String name, String department, String username, String password, boolean eligible, boolean hasVoted, String votedElectionId) {
        super(username, password, "VOTER");
        this.voterId = voterId == null ? "" : voterId.trim();
        this.name = name == null ? "" : name.trim();
        this.department = department == null ? "" : department.trim();
        this.eligible = eligible;
        this.hasVoted = hasVoted;
        this.votedElectionId = votedElectionId == null ? "" : votedElectionId.trim();
    }

    // --- Getters and Setters (Encapsulation) ---

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

    // --- Polymorphic Method Implementations ---

    /**
     * Returns voterId as the unique primary key contract.
     */
    @Override
    public String getId() {
        return voterId;
    }

    /**
     * Returns a student-specific display name including their academic department.
     */
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

