package com.votum.exception;

/**
 * Base checked exception class for all Votum domain errors.
 * Provides clear, friendly error messages suitable for display in UI dialogs.
 */
public class VotumException extends Exception {
    private static final long serialVersionUID = 1L;

    public VotumException(String message) {
        super(message);
    }

    public VotumException(String message, Throwable cause) {
        super(message, cause);
    }
}
