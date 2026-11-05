package com.aeroreserve.service;

import com.aeroreserve.exception.FlightNotFoundException;
import com.aeroreserve.exception.InvalidInputException;
import com.aeroreserve.model.Flight;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * FlightService manages all flight data.
 *
 * Internal storage:
 *   HashMap<String, Flight> flights — key is flightNo (e.g. "6E-2134")
 *   Using a HashMap means getFlightByNo() is O(1) — no looping needed.
 *
 * The service loads flights from file on startup.
 * Admins can add or remove flights; changes are persisted immediately.
 */
public class FlightService {

    // HashMap: flightNo -> Flight object
    private HashMap<String, Flight> flights;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Loads all flights from data/flights.csv on startup.
     * Each loaded flight also gets a fresh seat grid generated.
     * Booked seat states are applied later by BookingService.
     */
    public FlightService() {
        this.flights = FileStorage.loadFlights();
    }

    // ── Search ────────────────────────────────────────────────────────────────

    /**
     * Finds all flights matching the given route and date.
     *
     * Loops through the flights HashMap values and collects matches.
     * Also filters out flights with zero available seats.
     *
     * @param from the origin city code, e.g. "HYD"
     * @param to   the destination city code, e.g. "BOM"
     * @param date the travel date in "YYYY-MM-DD" format
     * @return list of matching Flight objects (may be empty if none found)
     */
    public ArrayList<Flight> searchFlights(String from, String to, String date) {
        ArrayList<Flight> results = new ArrayList<>();

        for (Flight f : flights.values()) {
            // Check all three criteria match (case-insensitive for city codes)
            boolean routeMatch = f.getFrom().equalsIgnoreCase(from)
                              && f.getTo().equalsIgnoreCase(to);
            boolean dateMatch  = f.getDate().equals(date);
            boolean hasSeats   = f.getAvailableSeatCount() > 0;

            if (routeMatch && dateMatch && hasSeats) {
                results.add(f);
            }
        }
        return results;
    }

    // ── Lookup ────────────────────────────────────────────────────────────────

    /**
     * Returns a specific Flight by its flight number.
     * HashMap.get() is O(1) — much faster than a linear search through a list.
     *
     * @param flightNo the flight number to look up, e.g. "6E-2134"
     * @return the matching Flight object
     * @throws FlightNotFoundException if no flight with that number exists
     */
    public Flight getFlightByNo(String flightNo) throws FlightNotFoundException {
        Flight f = flights.get(flightNo);
        if (f == null) {
            throw new FlightNotFoundException(
                "Flight '" + flightNo + "' does not exist in the system.");
        }
        return f;
    }

    // ── Admin operations ──────────────────────────────────────────────────────

    /**
     * Adds a new flight to the system (admin only).
     * Also generates the seat grid for the new flight.
     *
     * @param flight the Flight object to add
     * @throws InvalidInputException if a flight with the same number already exists
     */
    public void addFlight(Flight flight) throws InvalidInputException {
        if (flights.containsKey(flight.getFlightNo())) {
            throw new InvalidInputException(
                "Flight number '" + flight.getFlightNo() + "' already exists.");
        }
        flight.generateSeats(); // build fresh seat grid
        flights.put(flight.getFlightNo(), flight);
        FileStorage.saveFlights(flights); // persist to file
        System.out.println("[FlightService] Flight added: " + flight.getFlightNo());
    }

    /**
     * Removes a flight from the system (admin only).
     *
     * @param flightNo the flight number to remove
     * @throws FlightNotFoundException if the flight doesn't exist
     */
    public void removeFlight(String flightNo) throws FlightNotFoundException {
        if (!flights.containsKey(flightNo)) {
            throw new FlightNotFoundException(
                "Cannot remove — flight '" + flightNo + "' not found.");
        }
        flights.remove(flightNo);
        FileStorage.saveFlights(flights); // persist to file
        System.out.println("[FlightService] Flight removed: " + flightNo);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /**
     * Returns all flights as an ArrayList (useful for displaying in a list/table).
     *
     * @return list of all Flight objects
     */
    public ArrayList<Flight> getAllFlights() {
        return new ArrayList<>(flights.values());
    }

    /**
     * Returns the raw HashMap — needed by BookingService to restore booked seat states.
     *
     * @return the HashMap of flights keyed by flightNo
     */
    public HashMap<String, Flight> getFlightsMap() {
        return flights;
    }

    /**
     * Returns how many flights are currently in the system.
     *
     * @return total flight count
     */
    public int getFlightCount() {
        return flights.size();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    /**
     * Saves all flights to data/flights.csv.
     */
    public void save() {
        FileStorage.saveFlights(flights);
    }
}

