package com.votum.exception;

/**
 * Thrown when election creation or modification fails validation
 * (e.g. blank name, end time before start time, invalid ID).
 */
public class InvalidElectionException extends VotumException {
    private static final long serialVersionUID = 1L;

    public InvalidElectionException(String message) {
        super(message);
    }
}
