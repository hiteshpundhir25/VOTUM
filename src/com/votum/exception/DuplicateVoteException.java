package com.votum.exception;

/**
 * ============================================================================
 * DuplicateVoteException
 * ============================================================================
 * 
 * Course: CSC2104 - Object Oriented Programming (Java)
 * 
 * Thrown by VotingService when a student attempts to vote more than once in the 
 * same election. This strictly enforces the core democratic rule: "One voter, one vote".
 */
public class DuplicateVoteException extends VotumException {
    private static final long serialVersionUID = 1L;

    public DuplicateVoteException(String message) {
        super(message);
    }
}

