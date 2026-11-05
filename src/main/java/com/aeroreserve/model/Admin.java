package com.aeroreserve.model;

/**
 * Admin is a special user who can manage flights and view all bookings.
 * It extends User exactly like Customer does, but its getRole() returns "ADMIN".
 *
 * This demonstrates polymorphism: calling getRole() on a User reference gives
 * a different result depending on whether the actual object is a Customer or an Admin.
 */
public class Admin extends User {

    /**
     * Constructor: passes fields up to the parent User constructor.
     *
     * @param name     the admin's full name
     * @param email    the admin's email (login key)
     * @param password the admin's password
     */
    public Admin(String name, String email, String password) {
        super(name, email, password); // call the User constructor
    }

    /**
     * Implements the abstract method from User.
     * Returns "ADMIN", which the UI uses to decide which dashboard to show.
     *
     * @return the string "ADMIN"
     */
    @Override
    public String getRole() {
        return "ADMIN";
    }
}

