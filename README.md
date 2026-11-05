# AeroReserve

**A flight reservation system built with Java and JavaFX.** Search flights, pick seats on an aircraft seat map, simulate payment, get a boarding-pass ticket, and manage bookings from an admin panel.

![Java](https://img.shields.io/badge/Java-17%2B-orange) ![JavaFX](https://img.shields.io/badge/JavaFX-21-blue) ![Maven](https://img.shields.io/badge/Build-Maven-red) ![Storage](https://img.shields.io/badge/Storage-CSV%20files-lightgrey)

AeroReserve simulates the core of an airline booking platform. The interface is inspired by MakeMyTrip, Google Flights and Skyscanner, and is written entirely in Java code and one CSS file. There is no FXML, no Scene Builder, no image files and no database.

<!--
Add screenshots here once you have them, for example:

| Search | Results | Seat map | Boarding pass |
|---|---|---|---|
| ![Search](docs/screenshots/search.png) | ![Results](docs/screenshots/results.png) | ![Seat map](docs/screenshots/seats.png) | ![Boarding pass](docs/screenshots/boarding-pass.png) |
-->

---

## Features

**Customers**
- Create an account and log in
- Search flights by origin, destination, date and passenger count (1-6)
- Filter results by maximum fare, departure time and airline; sort by cheapest, fastest or earliest
- Enter passenger details and choose a baggage option
- Pick seats on a 30-row, 6-column aircraft seat map (rows 1-5 are extra legroom, +₹500)
- Pay with Credit Card, Debit Card, Net Banking or UPI / Wallet (simulated, with failure and retry)
- Receive a boarding-pass confirmation and a text e-ticket
- View bookings, search by booking reference, and cancel (seats are released immediately)

**Admins**
- Dashboard with total flights, bookings, confirmed revenue and seat capacity
- Add or remove flights (a 180-seat layout is generated automatically)
- View and cancel any booking across the system

---

## Screens

| # | Screen | Purpose |
|---|---|---|
| 1 | Login / Register | Sign in or create an account; demo accounts are one click away |
| 2 | Search | Route, date and passengers, with a swap button and quick-route shortcuts |
| 3 | Results | Filter sidebar, sort tabs and flight cards |
| 4 | Passengers and Seats | Passenger forms, baggage options and the interactive seat map |
| 5 | Review and Payment | Itinerary summary, payment method and a live fare breakdown |
| 6 | Confirmation | Boarding pass with a barcode strip and a link to the e-ticket |
| 7 | My Bookings | Search, filter, view tickets and cancel |
| 8 | Admin Control Center | Overview, flight management and all bookings |

---

## Tech Stack

| Area | Choice |
|---|---|
| Language | Java 17+ |
| UI | JavaFX 21, styled with `aeroreserve.css` |
| Data structures | `ArrayList`, `HashMap` |
| Persistence | Plain CSV and text files via `BufferedReader` / `FileWriter` |
| Error handling | Custom checked exceptions |
| Build | Maven (used for JavaFX only) |

---

## Architecture

```
model     Entities: User, Customer, Admin, Flight, Seat, Passenger, Booking, Payment
service   Business logic and file I/O: Auth, Flight, Booking, Payment, FileStorage
ui        JavaFX screens and the navigation header
exception Custom exceptions
util      Sample data loader
```

**OOP concepts used**
- **Encapsulation:** all model fields are private, accessed through getters, setters and methods.
- **Inheritance and polymorphism:** the abstract class `User` is extended by `Customer` and `Admin`, each implementing `getRole()`. The header and navigation use the role to show the right screens.
- **Separation of concerns:** the UI never touches files directly; it calls the services.
- **Custom exceptions:** `SeatNotAvailableException`, `InvalidInputException`, `PaymentFailedException`, `FlightNotFoundException`, `UserAlreadyExistsException`.

**Data structures**

| Structure | Where | Why |
|---|---|---|
| `HashMap<String, Flight>` | `FlightService` | Look up a flight by flight number |
| `HashMap<String, Booking>` | `BookingService` | Look up a booking by reference |
| `HashMap<String, User>` | `AuthService` | Check credentials by email |
| `ArrayList<Flight>` | Search results | Filtering and sorting |
| `ArrayList<Seat>` | Each `Flight` | 180 seats per aircraft |
| `ArrayList<Passenger>` | Each `Booking` | Travellers on a ticket |

---

## Data and Persistence

Everything is stored in the `data/` folder:

| File | Columns |
|---|---|
| `users.csv` | `role,name,email,password` |
| `flights.csv` | `flightNo,airline,from,to,date,departureTime,arrivalTime,price` |
| `bookings.csv` | `bookingRef,customerEmail,flightNo,seatNos,baggageOption,totalFare,status,paymentId,paymentMethod` |
| `passengers.csv` | `bookingRef,seatNo,name,age,gender,phone,email` |
| `ticket_<bookingRef>.txt` | Generated e-ticket for each confirmed booking |

**On startup:** flights are loaded and empty seat grids are built, bookings are read from `bookings.csv`, and every `CONFIRMED` booking marks its seats as booked. Cancelled bookings leave their seats free.

---

## Getting Started

**Prerequisites:** JDK 17 or higher and Apache Maven 3.8 or higher.

```bash
git clone https://github.com/bharat24082007-spec/AeroReserve.git
cd AeroReserve
mvn javafx:run
```

**Demo accounts** (created automatically on first launch)

| Role | Email | Password |
|---|---|---|
| Customer | `demo@example.com` | `demo123` |
| Admin | `admin@aeroreserve.com` | `admin123` |

The login screen also has "Demo Customer" and "Demo Admin" buttons that fill these in.

---

## Project Structure

```
AeroReserve/
├── pom.xml
├── data/                     CSV storage and generated tickets
└── src/main/
    ├── java/com/aeroreserve/
    │   ├── Main.java         Console test runner
    │   ├── exception/
    │   ├── model/
    │   ├── service/
    │   ├── ui/
    │   │   ├── AeroReserveApp.java
    │   │   ├── HeaderBar.java
    │   │   ├── LoginScreen.java
    │   │   ├── SearchScreen.java
    │   │   ├── ResultsScreen.java
    │   │   ├── PassengerAndSeatScreen.java
    │   │   ├── ReviewAndPaymentScreen.java
    │   │   ├── ConfirmationScreen.java
    │   │   ├── MyBookingsScreen.java
    │   │   └── AdminScreen.java
    │   └── util/
    │       └── SampleDataLoader.java
    └── resources/
        └── aeroreserve.css
```

---

## Design

| Role | Colour |
|---|---|
| Page background | `#F4F6F9` |
| Header | `#1A1D36` |
| Primary | `#2B2D7C` |
| Accent | `#FF6D00` |
| Cards | `#FFFFFF` with a `#E5E7EB` border |

Seat states: available (green), selected (orange), booked (grey). Icons, the barcode strip and the aircraft outline are drawn with JavaFX shapes, so no image files are needed.

---

## Limitations

- Payment is simulated (about 15% random failure); no real payment gateway is involved.
- Passwords are stored as plain text in `users.csv`. This is fine for a demo, but a real system would hash them.
- Data is read and written by a single running instance; there is no locking for multiple simultaneous users.

---

## Author

Built by Bharat ([@bharat24082007-spec](https://github.com/bharat24082007-spec)) as a Java programming project.
