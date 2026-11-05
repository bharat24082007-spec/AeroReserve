package com.aeroreserve.model;

/**
 * Customer is a regular user who can search flights, book, and cancel.
 * It extends User — inheriting name, email, password — and simply declares
 * its role as "CUSTOMER".
 *
 * This class can be expanded later with loyalty points, travel history, etc.
 */
public class Customer extends User {

    /**
     * Constructor: passes all fields up to the parent User constructor.
     *
     * @param name     the customer's full name
     * @param email    the customer's email (login key)
     * @param password the customer's password
     */
    public Customer(String name, String email, String password) {
        super(name, email, password); // call the User constructor
    }

    /**
     * Implements the abstract method from User.
     * Every Customer object returns "CUSTOMER" when getRole() is called.
     *
     * @return the string "CUSTOMER"
     */
    @Override
    public String getRole() {
        return "CUSTOMER";
    }
}

