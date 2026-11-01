package com.votum.exception;

/**
 * ============================================================================
 * InvalidCandidateException
 * ============================================================================
 * 
 * Course: CSC2104 - Object Oriented Programming (Java)
 * 
 * Thrown when selecting a candidate that is unregistered, null, or does not 
 * belong to the contested election.
 */
public class InvalidCandidateException extends VotumException {
    private static final long serialVersionUID = 1L;

    public InvalidCandidateException(String message) {
        super(message);
    }
}

