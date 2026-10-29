package com.votum.exception;

/**
 * Thrown when selecting a candidate that is null, unregistered,
 * or does not belong to the target election.
 */
public class InvalidCandidateException extends VotumException {
    private static final long serialVersionUID = 1L;

    public InvalidCandidateException(String message) {
        super(message);
    }
}
