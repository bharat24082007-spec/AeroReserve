package com.aeroreserve.exception;

/**
 * Thrown when someone tries to register with an email address that already exists.
 *
 * AuthService checks the users HashMap before creating a new account.
 * If the email key is already present it throws this exception instead of
 * silently overwriting the existing user — which would be a serious data bug.
 */
public class UserAlreadyExistsException extends Exception {

    /**
     * Creates the exception with a message identifying the duplicate email.
     *
     * @param message e.g. "An account with email rahul@example.com already exists."
     */
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}

