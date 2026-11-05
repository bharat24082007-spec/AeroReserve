package com.aeroreserve.exception;

/**
 * Thrown when the user enters data that fails validation.
 *
 * Examples:
 *  - Empty name field
 *  - Age entered as a negative number
 *  - Phone number that is not 10 digits
 *  - Invalid date format
 *
 * This keeps validation logic clean: instead of returning special error codes,
 * services throw this exception and the UI catches it to show an inline message.
 */
public class InvalidInputException extends Exception {

    /**
     * Creates the exception with a message describing what was wrong.
     *
     * @param message e.g. "Phone number must be exactly 10 digits."
     */
    public InvalidInputException(String message) {
        super(message);
    }
}

