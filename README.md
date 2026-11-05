# AeroReserve — Advanced Flight Reservation Management System

> **A Complete Java & JavaFX Desktop Application for Academic Evaluation & Viva Defense**  
> *Developed strictly following Object-Oriented Programming (OOP) design patterns, in-memory data structures (`ArrayList`, `HashMap`), CSV file persistence (no SQL/database), and pure JavaFX vector UI (no FXML, no Scene Builder, no image files).*

---

## ✈ 1. Project Overview

**AeroReserve** is an end-to-end domestic airline reservation and fleet management system built in Java 17 and JavaFX 21. It mirrors commercial flight booking platforms such as **MakeMyTrip**, **Google Flights**, and **Skyscanner**, providing:
- Complete customer booking lifecycle: route search $\rightarrow$ results filtering & sorting $\rightarrow$ passenger details & 30-row interactive aircraft seat map $\rightarrow$ payment simulation $\rightarrow$ boarding pass confirmation $\rightarrow$ text e-ticket file generation.
- Customer trip management (**My Bookings**): real-time PNR search, boarding pass recall, e-ticket viewer, and booking cancellation with automatic seat inventory release.
- **Admin Control Center**: real-time fleet analytics, dynamic flight fleet scheduling (with automatic 180-seat layout generation), and master passenger booking ledger.

---

## 🏛 2. Object-Oriented Architecture (Viva Core)

The application demonstrates all core tenets of Object-Oriented Software Engineering:

```
                          ┌──────────────────────────┐
                          │   abstract class User    │
                          │  (name, email, password) │
                          └─────────────┬────────────┘
                                        │
                         ┌──────────────┴──────────────┐
                         ▼                             ▼
              ┌─────────────────────┐       ┌─────────────────────┐
              │      Customer       │       │        Admin        │
              │  getRole(): CUSTOMER│       │   getRole(): ADMIN  │
              └─────────────────────┘       └─────────────────────┘
```

### 2.1 Encapsulation
- All entity fields in `com.aeroreserve.model` (`Flight`, `Seat`, `Booking`, `Passenger`, `User`, `Payment`) are declared `private`.
- Access is restricted through public getters, setters, and business logic methods.
- Defensive copies are returned when querying seat lists and bookings to prevent external mutation.

### 2.2 Inheritance & Polymorphism
- **Abstract Base Class:** `User.java` defines common user identity and credentials.
- **Derived Classes:** `Customer.java` and `Admin.java` inherit from `User` and provide polymorphic implementations of `getRole()`.
- **Polymorphic Navigation:** `HeaderBar.java` inspects `user.getRole()` to dynamically render Customer navigation buttons (`Search Flights`, `My Bookings`) or Admin navigation controls (`Admin Dashboard`, `Customer View`).

### 2.3 Abstraction & Separation of Concerns
- The UI layer (`com.aeroreserve.ui`) is completely decoupled from storage and business rules.
- Services in `com.aeroreserve.service` encapsulate all business validation, seat state mutation, and file I/O:
  - `AuthService`: Authentication, password verification, duplicate email checking.
  - `FlightService`: Flight search, route indexing, flight scheduling, and seat grid generation.
  - `BookingService`: Fare calculation, seat reservation/release, PNR generation, and status updating.
  - `PaymentService`: Multi-mode payment gateway simulation with a ~15% failure rate.
  - `FileStorage`: Plain-text CSV and ticket file persistence.

### 2.4 Custom Exception Hierarchy
The project uses custom checked exceptions in `com.aeroreserve.exception` for error boundaries:
- `SeatNotAvailableException`: Thrown if a requested seat was already reserved by another passenger.
- `InvalidInputException`: Thrown when validation rules fail (e.g. invalid phone digits, negative fare, identical origin/destination).
- `PaymentFailedException`: Thrown during simulated payment failures to trigger retry workflows.
- `FlightNotFoundException`: Thrown if a referenced flight number does not exist.
- `UserAlreadyExistsException`: Thrown when attempting to register an email already in use.

---

## ⚡ 3. Data Structures & Algorithmic Complexity

| Data Structure | Implementation | Purpose & Big-O Complexity |
| :--- | :--- | :--- |
| **`HashMap<String, Flight>`** | `FlightService.flights` | **$O(1)$ Average Lookup:** Instantaneous flight retrieval by flight number (e.g., `6E-2134`). |
| **`HashMap<String, Booking>`** | `BookingService.bookings` | **$O(1)$ Average Lookup:** Direct access to booking records by PNR reference (e.g., `ARB-00001`). |
| **`HashMap<String, User>`** | `AuthService.users` | **$O(1)$ Average Lookup:** Immediate customer/admin credential verification by email. |
| **`ArrayList<Flight>`** | Dynamic Search Results | Sequential iteration for multi-factor filtering (price, time, airline) and sorting ($O(n \log n)$). |
| **`ArrayList<Seat>`** | Aircraft Layout (`Flight.seats`) | Manages 180 seat positions (30 rows $\times$ 6 columns) per aircraft with direct seat state indexing. |
| **`ArrayList<Passenger>`** | `Booking.passengers` | Dynamic list of travelers associated with each ticket. |

---

## 💾 4. Persistence Architecture (`data/` Folder)

Data is saved locally in plain text CSV format via Java's native `BufferedReader` and `FileWriter` (zero database dependencies):

- **`data/users.csv`**: `role,name,email,password`
- **`data/flights.csv`**: `flightNo,airline,from,to,date,departureTime,arrivalTime,price`
- **`data/bookings.csv`**: `bookingRef,customerEmail,flightNo,seatNos,baggageOption,totalFare,status,paymentId,paymentMethod`
- **`data/passengers.csv`**: `bookingRef,seatNo,name,age,gender,phone,email`
- **`data/ticket_<bookingRef>.txt`**: Automatically generated ASCII/plain-text passenger e-ticket receipt containing route details, seat allocations, fare breakdown, and payment confirmation.

### Dynamic State Restoration on Startup
When the application starts:
1. `FlightService` loads all flights and generates empty seat grids.
2. `BookingService` loads existing bookings from `data/bookings.csv`.
3. `FileStorage.applyBookedSeats()` cross-references active `CONFIRMED` bookings and marks those specific seats as `isBooked = true` on the corresponding `Flight` objects. Cancelled bookings leave seats available.

---

## 🎨 5. JavaFX UI & Design System

The visual design follows modern airline platforms with a clean, light palette:
- **Canvas:** `#F4F6F9` (Clean airline slate)
- **Header:** `#1A1D36` (Midnight indigo with subtle drop shadow)
- **Primary Brand:** `#2B2D7C` (Royal Indigo)
- **Action Accent:** `#FF6D00` (Vibrant Flame Orange — MakeMyTrip style)
- **Cards:** Pure `#FFFFFF` with 1px `#E5E7EB` borders and soft gaussian elevation.
- **Zero Image Dependencies:** All icons and graphics (airplane icons, barcodes, checkmarks, radio buttons, swap arrows) are pure JavaFX vector shapes (`Rectangle`, `Circle`, `Path`, `-fx-shape`).

### Interactive Aircraft Seat Map (Stage 5)
- Realistic fuselage with curved cockpit header and nose indicator.
- Standard 30-row Airbus A320 configuration:
  - Rows 1–5: **Extra Legroom** (with subtle golden badge, +₹500 surcharge).
  - Rows 6–30: **Standard Economy**.
- Columns **A, B, C** $\parallel$ 24px Central Aisle $\parallel$ Columns **D, E, F**.
- Color-coded seat states:
  - **Available:** Mint emerald (`#ECFDF5`, border `#10B981`)
  - **Selected:** Vivid orange (`#FF6D00`)
  - **Booked / Occupied:** Muted slate (`#F3F4F6`, border `#E5E7EB`, disabled)

---

## 🚀 6. How to Run the Application

### Prerequisites
- **Java Development Kit (JDK):** Version 17 or higher.
- **Apache Maven:** Version 3.8 or higher.

### Command
Open a terminal in the project root directory and run:
```powershell
mvn javafx:run
```

---

## 🔑 7. Default Credentials for Testing

The system automatically seeds demo accounts on first launch:

| Role | Email | Password | Description |
| :--- | :--- | :--- | :--- |
| **Customer** | `demo@example.com` | `demo123` | Searches flights, books tickets, selects seats, views boarding pass, cancels reservations. |
| **Administrator** | `admin@aeroreserve.com` | `admin123` | Accesses Admin Control Center, views metrics, adds/removes flights, cancels reservations across the fleet. |

> **Viva Tip:** The Login screen features **"Demo Customer"** and **"Demo Admin"** one-click autofill buttons for rapid viva demonstration.

---

## 📋 8. Application Screens Breakdown

1. **Screen 1 — Login / Registration (`LoginScreen.java`):**
   - Dual-tab card for instant customer sign-in or account creation.
   - Real-time password length and email format verification.
   - Quick-fill demo account shortcuts.

2. **Screen 2 — Flight Search (`SearchScreen.java`):**
   - Trip Type selector (One Way / Round Trip).
   - Origin and Destination dropdowns with a 1-click swap button (`⇄`).
   - Departure DatePicker and Passenger count selector (1–6 travelers).
   - Quick viva route shortcut pills (`HYD → BOM`, `BOM → DEL`, `DEL → BLR`).

3. **Screen 3 — Search Results & Filters (`ResultsScreen.java`):**
   - 280px left filter sidebar:
     - Maximum fare price slider (₹1,500 – ₹10,000).
     - Departure time block checkboxes (Early Morning, Morning, Afternoon, Evening).
     - Dynamic airline carrier checkboxes with 1-click **Reset** button.
   - Sort tabs: **Cheapest First**, **Fastest First**, **Earliest First**.
   - Flight cards showing airline name badge, departure/arrival times, duration track, base price, and remaining available seat count.

4. **Screen 4 — Passenger Details & Interactive Seat Map (`PassengerAndSeatScreen.java`):**
   - Dynamic passenger form cards matching the chosen traveler count (Full Name, Age, Gender, 10-digit Phone, Email).
   - "Fill Demo Data" viva button.
   - Radio baggage options (Cabin Only, 15 kg, 20 kg, 25 kg).
   - 30-row $\times$ 6-column interactive aircraft seat grid with real-time seat selection counter and dynamic price calculation.

5. **Screen 5 — Review Booking & Payment Gateway (`ReviewAndPaymentScreen.java`):**
   - Itinerary summary and passenger recap.
   - 4 payment mode selectors: **Credit Card**, **Debit Card**, **Net Banking**, and **UPI / Wallets**.
   - Sticky live fare breakdown (base fare + extra legroom + baggage surcharge).
   - Background thread payment simulation handling realistic network latency and a **~15% simulated failure rate** with retry support.

6. **Screen 6 — Boarding Pass Confirmation (`ConfirmationScreen.java`):**
   - Physical airline boarding pass layout with large IATA codes (`HYD ✈ BOM`).
   - Dashed tear-off line and pure JavaFX vector barcode strip.
   - Confirmed green chip badge.
   - Direct link to view the generated plain-text ticket file (`data/ticket_<pnr>.txt`).
   - Actions to "View in My Bookings" or "Book Another Flight".

7. **Screen 7 — Customer Itinerary Dashboard (`MyBookingsScreen.java`):**
   - Real-time search by PNR or city code.
   - Status filters (`All`, `Confirmed`, `Cancelled`).
   - Card view with flight trajectory, assigned seats per passenger, and total fare.
   - View boarding pass, view e-ticket archive, or cancel booking (with immediate seat inventory release).

8. **Screen 8 — Admin Control Center (`AdminScreen.java`):**
   - **Overview Tab:** 4 KPI metric cards (Total Flights, Total Bookings, Confirmed Revenue, Seat Capacity) + viva architecture notes.
   - **Flight Fleet Management Tab:** Search flights, collapsible "Add Flight" form (with automatic 180-seat grid creation), and "Remove Flight" button.
   - **All Customer Bookings Tab:** System-wide booking ledger with PNR/email search, e-ticket viewer, and admin cancellation controls.

---

## 🎓 9. Comprehensive Viva Defense Q&A

### Q1: Why did you use `HashMap` instead of a simple `ArrayList` for storing flights and bookings?
> **Answer:** In flight reservation systems, flights are looked up by `flightNo` and bookings by `bookingRef` (PNR). An `ArrayList` requires linear search ($O(n)$ time complexity), which degrades performance as records grow. A `HashMap` provides constant time ($O(1)$) average lookup by hashing the key directly to its bucket, ensuring instantaneous access.

### Q2: How do you handle seat concurrency or preventing double booking?
> **Answer:** Before confirming any reservation, `BookingService.createBooking()` verifies each selected seat via `seat.isBooked()`. If any seat is already taken, it throws a custom `SeatNotAvailableException` before mutating state. Once validated, all seats are marked `setBooked(true)` atomically and persisted to `data/bookings.csv`.

### Q3: How is data restored after the application restarts without a database?
> **Answer:** On startup, `FlightService` loads the scheduled flights from `data/flights.csv` and initializes their seat grids. Then, `BookingService` reads `data/bookings.csv`. `FileStorage.applyBookedSeats()` iterates over all `CONFIRMED` bookings and marks those specific seats as taken on the corresponding `Flight` objects in memory.

### Q4: How is Polymorphism used in the user system?
> **Answer:** The application declares an abstract base class `User` with subclasses `Customer` and `Admin`. Each subclass polymorphically implements `getRole()`. In `HeaderBar` and `AeroReserveApp`, the system checks `user.getRole()` to present role-specific screens (Customer search vs Admin control center) without duplicating user authentication logic.

### Q5: Why is there no FXML or Scene Builder used?
> **Answer:** Building the JavaFX layout in pure Java code ensures type safety, full compile-time verification, dynamic component generation (such as dynamically building 1 to 6 passenger forms or 180 interactive seat buttons), and zero runtime reflection overhead.

### Q6: How does the payment simulation work, and why does it use a separate thread?
> **Answer:** `PaymentService.processPayment()` simulates real-world payment gateway latency by sleeping for 500ms and generates an intentional ~15% failure rate using `Math.random() < 0.15`. This runs on a background `Thread` so that the JavaFX Application Thread is never blocked, keeping the UI responsive. Once complete, `Platform.runLater()` updates the UI.

### Q7: What happens when a booking is cancelled?
> **Answer:** `BookingService.cancelBooking(pnr)` changes the booking status to `CANCELLED`, iterates through the assigned seats and sets `seat.setBooked(false)`, and writes the updated status to `data/bookings.csv`. Those seats immediately become available for other customers to book on the seat map.

### Q8: How are tickets generated and viewed?
> **Answer:** When a booking is confirmed, `FileStorage.saveTicket(booking)` formats the passenger names, seat numbers, flight route, timings, and payment reference into a plain-text file at `data/ticket_<pnr>.txt`. Both customers and administrators can inspect this file directly via a built-in modal file viewer dialog.

---

## 📁 10. Project Directory Structure

```
aeroreserve/
├── pom.xml                               # Maven project configuration (JavaFX 21 + JDK 17)
├── README.md                             # Comprehensive project & viva documentation
├── data/                                 # Persistent CSV storage & generated e-tickets
│   ├── users.csv
│   ├── flights.csv
│   ├── bookings.csv
│   ├── passengers.csv
│   └── ticket_ARB-00001.txt
└── src/
    └── main/
        ├── java/com/aeroreserve/
        │   ├── Main.java                 # Standalone console test suite
        │   ├── exception/                # Custom checked exceptions
        │   │   ├── FlightNotFoundException.java
        │   │   ├── InvalidInputException.java
        │   │   ├── PaymentFailedException.java
        │   │   ├── SeatNotAvailableException.java
        │   │   └── UserAlreadyExistsException.java
        │   ├── model/                    # Core entity domain models
        │   │   ├── User.java             # Abstract base class
        │   │   ├── Customer.java         # Inherits User
        │   │   ├── Admin.java            # Inherits User
        │   │   ├── Flight.java           # Flight details + seat grid
        │   │   ├── Seat.java             # Seat status and type enum
        │   │   ├── Passenger.java        # Passenger contact details
        │   │   ├── Booking.java          # Booking record + status enum
        │   │   └── Payment.java          # Payment transaction model
        │   ├── service/                  # Business logic & file persistence
        │   │   ├── AuthService.java      # User authentication & registration
        │   │   ├── FlightService.java    # Route search & flight fleet management
        │   │   ├── BookingService.java   # Seat reservation, release & fare calculation
        │   │   ├── PaymentService.java   # Simulated gateway (~15% failure rate)
        │   │   └── FileStorage.java      # CSV parser & ticket text generator
        │   ├── ui/                       # Pure JavaFX UI screens & components
        │   │   ├── AeroReserveApp.java   # App entry point & central navigator
        │   │   ├── HeaderBar.java        # Persistent top navigation bar
        │   │   ├── LoginScreen.java      # Screen 1: Login & Create Account
        │   │   ├── SearchScreen.java     # Screen 2: Domestic route search
        │   │   ├── ResultsScreen.java    # Screen 3: Filter & sort flight results
        │   │   ├── PassengerAndSeatScreen.java # Screen 4 & 5: Passenger form + Seat map
        │   │   ├── ReviewAndPaymentScreen.java # Screen 6: Review & Payment
        │   │   ├── ConfirmationScreen.java    # Screen 6: Boarding pass confirmation
        │   │   ├── MyBookingsScreen.java      # Screen 7: Customer bookings & cancellation
        │   │   └── AdminScreen.java           # Screen 7: Admin control center
        │   └── util/
        │       └── SampleDataLoader.java # Seeds domestic flights and demo users
        └── resources/
            └── aeroreserve.css           # MakeMyTrip/Google Flights style design system
```

---
*AeroReserve — Advanced Flight Reservation Management System. Engineered for excellence in software architecture and academic defense.*

