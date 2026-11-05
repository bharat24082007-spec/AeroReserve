package com.aeroreserve;

import com.aeroreserve.exception.PaymentFailedException;
import com.aeroreserve.model.*;
import com.aeroreserve.model.Payment.PaymentMethod;
import com.aeroreserve.service.*;
import com.aeroreserve.util.SampleDataLoader;

import java.util.ArrayList;

/**
 * Main — a small console program to test Stage 2 (services + file persistence).
 *
 * This class will NOT be used once the JavaFX UI is added in Stage 3.
 * It exists only to verify that every service works correctly and that
 * data is properly saved to and loaded from the /data CSV files.
 *
 * Run with:  mvn compile exec:java -Dexec.mainClass=com.aeroreserve.Main
 */
public class Main {

    /**
     * Entry point. Creates all services, seeds data, then runs a series of tests.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {

        System.out.println("=".repeat(55));
        System.out.println("   AeroReserve — Stage 2 Console Test");
        System.out.println("=".repeat(55));

        // ── Step 1: Ensure the data/ directory exists ──
        FileStorage.createDataDirectory();

        // ── Step 2: Initialise services ──
        // Order matters: FlightService must come before BookingService
        AuthService    authService    = new AuthService();
        FlightService  flightService  = new FlightService();
        BookingService bookingService = new BookingService(flightService); // loads bookings + fixes seat states
        PaymentService paymentService = new PaymentService();

        // ── Step 3: Seed sample data (skipped if already seeded) ──
        SampleDataLoader.seedData(authService, flightService);

        // ═══════════════════════════════════════════════════════════
        // TEST A — Login
        // ═══════════════════════════════════════════════════════════
        System.out.println("\n--- TEST A: Login ---");
        testLogin(authService);

        // ═══════════════════════════════════════════════════════════
        // TEST B — List all flights
        // ═══════════════════════════════════════════════════════════
        System.out.println("\n--- TEST B: All Flights ---");
        ArrayList<Flight> allFlights = flightService.getAllFlights();
        System.out.println("Total flights loaded: " + allFlights.size());
        for (Flight f : allFlights) {
            System.out.println("  " + f);
        }

        // ═══════════════════════════════════════════════════════════
        // TEST C — Search flights (HYD -> BOM on 2025-12-15)
        // ═══════════════════════════════════════════════════════════
        System.out.println("\n--- TEST C: Search HYD -> BOM on 2025-12-15 ---");
        ArrayList<Flight> results = flightService.searchFlights("HYD", "BOM", "2025-12-15");
        System.out.println("Found " + results.size() + " flight(s):");
        for (Flight f : results) {
            System.out.println("  " + f);
            System.out.println("    Available seats: " + f.getAvailableSeatCount());
        }

        // ═══════════════════════════════════════════════════════════
        // TEST D — Create a booking on the first search result
        // ═══════════════════════════════════════════════════════════
        System.out.println("\n--- TEST D: Create Booking ---");
        Booking newBooking = null;
        if (!results.isEmpty()) {
            Flight chosenFlight = results.get(0);

            // Build one passenger
            ArrayList<Passenger> passengers = new ArrayList<>();
            passengers.add(new Passenger("Rahul Sharma", 28, "Male", "9876543210", "rahul@example.com"));

            // Choose seat 10A (should be available on a fresh flight)
            ArrayList<String> seatNos = new ArrayList<>();
            seatNos.add("10A");

            try {
                newBooking = bookingService.createBooking(
                    "demo@example.com",  // customer email
                    chosenFlight,
                    passengers,
                    seatNos,
                    "15 kg"              // baggage option
                );
                System.out.println("Booking created successfully!");
                System.out.println(newBooking);

                // Confirm seat is now marked as booked
                Seat s = chosenFlight.getSeatByNumber("10A");
                System.out.println("Seat 10A booked flag: " + s.isBooked()); // should be true

            } catch (Exception e) {
                System.out.println("Booking failed: " + e.getMessage());
            }
        }

        // ═══════════════════════════════════════════════════════════
        // TEST E — Simulate payment
        // ═══════════════════════════════════════════════════════════
        System.out.println("\n--- TEST E: Simulate Payment ---");
        if (newBooking != null) {
            boolean paid = false;
            int attempts = 0;
            while (!paid && attempts < 5) {
                attempts++;
                try {
                    Payment payment = paymentService.processPayment(
                        PaymentMethod.CREDIT_CARD, newBooking.getTotalFare()
                    );
                    // Attach payment to booking and save ticket file
                    bookingService.attachPayment(newBooking.getBookingRef(), payment);
                    System.out.println("Payment SUCCESS on attempt " + attempts);
                    System.out.println("Ticket file saved to data/ticket_"
                                       + newBooking.getBookingRef() + ".txt");
                    paid = true;
                } catch (PaymentFailedException e) {
                    System.out.println("Attempt " + attempts + " failed: " + e.getMessage());
                }
            }
        }

        // ═══════════════════════════════════════════════════════════
        // TEST F — Cancel the booking; verify seats are freed
        // ═══════════════════════════════════════════════════════════
        System.out.println("\n--- TEST F: Cancel Booking ---");
        if (newBooking != null) {
            try {
                String ref = newBooking.getBookingRef();
                bookingService.cancelBooking(ref);
                System.out.println("Booking " + ref + " cancelled.");

                // Verify seat 10A is free again
                Flight f = results.get(0);
                Seat s = f.getSeatByNumber("10A");
                System.out.println("Seat 10A booked flag after cancel: " + s.isBooked()); // false

            } catch (Exception e) {
                System.out.println("Cancel failed: " + e.getMessage());
            }
        }

        // ═══════════════════════════════════════════════════════════
        // TEST G — Reload services from disk (simulate restart)
        // ═══════════════════════════════════════════════════════════
        System.out.println("\n--- TEST G: Reload from disk (simulating app restart) ---");
        FlightService  fs2 = new FlightService();
        BookingService bs2 = new BookingService(fs2);
        FileStorage.applyBookedSeats(new java.util.HashMap<>() {{
            bs2.getAllBookings().forEach(b -> put(b.getBookingRef(), b));
        }});
        System.out.println("Flights reloaded: "  + fs2.getFlightCount());
        System.out.println("Bookings reloaded: " + bs2.getBookingCount());
        System.out.println("All bookings after reload:");
        for (Booking b : bs2.getAllBookings()) {
            System.out.println("  " + b.getBookingRef()
                + " | " + b.getStatus()
                + " | " + b.getFlight().getFlightNo()
                + " | INR " + b.getTotalFare());
        }

        System.out.println("\n" + "=".repeat(55));
        System.out.println("   Stage 2 test complete — check data/ folder");
        System.out.println("=".repeat(55));
    }

    // ── Helper: test login scenarios ──────────────────────────────────────────

    /**
     * Tests valid and invalid login cases against AuthService.
     *
     * @param authService the AuthService to test against
     */
    private static void testLogin(AuthService authService) {
        // Valid admin login
        try {
            User u = authService.login("admin@aeroreserve.com", "admin123");
            System.out.println("Admin login OK:    " + u);
        } catch (Exception e) {
            System.out.println("Admin login FAIL: " + e.getMessage());
        }

        // Valid customer login
        try {
            User u = authService.login("demo@example.com", "demo123");
            System.out.println("Customer login OK: " + u);
        } catch (Exception e) {
            System.out.println("Customer login FAIL: " + e.getMessage());
        }

        // Wrong password
        try {
            authService.login("demo@example.com", "wrongpass");
            System.out.println("Should NOT reach here");
        } catch (Exception e) {
            System.out.println("Wrong password caught correctly: " + e.getMessage());
        }
    }
}

