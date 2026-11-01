package com.votum.exception;

/**
 * ============================================================================
 * InvalidLoginException
 * ============================================================================
 * 
 * Course: CSC2104 - Object Oriented Programming (Java)
 * 
 * Thrown by AuthenticationService when login credentials fail verification 
 * (e.g. wrong password, non-existent username, or blank inputs).
 */
public class InvalidLoginException extends VotumException {
    private static final long serialVersionUID = 1L;

    public InvalidLoginException(String message) {
        super(message);
    }
}

