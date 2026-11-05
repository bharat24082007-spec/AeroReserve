package com.aeroreserve.service;

import com.aeroreserve.exception.InvalidInputException;
import com.aeroreserve.exception.SeatNotAvailableException;
import com.aeroreserve.model.*;
import com.aeroreserve.model.Booking.BookingStatus;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * BookingService manages the creation, retrieval, and cancellation of bookings.
 *
 * Internal storage:
 *   HashMap<String, Booking> bookings — key is bookingRef (e.g. "ARB-00001")
 *
 * Booking reference format: "ARB-" + 5-digit counter, e.g. "ARB-00001"
 *
 * On startup it loads bookings from file and applies booked-seat states to flights.
 */
public class BookingService {

    // HashMap: bookingRef -> Booking object
    private HashMap<String, Booking> bookings;

    // Auto-incrementing counter used to generate booking references
    private int bookingCounter;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Loads all bookings from file and restores booked seat states on flights.
     *
     * Step 1: Load bookings (requires the flights map to link Flight objects).
     * Step 2: Restore booked seats — marks the right seats as taken on each flight.
     * Step 3: Figure out the next booking counter by scanning existing refs.
     *
     * @param flightService needed to get the flights map for restoring seat state
     */
    public BookingService(FlightService flightService) {
        // Load bookings from file; pass the flights map so we can link Flight objects
        this.bookings = FileStorage.loadBookings(flightService.getFlightsMap());

        // Restore booked seats on all flight objects
        FileStorage.applyBookedSeats(this.bookings);

        // Find the highest existing booking number so we don't reuse a ref
        this.bookingCounter = findMaxCounter() + 1;
    }

    // ── Create Booking ────────────────────────────────────────────────────────

    /**
     * Creates a new confirmed booking.
     *
     * Steps:
     *  1. Validate inputs (at least one passenger, at least one seat).
     *  2. For each requested seat, check it is still available.
     *  3. Mark each seat as booked on the flight.
     *  4. Calculate the total fare.
     *  5. Build and store the Booking object.
     *  6. Save to file.
     *
     * @param customerEmail  email of the logged-in customer
     * @param flight         the flight to book
     * @param passengers     list of Passenger objects (one per traveller)
     * @param seatNos        list of seat labels chosen by the user (e.g. ["12A", "12B"])
     * @param baggageOption  selected baggage option string (e.g. "15 kg")
     * @return the created Booking object
     * @throws SeatNotAvailableException if any chosen seat is already booked
     * @throws InvalidInputException     if passenger or seat list is empty
     */
    public Booking createBooking(String customerEmail, Flight flight,
                                 ArrayList<Passenger> passengers,
                                 ArrayList<String> seatNos,
                                 String baggageOption)
            throws SeatNotAvailableException, InvalidInputException {

        // ── Basic validation ──
        if (passengers == null || passengers.isEmpty()) {
            throw new InvalidInputException("At least one passenger is required.");
        }
        if (seatNos == null || seatNos.isEmpty()) {
            throw new InvalidInputException("Please select at least one seat.");
        }
        if (seatNos.size() != passengers.size()) {
            throw new InvalidInputException(
                "Number of seats must match number of passengers.");
        }

        // ── Check and collect seat objects ──
        ArrayList<Seat> selectedSeats = new ArrayList<>();
        for (String seatNo : seatNos) {
            Seat seat = flight.getSeatByNumber(seatNo);
            if (seat == null) {
                throw new InvalidInputException("Seat '" + seatNo + "' does not exist.");
            }
            if (seat.isBooked()) {
                // Throw custom exception — caught by the UI to show an error message
                throw new SeatNotAvailableException(
                    "Seat " + seatNo + " is already booked. Please choose another seat.");
            }
            selectedSeats.add(seat);
        }

        // ── Mark seats as booked on the flight object ──
        for (Seat seat : selectedSeats) {
            seat.setBooked(true);
        }

        // ── Calculate total fare ──
        double totalFare = calculateFare(flight, selectedSeats, baggageOption);

        // ── Generate unique booking reference ──
        String bookingRef = generateBookingRef();

        // ── Build the Booking object ──
        Booking booking = new Booking(
            bookingRef, flight, passengers,
            selectedSeats, baggageOption, totalFare, customerEmail
        );

        // ── Store and save ──
        bookings.put(bookingRef, booking);
        FileStorage.saveBookings(bookings);

        System.out.println("[BookingService] Booking created: " + bookingRef);
        return booking;
    }

    // ── Cancel Booking ────────────────────────────────────────────────────────

    /**
     * Cancels a booking and frees up the reserved seats.
     *
     * Sets the booking status to CANCELLED and marks each seat as available again
     * on the flight object so other users can book them.
     *
     * @param bookingRef the reference of the booking to cancel
     * @throws InvalidInputException if the booking ref does not exist or is already cancelled
     */
    public void cancelBooking(String bookingRef) throws InvalidInputException {
        Booking booking = bookings.get(bookingRef);

        if (booking == null) {
            throw new InvalidInputException(
                "No booking found with reference '" + bookingRef + "'.");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidInputException(
                "Booking " + bookingRef + " is already cancelled.");
        }

        // ── Mark status as CANCELLED ──
        booking.setStatus(BookingStatus.CANCELLED);

        // ── Free the seats on the flight (so others can book them) ──
        for (Seat seat : booking.getSeats()) {
            seat.setBooked(false);
        }

        // ── Save updated data ──
        FileStorage.saveBookings(bookings);
        System.out.println("[BookingService] Booking cancelled: " + bookingRef);
    }

    // ── Attach Payment ────────────────────────────────────────────────────────

    /**
     * Attaches a completed Payment to a booking and saves.
     * Called by the UI after PaymentService confirms the transaction.
     *
     * @param bookingRef the reference of the booking to update
     * @param payment    the successful Payment object
     */
    public void attachPayment(String bookingRef, Payment payment) {
        Booking booking = bookings.get(bookingRef);
        if (booking != null) {
            booking.setPayment(payment);
            FileStorage.saveBookings(bookings);   // persist payment record
            FileStorage.saveTicket(booking);      // write ticket text file
        }
    }

    // ── Retrieval ─────────────────────────────────────────────────────────────

    /**
     * Returns a single booking by its reference code.
     *
     * @param bookingRef the reference to look up
     * @return the Booking, or null if not found
     */
    public Booking getBooking(String bookingRef) {
        return bookings.get(bookingRef); // O(1) HashMap lookup
    }

    /**
     * Returns all bookings made by a specific customer.
     * Used on the "My Bookings" screen.
     *
     * @param email the customer's email
     * @return list of Booking objects belonging to that customer
     */
    public ArrayList<Booking> getBookingsByCustomer(String email) {
        ArrayList<Booking> result = new ArrayList<>();
        for (Booking b : bookings.values()) {
            if (b.getCustomerEmail().equalsIgnoreCase(email)) {
                result.add(b);
            }
        }
        return result;
    }

    /**
     * Returns all bookings in the system (admin view).
     *
     * @return list of all Booking objects
     */
    public ArrayList<Booking> getAllBookings() {
        return new ArrayList<>(bookings.values());
    }

    /**
     * Returns the total number of bookings.
     *
     * @return booking count
     */
    public int getBookingCount() {
        return bookings.size();
    }

    // ── Fare Calculation ──────────────────────────────────────────────────────

    /**
     * Calculates the total fare for a booking.
     *
     * Formula:
     *   base fare × number of seats
     *   + baggage surcharge
     *   + extra legroom surcharge (INR 500 per extra-legroom seat)
     *
     * Baggage prices:
     *   "No Baggage"  -> +0
     *   "15 kg"       -> +500
     *   "20 kg"       -> +800
     *   "25 kg"       -> +1200
     *
     * @param flight        the flight (holds the base price)
     * @param seats         the chosen seats (checked for extra-legroom type)
     * @param baggageOption the baggage choice string
     * @return total fare in INR
     */
    public static double calculateFare(Flight flight, ArrayList<Seat> seats,
                                       String baggageOption) {
        // Start with base fare × number of passengers
        double total = flight.getPrice() * seats.size();

        // Add baggage cost
        switch (baggageOption) {
            case "15 kg" -> total += 500.0;
            case "20 kg" -> total += 800.0;
            case "25 kg" -> total += 1200.0;
            // "No Baggage" or anything else -> 0 extra
        }

        // Extra legroom surcharge: INR 500 per such seat
        for (Seat s : seats) {
            if (s.getType() == Seat.SeatType.EXTRA_LEGROOM) {
                total += 500.0;
            }
        }

        return total;
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    /**
     * Saves all bookings to file.
     */
    public void save() {
        FileStorage.saveBookings(bookings);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Generates the next booking reference string in the format "ARB-00001".
     * Increments the counter after generating.
     *
     * @return a unique booking reference string
     */
    private String generateBookingRef() {
        // Format counter as 5-digit zero-padded number
        String ref = "ARB-" + String.format("%05d", bookingCounter);
        bookingCounter++; // move counter forward for the next booking
        return ref;
    }

    /**
     * Scans existing bookings to find the highest counter value.
     * This prevents reusing a ref after the app restarts.
     *
     * Example: "ARB-00007" -> extracts 7
     *
     * @return the highest counter found, or 0 if no bookings exist
     */
    private int findMaxCounter() {
        int max = 0;
        for (String ref : bookings.keySet()) {
            try {
                // Booking ref format: "ARB-NNNNN"
                int num = Integer.parseInt(ref.replace("ARB-", ""));
                if (num > max) max = num;
            } catch (NumberFormatException e) {
                // Ignore any ref that doesn't match the expected format
            }
        }
        return max;
    }
}

