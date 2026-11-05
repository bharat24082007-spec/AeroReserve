package com.aeroreserve.exception;

/**
 * Thrown when a flight lookup (by flight number or search criteria) returns nothing.
 *
 * FlightService throws this when:
 *  - An admin tries to remove a flight that does not exist.
 *  - A deep-link or restore operation references an unknown flight number.
 *
 * It is separate from "no results on search" (which returns an empty list, not an exception).
 */
public class FlightNotFoundException extends Exception {

    /**
     * Creates the exception with a message stating which flight was not found.
     *
     * @param message e.g. "Flight 6E-9999 does not exist in the system."
     */
    public FlightNotFoundException(String message) {
        super(message);
    }
}

