package com.aeroreserve.model;

/**
 * Seat represents one physical seat on an aircraft.
 *
 * Every seat has:
 *  - a seat number  (e.g. "12A")
 *  - a type         (WINDOW, MIDDLE, AISLE, or EXTRA_LEGROOM)
 *  - a booked flag  (true = already taken, false = available)
 *
 * SeatType is a nested enum — an enum defined inside the Seat class — which
 * keeps related constants together and avoids a separate file.
 */
public class Seat {

    // ── Nested enum for seat categories ─────────────────────────────────────

    /**
     * SeatType labels the kind of seat.
     * We display a legend on the seat map using these values.
     */
    public enum SeatType {
        WINDOW,         // next to the window
        MIDDLE,         // between two other seats
        AISLE,          // next to the aisle
        EXTRA_LEGROOM   // front rows with more legroom (slightly pricier)
    }

    // ── Fields ───────────────────────────────────────────────────────────────

    private String   seatNo;    // e.g. "12A", "1B"
    private SeatType type;      // WINDOW / MIDDLE / AISLE / EXTRA_LEGROOM
    private boolean  isBooked;  // true if someone already holds this seat

    // ── Constructor ──────────────────────────────────────────────────────────

    /**
     * Creates a new Seat.  By default no seat is booked when a flight is created.
     *
     * @param seatNo the alphanumeric seat label (row + column letter)
     * @param type   the seat category
     */
    public Seat(String seatNo, SeatType type) {
        this.seatNo   = seatNo;
        this.type     = type;
        this.isBooked = false; // fresh seat — not yet taken
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Returns the seat label, e.g. "12A". */
    public String getSeatNo() {
        return seatNo;
    }

    /** Returns the seat category (WINDOW, MIDDLE, etc.). */
    public SeatType getType() {
        return type;
    }

    /** Returns true if the seat is already booked. */
    public boolean isBooked() {
        return isBooked;
    }

    // ── Setter ────────────────────────────────────────────────────────────────

    /**
     * Changes the booked status.
     * Called by BookingService when a user books or cancels a seat.
     *
     * @param booked pass true to mark the seat taken, false to free it
     */
    public void setBooked(boolean booked) {
        this.isBooked = booked;
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    /**
     * Returns a compact string for debugging, e.g. "12A[WINDOW, available]".
     */
    @Override
    public String toString() {
        return seatNo + "[" + type + ", " + (isBooked ? "booked" : "available") + "]";
    }
}

