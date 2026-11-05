package com.aeroreserve.util;

import com.aeroreserve.model.Flight;
import com.aeroreserve.service.AuthService;
import com.aeroreserve.service.FileStorage;
import com.aeroreserve.service.FlightService;

import java.io.File;

/**
 * SampleDataLoader seeds the system with realistic sample data on the very first run.
 *
 * It creates:
 *  - 1 Admin account
 *  - 1 demo Customer account
 *  - 20 Indian domestic flights across 6 cities:
 *      HYD (Hyderabad), BOM (Mumbai), DEL (Delhi),
 *      BLR (Bengaluru), MAA (Chennai), CCU (Kolkata)
 *
 * Safety: checks if data/flights.csv already has content before seeding,
 * so this method is safe to call every time the app starts.
 */
public class SampleDataLoader {

    /**
     * Seeds users and flights if data files are empty or missing.
     * Call this once during application startup, right after services are created.
     *
     * @param authService   the AuthService to add seed users into
     * @param flightService the FlightService to add seed flights into
     */
    public static void seedData(AuthService authService, FlightService flightService) {

        // ── Seed users if missing or empty ──
        File usersFile = new File("data/users.csv");
        if (!usersFile.exists() || usersFile.length() == 0) {
            System.out.println("[SampleDataLoader] Seeding default admin and customer accounts...");
            seedUsers(authService);
        }

        // ── Seed flights if missing or empty ──
        File flightsFile = new File("data/flights.csv");
        if (!flightsFile.exists() || flightsFile.length() == 0) {
            System.out.println("[SampleDataLoader] Seeding 20 domestic flights across major routes...");
            seedFlights(flightService);
        }
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    /**
     * Creates one Admin and one demo Customer account.
     *
     * Admin:    admin@aeroreserve.com / admin123
     * Customer: demo@example.com      / demo123
     */
    private static void seedUsers(AuthService authService) {
        // Add admin using the special admin method (bypasses user-facing validation)
        authService.addAdmin("AeroReserve Admin", "admin@aeroreserve.com", "admin123");

        // Add demo customer via register — this also validates and saves
        try {
            authService.register("Demo Customer", "demo@example.com", "demo123");
        } catch (Exception e) {
            // If demo user already exists, ignore silently
        }

        // Save both to file
        authService.save();
        System.out.println("[SampleDataLoader] Users: admin@aeroreserve.com | demo@example.com");
    }

    // ── Flights ───────────────────────────────────────────────────────────────

    /**
     * Creates 20 realistic Indian domestic flights and adds them via FlightService.
     *
     * Airlines used:
     *   IndiGo (6E), Air India (AI), SpiceJet (SG), Vistara (UK), Go First (G8)
     *
     * Routes covered (and return legs):
     *   HYD-BOM, HYD-DEL, HYD-BLR, HYD-MAA
     *   BOM-DEL, BOM-BLR, BOM-MAA
     *   DEL-BLR, DEL-CCU
     *   BLR-MAA, MAA-CCU
     *   + a few returns / different timings
     */
    private static void seedFlights(FlightService flightService) {
        // Helper: build a flight and catch any duplicate-key exceptions silently
        addFlight(flightService, "6E-2134", "IndiGo",     "HYD", "BOM", "2025-12-15", "06:30", "08:20", 3450.0);
        addFlight(flightService, "AI-805",  "Air India",  "HYD", "DEL", "2025-12-15", "07:00", "09:30", 5200.0);
        addFlight(flightService, "SG-172",  "SpiceJet",   "HYD", "BLR", "2025-12-15", "08:15", "09:25", 2100.0);
        addFlight(flightService, "UK-837",  "Vistara",    "HYD", "MAA", "2025-12-15", "10:30", "11:45", 2800.0);
        addFlight(flightService, "6E-5312", "IndiGo",     "BOM", "DEL", "2025-12-15", "06:00", "08:15", 4100.0);
        addFlight(flightService, "AI-657",  "Air India",  "BOM", "BLR", "2025-12-15", "09:45", "11:10", 3200.0);
        addFlight(flightService, "SG-891",  "SpiceJet",   "DEL", "BLR", "2025-12-15", "07:30", "10:00", 4800.0);
        addFlight(flightService, "UK-514",  "Vistara",    "DEL", "CCU", "2025-12-15", "11:00", "13:15", 3900.0);
        addFlight(flightService, "G8-301",  "Go First",   "BLR", "MAA", "2025-12-15", "06:45", "07:50", 1800.0);
        addFlight(flightService, "AI-765",  "Air India",  "MAA", "CCU", "2025-12-15", "08:00", "10:30", 4300.0);
        addFlight(flightService, "6E-3421", "IndiGo",     "HYD", "BOM", "2025-12-15", "14:00", "15:50", 3200.0);
        addFlight(flightService, "SG-445",  "SpiceJet",   "HYD", "DEL", "2025-12-15", "15:30", "18:00", 4700.0);
        addFlight(flightService, "UK-209",  "Vistara",    "BOM", "DEL", "2025-12-15", "13:00", "15:10", 4500.0);
        addFlight(flightService, "AI-401",  "Air India",  "BLR", "DEL", "2025-12-15", "09:00", "11:40", 5100.0);
        addFlight(flightService, "6E-7823", "IndiGo",     "CCU", "BOM", "2025-12-15", "06:15", "09:30", 4600.0);
        addFlight(flightService, "G8-512",  "Go First",   "MAA", "BOM", "2025-12-15", "07:00", "09:15", 3100.0);
        addFlight(flightService, "SG-234",  "SpiceJet",   "BOM", "MAA", "2025-12-15", "10:00", "12:10", 2900.0);
        addFlight(flightService, "UK-650",  "Vistara",    "DEL", "HYD", "2025-12-15", "08:30", "10:45", 4200.0);
        addFlight(flightService, "AI-203",  "Air India",  "BLR", "BOM", "2025-12-15", "16:00", "17:50", 3600.0);
        addFlight(flightService, "6E-9981", "IndiGo",     "CCU", "DEL", "2025-12-15", "05:45", "07:30", 3800.0);

        // Save all flights to file
        flightService.save();
        System.out.println("[SampleDataLoader] 20 flights saved to data/flights.csv");
    }

    /**
     * Helper method: adds a single flight using FlightService.addFlight().
     * Catches and prints any exception so one bad flight doesn't block the rest.
     */
    private static void addFlight(FlightService svc,
                                   String flightNo, String airline,
                                   String from,     String to,
                                   String date,     String dep, String arr,
                                   double price) {
        try {
            Flight f = new Flight(flightNo, airline, from, to, date, dep, arr, price);
            svc.addFlight(f); // addFlight also calls generateSeats() internally
        } catch (Exception e) {
            System.out.println("[SampleDataLoader] Could not add " + flightNo + ": " + e.getMessage());
        }
    }
}

