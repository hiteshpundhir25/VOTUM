package com.votum.model;

/**
 * Interface representing any domain entity in the VOTUM system that possesses
 * a unique string identifier.
 *
 * Demonstrates CSC2104 Course Outcome CSC2104.3 (Interfaces & Polymorphism).
 */
public interface Identifiable {
    /**
     * Returns the unique string identifier of this entity.
     *
     * @return the unique identifier
     */
    String getId();
}

