package com.votum.exception;

/**
 * ============================================================================
 * VotumException (Base Checked Domain Exception)
 * ============================================================================
 * 
 * Course: CSC2104 - Object Oriented Programming (Java)
 * 
 * Demonstrates:
 *  1. CUSTOM EXCEPTION HIERARCHY:
 *     Extends java.lang.Exception to create a checked exception specifically 
 *     for the VOTUM voting system.
 *  2. INHERITANCE:
 *     All other domain-specific exceptions (InvalidLoginException, DuplicateVoteException,
 *     ElectionClosedException, etc.) inherit from this base class.
 *  3. ERROR HANDLING:
 *     Allows the UI layer to catch VotumException generically and display the message 
 *     in user-friendly Swing dialogs (JOptionPane).
 */
public class VotumException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor accepting an informative error message.
     */
    public VotumException(String message) {
        super(message);
    }

    /**
     * Constructor accepting both an error message and a nested root cause.
     */
    public VotumException(String message, Throwable cause) {
        super(message, cause);
    }
}

