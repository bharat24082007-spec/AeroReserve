package com.aeroreserve.ui;

import com.aeroreserve.model.Flight;
import com.aeroreserve.model.Passenger;
import com.aeroreserve.model.User;
import com.aeroreserve.service.*;
import com.aeroreserve.util.SampleDataLoader;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.ArrayList;

/**
 * AeroReserveApp is the JavaFX entry point for the entire application.
 *
 * Responsibilities:
 *  1. Initialise all services (once, on startup).
 *  2. Build the main Scene and load the CSS theme.
 *  3. Act as a central navigator — every screen calls showScreen() to switch views.
 *  4. Hold shared state between screens (currentUser, selectedFlight, etc.)
 *     as public static fields — simple and readable for a college project.
 *
 * Run command:  mvn javafx:run
 */
public class AeroReserveApp extends Application {

    // ── Window / Scene references ─────────────────────────────────────────────

    /** The main application window, shared so any screen can resize or retitle it. */
    public static Stage primaryStage;

    /**
     * Root layout: a BorderPane.
     * - top: the header bar (once the user is logged in)
     * - center: the current screen (swapped by showScreen())
     */
    private static BorderPane rootLayout;

    // ── Services (public static — accessible from every UI class) ─────────────

    public static AuthService    authService;
    public static FlightService  flightService;
    public static BookingService bookingService;
    public static PaymentService paymentService;

    // ── Cross-screen shared state ─────────────────────────────────────────────

    /** Persistent top navigation header bar (null when on login screen). */
    public static HeaderBar headerBar;

    /** The user who is currently logged in (null if on login screen). */
    public static User currentUser;

    /** Flight chosen on the results screen, passed to passenger details screen. */
    public static Flight selectedFlight;

    /** Passengers entered on the passenger details screen, passed to review screen. */
    public static ArrayList<Passenger> pendingPassengers;

    /** Seat numbers chosen on the seat map, passed to review screen. */
    public static ArrayList<String> pendingSeatNos;

    /** Baggage choice from passenger details screen, passed to review screen. */
    public static String pendingBaggageOption;

    /** Number of passengers from the search screen, carried forward. */
    public static int passengerCount = 1;

    // ── JavaFX lifecycle ──────────────────────────────────────────────────────

    /**
     * start() is called by JavaFX after launch().
     * Sets up the scene, loads CSS, and shows the login screen.
     *
     * @param stage the primary window provided by JavaFX
     */
    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        // Initialise all services and seed sample data
        initServices();

        // Build the root BorderPane — center changes on every navigation
        rootLayout = new BorderPane();
        rootLayout.getStyleClass().add("app-root");

        // Build the scene at 1280×800
        Scene scene = new Scene(rootLayout, 1280, 800);

        // Load the external CSS file from the resources folder
        // Maven copies src/main/resources/ to the classpath root,
        // so "/aeroreserve.css" is the correct path.
        String cssPath = getClass().getResource("/aeroreserve.css").toExternalForm();
        scene.getStylesheets().add(cssPath);

        // Begin with the login screen
        showLoginScreen();

        // Window settings
        stage.setTitle("AeroReserve — Flight Reservation System");
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }

    // ── Service initialisation ────────────────────────────────────────────────

    /**
     * Creates all service objects and seeds sample data on the first run.
     * Order matters: FlightService must be ready before BookingService.
     */
    private void initServices() {
        FileStorage.createDataDirectory();              // ensure data/ folder exists
        authService    = new AuthService();             // loads users from file
        flightService  = new FlightService();           // loads flights from file
        bookingService = new BookingService(flightService); // loads bookings, marks seats
        paymentService = new PaymentService();          // stateless, no loading needed

        // Seeds 20 flights + 2 users if running for the first time
        SampleDataLoader.seedData(authService, flightService);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    /**
     * Swaps the center content to show a different screen.
     * Every UI class calls this method to navigate forward.
     *
     * @param screen the new screen to display (any JavaFX Parent node)
     */
    public static void showScreen(Parent screen) {
        rootLayout.setCenter(screen);
    }

    /**
     * Shows the login screen and clears all session state.
     * Called on startup and when the user clicks Logout.
     */
    public static void showLoginScreen() {
        currentUser          = null;
        selectedFlight       = null;
        pendingPassengers    = null;
        pendingSeatNos       = null;
        pendingBaggageOption = null;
        headerBar            = null;

        rootLayout.setTop(null); // remove header on login screen
        showScreen(new LoginScreen().getView());
    }

    /**
     * Called by LoginScreen after a successful login or registration.
     * Routes to the admin panel or the customer search screen.
     *
     * @param user the user who just logged in
     */
    public static void onLoginSuccess(User user) {
        currentUser = user;

        // Display the persistent top navigation header
        headerBar = new HeaderBar(user);
        rootLayout.setTop(headerBar.getView());

        if (user.getRole().equals("ADMIN")) {
            // Admin goes directly to the Admin Dashboard (Stage 7)
            showScreen(new AdminScreen().getView());
        } else {
            // Customer goes directly to the flight search screen
            showScreen(new SearchScreen().getView());
        }
    }

    // ── Main ─────────────────────────────────────────────────────────────────

    /**
     * Java entry point. JavaFX requires calling launch() here.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        launch(args); // hands control to JavaFX, which then calls start()
    }
}

