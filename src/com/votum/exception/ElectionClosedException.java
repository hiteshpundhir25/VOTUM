package com.votum.exception;

/**
 * ============================================================================
 * ElectionClosedException
 * ============================================================================
 * 
 * Course: CSC2104 - Object Oriented Programming (Java)
 * 
 * Thrown when an action (such as casting a ballot) is attempted on an election 
 * that is not currently active (i.e., status is DRAFT, CLOSED, or the current time 
 * is before startTime or after endTime).
 */
public class ElectionClosedException extends VotumException {
    private static final long serialVersionUID = 1L;

    public ElectionClosedException(String message) {
        super(message);
    }
}

