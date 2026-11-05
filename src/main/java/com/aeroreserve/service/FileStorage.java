package com.aeroreserve.service;

import com.aeroreserve.model.*;
import com.aeroreserve.model.Booking.BookingStatus;
import com.aeroreserve.model.Payment.PaymentMethod;
import com.aeroreserve.model.Payment.PaymentStatus;
import com.aeroreserve.model.Seat.SeatType;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * FileStorage handles ALL reading and writing of data to/from CSV files in the /data folder.
 *
 * Files used:
 *   data/users.csv      — one user per line
 *   data/flights.csv    — one flight per line
 *   data/bookings.csv   — one booking per line
 *   data/passengers.csv — one passenger per line (multiple rows share a bookingRef)
 *
 * CSV separator: comma ','
 * Sub-list separator (e.g. seat numbers): pipe '|'
 *
 * Every method is static — no need to create a FileStorage object.
 */
public class FileStorage {

    // ── File path constants ───────────────────────────────────────────────────

    private static final String DATA_DIR       = "data/";
    private static final String USERS_FILE     = "data/users.csv";
    private static final String FLIGHTS_FILE   = "data/flights.csv";
    private static final String BOOKINGS_FILE  = "data/bookings.csv";
    private static final String PASSENGERS_FILE = "data/passengers.csv";

    // ── Setup ─────────────────────────────────────────────────────────────────

    /**
     * Creates the /data directory if it does not already exist.
     * Called once at application startup.
     */
    public static void createDataDirectory() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs(); // creates all missing parent folders too
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  USERS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Writes all users to data/users.csv.
     * Format per line: email,password,name,role
     *
     * @param users the HashMap of users keyed by email
     */
    public static void saveUsers(HashMap<String, User> users) {
        try (FileWriter fw = new FileWriter(USERS_FILE)) {
            for (User u : users.values()) {
                // Build one CSV line per user
                String line = u.getEmail() + ","
                            + u.getPassword() + ","
                            + u.getName() + ","
                            + u.getRole();
                fw.write(line + "\n");
            }
        } catch (IOException e) {
            System.out.println("[FileStorage] ERROR saving users: " + e.getMessage());
        }
    }

    /**
     * Reads data/users.csv and rebuilds the users HashMap.
     * Creates a Customer or Admin object based on the 'role' column.
     *
     * @return HashMap keyed by email, or empty map if file doesn't exist
     */
    public static HashMap<String, User> loadUsers() {
        HashMap<String, User> users = new HashMap<>();
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            return users; // no file yet — return empty map
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue; // skip blank lines

                // Split into exactly 4 parts: email, password, name, role
                String[] parts = line.split(",", 4);
                if (parts.length < 4) continue; // skip malformed lines

                String email    = parts[0];
                String password = parts[1];
                String name     = parts[2];
                String role     = parts[3];

                // Reconstruct the right subclass based on role
                if (role.equals("ADMIN")) {
                    users.put(email, new Admin(name, email, password));
                } else {
                    users.put(email, new Customer(name, email, password));
                }
            }
        } catch (IOException e) {
            System.out.println("[FileStorage] ERROR loading users: " + e.getMessage());
        }
        return users;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FLIGHTS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Writes all flights to data/flights.csv.
     * Format per line: flightNo,airline,from,to,date,departureTime,arrivalTime,price
     *
     * We do NOT save individual seat states here — those are derived from bookings when loading.
     *
     * @param flights HashMap of flights keyed by flightNo
     */
    public static void saveFlights(HashMap<String, Flight> flights) {
        try (FileWriter fw = new FileWriter(FLIGHTS_FILE)) {
            for (Flight f : flights.values()) {
                String line = f.getFlightNo() + ","
                            + f.getAirline()  + ","
                            + f.getFrom()     + ","
                            + f.getTo()       + ","
                            + f.getDate()     + ","
                            + f.getDepartureTime() + ","
                            + f.getArrivalTime()   + ","
                            + f.getPrice();
                fw.write(line + "\n");
            }
        } catch (IOException e) {
            System.out.println("[FileStorage] ERROR saving flights: " + e.getMessage());
        }
    }

    /**
     * Reads data/flights.csv and rebuilds the flights HashMap.
     * Calls generateSeats() on each flight so the seat grid is fresh and all seats are available.
     * Booked seats are restored separately by applyBookedSeats() after bookings are loaded.
     *
     * @return HashMap keyed by flightNo, or empty map if file doesn't exist
     */
    public static HashMap<String, Flight> loadFlights() {
        HashMap<String, Flight> flights = new HashMap<>();
        File file = new File(FLIGHTS_FILE);
        if (!file.exists()) {
            return flights;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] p = line.split(",", 8); // 8 columns
                if (p.length < 8) continue;

                Flight f = new Flight(
                    p[0],                    // flightNo
                    p[1],                    // airline
                    p[2],                    // from
                    p[3],                    // to
                    p[4],                    // date
                    p[5],                    // departureTime
                    p[6],                    // arrivalTime
                    Double.parseDouble(p[7]) // price
                );
                f.generateSeats(); // build fresh seat grid — all available
                flights.put(f.getFlightNo(), f);
            }
        } catch (IOException e) {
            System.out.println("[FileStorage] ERROR loading flights: " + e.getMessage());
        }
        return flights;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BOOKINGS  (bookings.csv + passengers.csv)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Writes all bookings to data/bookings.csv AND all passengers to data/passengers.csv.
     *
     * bookings.csv format:
     *   bookingRef,flightNo,customerEmail,seatNos(pipe-sep),baggageOption,
     *   totalFare,status,paymentMethod,paymentStatus,paymentAmount
     *
     * passengers.csv format:
     *   bookingRef,name,age,gender,phone,email
     *
     * @param bookings HashMap of bookings keyed by bookingRef
     */
    public static void saveBookings(HashMap<String, Booking> bookings) {
        try (FileWriter bw = new FileWriter(BOOKINGS_FILE);
             FileWriter pw = new FileWriter(PASSENGERS_FILE)) {

            for (Booking b : bookings.values()) {

                // ── Build the seat numbers string (pipe-separated) ──
                StringBuilder seatStr = new StringBuilder();
                for (int i = 0; i < b.getSeats().size(); i++) {
                    if (i > 0) seatStr.append("|");
                    seatStr.append(b.getSeats().get(i).getSeatNo());
                }

                // ── Payment fields (may be absent) ──
                String payMethod  = "";
                String payStatus  = "";
                String payAmount  = "0.0";
                if (b.getPayment() != null) {
                    payMethod = b.getPayment().getMethod().name();
                    payStatus = b.getPayment().getStatus().name();
                    payAmount = String.valueOf(b.getPayment().getAmount());
                }

                // ── Write booking line ──
                String bookingLine =
                    b.getBookingRef()          + "," +
                    b.getFlight().getFlightNo()+ "," +
                    b.getCustomerEmail()       + "," +
                    seatStr                    + "," +
                    b.getBaggageOption()       + "," +
                    b.getTotalFare()           + "," +
                    b.getStatus().name()       + "," +
                    payMethod                  + "," +
                    payStatus                  + "," +
                    payAmount;
                bw.write(bookingLine + "\n");

                // ── Write one passenger line per passenger ──
                for (Passenger p : b.getPassengers()) {
                    String passengerLine =
                        b.getBookingRef() + "," +
                        p.getName()       + "," +
                        p.getAge()        + "," +
                        p.getGender()     + "," +
                        p.getPhone()      + "," +
                        p.getEmail();
                    pw.write(passengerLine + "\n");
                }
            }
        } catch (IOException e) {
            System.out.println("[FileStorage] ERROR saving bookings: " + e.getMessage());
        }
    }

    /**
     * Reads data/bookings.csv and data/passengers.csv to rebuild the bookings HashMap.
     *
     * Requires the flights map so we can attach the actual Flight object to each Booking.
     * Requires that flights already have their seat grids generated.
     *
     * @param flights the already-loaded flights map (keyed by flightNo)
     * @return HashMap of Booking objects keyed by bookingRef
     */
    public static HashMap<String, Booking> loadBookings(HashMap<String, Flight> flights) {
        HashMap<String, Booking> bookings = new HashMap<>();

        // ── Step 1: Load passengers into a temporary map keyed by bookingRef ──
        HashMap<String, ArrayList<Passenger>> passengerMap = loadPassengers();

        // ── Step 2: Load bookings file ──
        File file = new File(BOOKINGS_FILE);
        if (!file.exists()) {
            return bookings;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Split into 10 columns
                String[] p = line.split(",", 10);
                if (p.length < 10) continue;

                String bookingRef   = p[0];
                String flightNo     = p[1];
                String customerEmail= p[2];
                String seatNosStr   = p[3]; // e.g. "12A|13B"
                String baggageOption= p[4];
                double totalFare    = Double.parseDouble(p[5]);
                String statusStr    = p[6];
                String payMethod    = p[7];
                String payStatus    = p[8];
                double payAmount    = Double.parseDouble(p[9]);

                // ── Find the flight object ──
                Flight flight = flights.get(flightNo);
                if (flight == null) {
                    // Flight was deleted after booking — skip gracefully
                    System.out.println("[FileStorage] Warning: flight " + flightNo + " not found for booking " + bookingRef);
                    continue;
                }

                // ── Rebuild seat objects from seat number strings ──
                ArrayList<Seat> seats = new ArrayList<>();
                if (!seatNosStr.isEmpty()) {
                    String[] seatNos = seatNosStr.split("\\|"); // split on pipe
                    for (String seatNo : seatNos) {
                        Seat s = flight.getSeatByNumber(seatNo.trim());
                        if (s != null) {
                            seats.add(s);
                        }
                    }
                }

                // ── Get passengers for this booking ──
                ArrayList<Passenger> passengers = passengerMap.getOrDefault(
                    bookingRef, new ArrayList<>()
                );

                // ── Construct the Booking object ──
                Booking booking = new Booking(
                    bookingRef, flight, passengers, seats,
                    baggageOption, totalFare, customerEmail
                );

                // ── Restore booking status ──
                booking.setStatus(BookingStatus.valueOf(statusStr));

                // ── Restore payment (if present) ──
                if (!payMethod.isEmpty() && !payStatus.isEmpty()) {
                    Payment payment = new Payment(
                        PaymentMethod.valueOf(payMethod), payAmount
                    );
                    payment.setStatus(PaymentStatus.valueOf(payStatus));
                    booking.setPayment(payment);
                }

                bookings.put(bookingRef, booking);
            }
        } catch (IOException e) {
            System.out.println("[FileStorage] ERROR loading bookings: " + e.getMessage());
        }
        return bookings;
    }

    /**
     * Helper: reads data/passengers.csv and groups passengers by bookingRef.
     *
     * @return a map of bookingRef -> list of Passenger objects
     */
    private static HashMap<String, ArrayList<Passenger>> loadPassengers() {
        HashMap<String, ArrayList<Passenger>> map = new HashMap<>();
        File file = new File(PASSENGERS_FILE);
        if (!file.exists()) {
            return map;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] p = line.split(",", 6); // 6 columns
                if (p.length < 6) continue;

                String bookingRef = p[0];
                Passenger passenger = new Passenger(
                    p[1],                   // name
                    Integer.parseInt(p[2]), // age
                    p[3],                   // gender
                    p[4],                   // phone
                    p[5]                    // email
                );

                // Add to list for this bookingRef
                map.computeIfAbsent(bookingRef, k -> new ArrayList<>()).add(passenger);
            }
        } catch (IOException e) {
            System.out.println("[FileStorage] ERROR loading passengers: " + e.getMessage());
        }
        return map;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SEAT STATE RESTORATION
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * After loading bookings, marks the correct seats as booked on each flight.
     *
     * Logic: for every CONFIRMED booking, find each seat in the flight's seat list
     *        and set isBooked = true.  CANCELLED bookings are skipped (seats are free).
     *
     * This must be called AFTER both loadFlights() and loadBookings() complete.
     *
     * @param bookings the loaded bookings map
     */
    public static void applyBookedSeats(HashMap<String, Booking> bookings) {
        for (Booking b : bookings.values()) {
            // Only mark seats for active (CONFIRMED) bookings
            if (b.getStatus() == BookingStatus.CONFIRMED) {
                for (Seat seat : b.getSeats()) {
                    seat.setBooked(true); // mark this seat as taken on the flight
                }
            }
        }
    }

    /**
     * Saves a single text ticket file for a confirmed booking.
     * File is written to data/ticket_<bookingRef>.txt
     * This simulates the email/SMS step.
     *
     * @param booking the confirmed booking to generate a ticket for
     */
    public static void saveTicket(Booking booking) {
        String fileName = DATA_DIR + "ticket_" + booking.getBookingRef() + ".txt";
        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write("=".repeat(55) + "\n");
            fw.write("        AERORESERVE — BOOKING CONFIRMATION\n");
            fw.write("=".repeat(55) + "\n\n");
            fw.write(booking.toString());
            fw.write("\n" + "=".repeat(55) + "\n");
            fw.write("       Thank you for flying with AeroReserve!\n");
            fw.write("=".repeat(55) + "\n");
            System.out.println("[FileStorage] Ticket saved: " + fileName);
        } catch (IOException e) {
            System.out.println("[FileStorage] ERROR saving ticket: " + e.getMessage());
        }
    }
}

