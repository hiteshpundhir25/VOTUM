package com.votum.exception;

/**
 * Thrown when an already-voted student attempts to cast a second ballot.
 * Enforces the core rule: "One voter, one vote".
 */
public class DuplicateVoteException extends VotumException {
    private static final long serialVersionUID = 1L;

    public DuplicateVoteException(String message) {
        super(message);
    }
}
