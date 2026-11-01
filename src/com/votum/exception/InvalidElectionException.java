package com.votum.exception;

/**
 * ============================================================================
 * InvalidElectionException
 * ============================================================================
 * 
 * Course: CSC2104 - Object Oriented Programming (Java)
 * 
 * Thrown when election creation, candidate registration, or voter registration fails validation 
 * (e.g., blank name, end time before start time, duplicate ID, invalid dates).
 */
public class InvalidElectionException extends VotumException {
    private static final long serialVersionUID = 1L;

    public InvalidElectionException(String message) {
        super(message);
    }
}

