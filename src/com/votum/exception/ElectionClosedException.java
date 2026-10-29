package com.votum.exception;

/**
 * Thrown when an action is attempted on an election that is DRAFT, CLOSED,
 * or outside its valid start and end voting period.
 */
public class ElectionClosedException extends VotumException {
    private static final long serialVersionUID = 1L;

    public ElectionClosedException(String message) {
        super(message);
    }
}
