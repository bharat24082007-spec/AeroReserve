package com.aeroreserve.model;

/**
 * User is the base (parent) class for all people who use the system.
 * Customer and Admin both extend this class — that is inheritance + polymorphism.
 *
 * We use 'protected' fields so sub-classes can read them directly,
 * but code outside the package must go through getters — that is encapsulation.
 */
public abstract class User {

    // Every user has an email (used as the unique login key), a password, and a display name
    protected String email;
    protected String password;
    protected String name;

    /**
     * Constructor: called when we create a new User (or sub-class object).
     *
     * @param name     the user's full name
     * @param email    the user's email address (must be unique in the system)
     * @param password the user's plain-text password (kept simple for a college project)
     */
    public User(String name, String email, String password) {
        this.name     = name;
        this.email    = email;
        this.password = password;
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    /** Returns the user's display name. */
    public String getName() {
        return name;
    }

    /** Returns the user's email address. */
    public String getEmail() {
        return email;
    }

    /** Returns the stored password (used during login check). */
    public String getPassword() {
        return password;
    }

    // ── Abstract method (polymorphism) ───────────────────────────────────────

    /**
     * Every sub-class MUST implement getRole().
     * Customer returns "CUSTOMER", Admin returns "ADMIN".
     * This is the key polymorphic method — the same call behaves differently
     * depending on which type of User object we have at runtime.
     *
     * @return a string describing the user's role
     */
    public abstract String getRole();

    // ── Utility ─────────────────────────────────────────────────────────────

    /**
     * Returns a human-readable summary of the user.
     * Useful for debugging and console output.
     */
    @Override
    public String toString() {
        return "[" + getRole() + "] " + name + " <" + email + ">";
    }
}

