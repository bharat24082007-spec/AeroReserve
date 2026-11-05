package com.aeroreserve.exception;

/**
 * Thrown when a user tries to book a seat that is already taken.
 *
 * We extend Exception (checked exception) so the compiler forces every caller
 * to either catch it or declare it in their method signature — good practice
 * for a reservation system where seat conflicts must always be handled.
 */
public class SeatNotAvailableException extends Exception {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message explains which seat is unavailable, e.g. "Seat 12A is already booked."
     */
    public SeatNotAvailableException(String message) {
        super(message); // pass the message up to the Exception parent class
    }
}

