package com.aeroreserve.service;

import com.aeroreserve.exception.InvalidInputException;
import com.aeroreserve.exception.UserAlreadyExistsException;
import com.aeroreserve.model.Admin;
import com.aeroreserve.model.Customer;
import com.aeroreserve.model.User;

import java.util.HashMap;

/**
 * AuthService manages all user accounts.
 *
 * It holds a HashMap<String, User> where the key is the user's email address.
 * Using a HashMap means looking up a user by email is O(1) — instant, no looping.
 *
 * On startup it loads existing users from the file.
 * After any change (register) it saves back to the file.
 */
public class AuthService {

    // HashMap: email -> User object (Customer or Admin)
    private HashMap<String, User> users;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Loads all users from data/users.csv on startup.
     * If the file doesn't exist yet, starts with an empty map.
     */
    public AuthService() {
        this.users = FileStorage.loadUsers();
    }

    // ── Authentication ────────────────────────────────────────────────────────

    /**
     * Verifies email + password and returns the matching User object.
     *
     * Steps:
     *  1. Check the email and password fields are not empty.
     *  2. Look up the email in the HashMap — O(1) lookup.
     *  3. Compare the password.
     *  4. Return the User if everything matches, or throw InvalidInputException.
     *
     * @param email    the email the user typed
     * @param password the password the user typed
     * @return the matching User (either Customer or Admin)
     * @throws InvalidInputException if email/password is blank or credentials don't match
     */
    public User login(String email, String password) throws InvalidInputException {
        // Basic blank-field check
        if (email == null || email.trim().isEmpty()) {
            throw new InvalidInputException("Email cannot be empty.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new InvalidInputException("Password cannot be empty.");
        }

        // Look up by email (HashMap O(1))
        User user = users.get(email.trim().toLowerCase());

        // Check if user exists and password matches
        if (user == null || !user.getPassword().equals(password)) {
            throw new InvalidInputException("Invalid email or password. Please try again.");
        }

        return user;
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Creates a new Customer account and saves it.
     *
     * Validation rules:
     *  - name, email, password must not be blank
     *  - email must contain '@' and '.'
     *  - password must be at least 6 characters
     *  - email must not already be registered
     *
     * @param name     the user's full name
     * @param email    the user's email (will be stored in lower-case)
     * @param password chosen password
     * @return the newly created Customer object
     * @throws InvalidInputException      if any field fails validation
     * @throws UserAlreadyExistsException if that email is already registered
     */
    public Customer register(String name, String email, String password)
            throws InvalidInputException, UserAlreadyExistsException {

        // ── Validate name ──
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Name cannot be empty.");
        }

        // ── Validate email ──
        if (email == null || email.trim().isEmpty()) {
            throw new InvalidInputException("Email cannot be empty.");
        }
        email = email.trim().toLowerCase(); // normalise to lower-case
        if (!email.contains("@") || !email.contains(".")) {
            throw new InvalidInputException("Please enter a valid email address.");
        }

        // ── Validate password ──
        if (password == null || password.length() < 6) {
            throw new InvalidInputException("Password must be at least 6 characters long.");
        }

        // ── Check for duplicate email ──
        if (users.containsKey(email)) {
            throw new UserAlreadyExistsException(
                "An account with email '" + email + "' already exists. Please log in.");
        }

        // ── Create and store the new customer ──
        Customer customer = new Customer(name.trim(), email, password);
        users.put(email, customer);
        FileStorage.saveUsers(users); // persist immediately
        return customer;
    }

    // ── Admin helper (used by SampleDataLoader) ───────────────────────────────

    /**
     * Adds an Admin account directly (bypasses user-facing validation).
     * Used only by SampleDataLoader to seed the default admin.
     *
     * @param name     admin's name
     * @param email    admin's email
     * @param password admin's password
     */
    public void addAdmin(String name, String email, String password) {
        // Only add if not already present (safe to call multiple times)
        if (!users.containsKey(email)) {
            users.put(email, new Admin(name, email, password));
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /**
     * Returns the full users map — used by the Admin panel to list all accounts.
     *
     * @return HashMap of all users keyed by email
     */
    public HashMap<String, User> getAllUsers() {
        return users;
    }

    /**
     * Checks whether any users are loaded (useful for debugging).
     *
     * @return number of registered users
     */
    public int getUserCount() {
        return users.size();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    /**
     * Saves the current users map to data/users.csv.
     * Call this after any change (register, etc.).
     */
    public void save() {
        FileStorage.saveUsers(users);
    }
}

