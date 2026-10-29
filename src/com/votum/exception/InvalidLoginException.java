package com.votum.exception;

/**
 * Thrown when user authentication fails due to bad credentials or unknown user.
 */
public class InvalidLoginException extends VotumException {
    private static final long serialVersionUID = 1L;

    public InvalidLoginException(String message) {
        super(message);
    }
}
