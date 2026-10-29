package com.votum.model;

/**
 * Represents the current status of an election in Votum.
 * 
 * DRAFT: Election created, candidates and voters being registered. Voting not allowed.
 * OPEN: Election is officially running and accepting votes within its time window.
 * CLOSED: Election is finished. No further votes allowed. Results can be declared.
 */
public enum ElectionStatus {
    DRAFT,
    OPEN,
    CLOSED
}
