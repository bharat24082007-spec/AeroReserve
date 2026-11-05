package com.aeroreserve.model;

/**
 * Passenger holds the personal details of one traveller on a booking.
 *
 * A single Booking may have multiple Passenger objects — one per person travelling.
 * These details are entered by the user on the "Passenger Details" screen.
 */
public class Passenger {

    // ── Fields ────────────────────────────────────────────────────────────────

    private String name;    // full name as it appears on the ID
    private int    age;     // age in years
    private String gender;  // "Male", "Female", or "Other"
    private String phone;   // 10-digit mobile number
    private String email;   // contact email for the e-ticket

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Creates a Passenger with all required details.
     *
     * @param name   full name
     * @param age    age in years (must be > 0)
     * @param gender gender string
     * @param phone  contact phone number
     * @param email  contact email address
     */
    public Passenger(String name, int age, String gender, String phone, String email) {
        this.name   = name;
        this.age    = age;
        this.gender = gender;
        this.phone  = phone;
        this.email  = email;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Returns the passenger's full name. */
    public String getName() { return name; }

    /** Returns the passenger's age. */
    public int getAge() { return age; }

    /** Returns the passenger's gender string. */
    public String getGender() { return gender; }

    /** Returns the contact phone number. */
    public String getPhone() { return phone; }

    /** Returns the contact email address. */
    public String getEmail() { return email; }

    // ── Utility ──────────────────────────────────────────────────────────────

    /**
     * Returns a compact summary, useful for the ticket file.
     * Example: "Rahul Sharma | 28 | Male | 9876543210 | rahul@email.com"
     */
    @Override
    public String toString() {
        return name + " | " + age + " | " + gender + " | " + phone + " | " + email;
    }
}

