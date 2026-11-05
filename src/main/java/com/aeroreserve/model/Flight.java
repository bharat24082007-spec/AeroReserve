package com.aeroreserve.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Flight holds all the information about one scheduled flight.
 *
 * Key design points:
 *  - seats is an ArrayList<Seat> — the seat grid for this specific flight.
 *  - Helper methods on this class let services query and modify seats
 *    without reaching into the list directly.
 *
 * A Flight object is immutable except for its seats (which get booked/freed).
 */
public class Flight {

    // ── Fields ────────────────────────────────────────────────────────────────

    private String flightNo;       // e.g. "6E-2134"
    private String airline;        // e.g. "IndiGo"
    private String from;           // IATA city code, e.g. "HYD"
    private String to;             // IATA city code, e.g. "BOM"
    private String date;           // travel date as "YYYY-MM-DD"
    private String departureTime;  // e.g. "06:30"
    private String arrivalTime;    // e.g. "08:20"
    private double price;          // base fare in INR (Indian Rupees)

    // The seat grid — every Flight carries its own list of Seat objects
    private ArrayList<Seat> seats;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Creates a Flight with no seats yet.
     * Call generateSeats() right after construction to fill the seat grid.
     *
     * @param flightNo      unique flight number
     * @param airline       airline name shown as a text badge
     * @param from          origin city code
     * @param to            destination city code
     * @param date          date string "YYYY-MM-DD"
     * @param departureTime departure time "HH:MM"
     * @param arrivalTime   arrival time "HH:MM"
     * @param price         base price in INR
     */
    public Flight(String flightNo, String airline, String from, String to,
                  String date, String departureTime, String arrivalTime, double price) {
        this.flightNo      = flightNo;
        this.airline       = airline;
        this.from          = from;
        this.to            = to;
        this.date          = date;
        this.departureTime = departureTime;
        this.arrivalTime   = arrivalTime;
        this.price         = price;
        this.seats         = new ArrayList<>(); // empty list — filled by generateSeats()
    }

    // ── Seat generation ───────────────────────────────────────────────────────

    /**
     * Builds a standard 30-row, 6-column seat grid (180 seats total).
     *
     * Layout per row:
     *   Col A = WINDOW  |  B = MIDDLE  |  C = AISLE  || D = AISLE  |  E = MIDDLE  |  F = WINDOW
     *
     * Rows 1-3 are labelled EXTRA_LEGROOM.
     * Column letters A/F = WINDOW, B/E = MIDDLE, C/D = AISLE.
     *
     * This method is called once after the Flight is constructed.
     */
    public void generateSeats() {
        seats.clear(); // reset in case called twice

        String[] cols = {"A", "B", "C", "D", "E", "F"};

        for (int row = 1; row <= 30; row++) {
            for (String col : cols) {
                String seatNo = row + col; // e.g. "1A", "12F"

                // Decide the seat type based on row and column
                Seat.SeatType type;
                if (row <= 3) {
                    type = Seat.SeatType.EXTRA_LEGROOM; // first 3 rows are premium
                } else if (col.equals("A") || col.equals("F")) {
                    type = Seat.SeatType.WINDOW;
                } else if (col.equals("B") || col.equals("E")) {
                    type = Seat.SeatType.MIDDLE;
                } else {
                    type = Seat.SeatType.AISLE; // C and D
                }

                seats.add(new Seat(seatNo, type));
            }
        }
    }

    // ── Seat query helpers ────────────────────────────────────────────────────

    /**
     * Returns every seat on this flight (booked + available).
     *
     * @return the full seat ArrayList
     */
    public ArrayList<Seat> getAllSeats() {
        return seats;
    }

    /**
     * Returns only the seats that are still available (not yet booked).
     * Used by the seat-map screen to show which seats can be selected.
     *
     * @return a new list containing only available Seat objects
     */
    public List<Seat> getAvailableSeats() {
        List<Seat> available = new ArrayList<>();
        for (Seat s : seats) {
            if (!s.isBooked()) {
                available.add(s);
            }
        }
        return available;
    }

    /**
     * Finds and returns a single Seat by its label (e.g. "12A").
     * Returns null if no seat with that label exists on this flight.
     *
     * @param seatNo the seat label to look for
     * @return the matching Seat, or null
     */
    public Seat getSeatByNumber(String seatNo) {
        for (Seat s : seats) {
            if (s.getSeatNo().equals(seatNo)) {
                return s;
            }
        }
        return null; // not found
    }

    /**
     * Counts how many seats are still free on this flight.
     * Shown on the results screen next to each flight card.
     *
     * @return number of unbooked seats
     */
    public int getAvailableSeatCount() {
        int count = 0;
        for (Seat s : seats) {
            if (!s.isBooked()) {
                count++;
            }
        }
        return count;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Returns the flight number, e.g. "6E-2134". */
    public String getFlightNo() { return flightNo; }

    /** Returns the airline name, e.g. "IndiGo". */
    public String getAirline() { return airline; }

    /** Returns the origin city code, e.g. "HYD". */
    public String getFrom() { return from; }

    /** Returns the destination city code, e.g. "BOM". */
    public String getTo() { return to; }

    /** Returns the travel date as "YYYY-MM-DD". */
    public String getDate() { return date; }

    /** Returns the departure time as "HH:MM". */
    public String getDepartureTime() { return departureTime; }

    /** Returns the arrival time as "HH:MM". */
    public String getArrivalTime() { return arrivalTime; }

    /** Returns the base price in INR. */
    public double getPrice() { return price; }

    /** Returns all seats on this flight. */
    public ArrayList<Seat> getSeats() { return seats; }

    /** Returns the total capacity / seat count of this flight. */
    public int getTotalSeatCount() { return seats != null ? seats.size() : 0; }

    /**
     * Calculates the flight duration in minutes from departure and arrival times.
     * Used for sorting flights by fastest duration.
     *
     * @return duration in minutes
     */
    public int getDurationMinutes() {
        try {
            String[] depParts = departureTime.split(":");
            String[] arrParts = arrivalTime.split(":");
            int depMin = Integer.parseInt(depParts[0]) * 60 + Integer.parseInt(depParts[1]);
            int arrMin = Integer.parseInt(arrParts[0]) * 60 + Integer.parseInt(arrParts[1]);
            if (arrMin < depMin) {
                arrMin += 24 * 60; // Arrival past midnight
            }
            return arrMin - depMin;
        } catch (Exception e) {
            return 120; // Default 2h fallback
        }
    }

    /**
     * Returns a formatted duration string like "1h 50m".
     *
     * @return human-readable duration
     */
    public String getDurationString() {
        int totalMin = getDurationMinutes();
        int hrs = totalMin / 60;
        int mins = totalMin % 60;
        return hrs + "h " + mins + "m";
    }

    // ── Setters (needed for file-based data loading) ──────────────────────────

    /**
     * Replaces the seats list entirely.
     * Used by FileStorage when loading a flight's booked-seat state from disk.
     *
     * @param seats the restored seat list
     */
    public void setSeats(ArrayList<Seat> seats) {
        this.seats = seats;
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    /**
     * Returns a one-line summary of the flight.
     * Example: "6E-2134 | IndiGo | HYD -> BOM | 2025-07-10 | 06:30 -> 08:20 | INR 3450.0"
     */
    @Override
    public String toString() {
        return flightNo + " | " + airline + " | " + from + " -> " + to
                + " | " + date + " | " + departureTime + " -> " + arrivalTime
                + " | INR " + price;
    }
}

