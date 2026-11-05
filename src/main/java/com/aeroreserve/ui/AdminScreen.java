package com.aeroreserve.ui;

import com.aeroreserve.exception.InvalidInputException;
import com.aeroreserve.model.*;
import com.aeroreserve.model.Booking.BookingStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * AdminScreen implements Screen 7: The Comprehensive Airline Operations Dashboard.
 *
 * Designed for administrators to:
 *  1. Monitor real-time KPIs: fleet flight count, customer bookings, gross revenue, and seat capacity.
 *  2. Manage the flight fleet: add new domestic flights with automatic seat grid generation (30x6 = 180 seats),
 *     search flights, and remove scheduled flights.
 *  3. Oversee all passenger bookings across all users with real-time cancellation and e-ticket archiving.
 *  4. Showcase architecture principles (Polymorphism, CSV persistence, HashMap O(1) lookups) for viva defense.
 */
public class AdminScreen {

    private ScrollPane scrollPane;
    private VBox mainContentArea;

    // Active tab tracking
    private Button tabOverviewBtn;
    private Button tabFlightsBtn;
    private Button tabBookingsBtn;
    private String currentTab = "OVERVIEW";

    // Flight management controls
    private VBox addFlightFormCard;
    private TextField flightNoField;
    private ComboBox<String> airlineCombo;
    private ComboBox<String> fromCombo;
    private ComboBox<String> toCombo;
    private DatePicker flightDatePicker;
    private TextField depTimeField;
    private TextField arrTimeField;
    private TextField priceField;
    private Label formErrorLabel;
    private VBox flightsContainer;
    private TextField flightSearchField;

    // All bookings controls
    private VBox allBookingsContainer;
    private TextField bookingSearchField;
    private ComboBox<String> adminStatusFilter;

    /**
     * Constructs the Admin Screen.
     */
    public AdminScreen() {
        if (AeroReserveApp.headerBar != null) {
            AeroReserveApp.headerBar.setActiveTab("ADMIN");
        }
        createUI();
    }

    /**
     * Builds the complete admin layout.
     */
    private void createUI() {
        VBox root = new VBox(0);
        root.setAlignment(Pos.TOP_CENTER);

        // ── 1. Admin Top Banner ──
        VBox banner = new VBox(6);
        banner.getStyleClass().add("hero-banner");
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(24, 40, 24, 40));

        HBox bannerTitleRow = new HBox(12);
        bannerTitleRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Admin Control Center");
        titleLabel.setStyle("-fx-font-size: 26; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");

        Label adminPill = new Label("ADMINISTRATOR");
        adminPill.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 12;");

        bannerTitleRow.getChildren().addAll(titleLabel, adminPill);

        Label subLabel = new Label("Fleet Scheduling, System-Wide Passenger Reservations, and Inventory Management");
        subLabel.setStyle("-fx-font-size: 13; -fx-text-fill: rgba(255, 255, 255, 0.85);");

        banner.getChildren().addAll(bannerTitleRow, subLabel);

        // ── 2. Tab Navigation Bar ──
        HBox tabContainer = new HBox(16);
        tabContainer.getStyleClass().add("admin-tabs-container");
        tabContainer.setAlignment(Pos.CENTER_LEFT);

        tabOverviewBtn = new Button("📊  Dashboard Overview");
        tabOverviewBtn.getStyleClass().addAll("admin-tab", "admin-tab-active");
        tabOverviewBtn.setOnAction(e -> switchTab("OVERVIEW"));

        tabFlightsBtn = new Button("✈  Flight Fleet Management");
        tabFlightsBtn.getStyleClass().add("admin-tab");
        tabFlightsBtn.setOnAction(e -> switchTab("FLIGHTS"));

        tabBookingsBtn = new Button("📋  All Customer Bookings");
        tabBookingsBtn.getStyleClass().add("admin-tab");
        tabBookingsBtn.setOnAction(e -> switchTab("BOOKINGS"));

        tabContainer.getChildren().addAll(tabOverviewBtn, tabFlightsBtn, tabBookingsBtn);

        // ── 3. Main Dynamic Content Container ──
        mainContentArea = new VBox(24);
        mainContentArea.setPadding(new Insets(28, 40, 48, 40));
        mainContentArea.setMaxWidth(1100);
        mainContentArea.setAlignment(Pos.TOP_CENTER);

        // Initial view
        renderOverviewTab();

        root.getChildren().addAll(banner, tabContainer, mainContentArea);

        scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-transparent");
    }

    /**
     * Tab switcher updating CSS states and replacing dynamic view.
     */
    private void switchTab(String tab) {
        currentTab = tab;

        tabOverviewBtn.getStyleClass().remove("admin-tab-active");
        tabFlightsBtn.getStyleClass().remove("admin-tab-active");
        tabBookingsBtn.getStyleClass().remove("admin-tab-active");

        mainContentArea.getChildren().clear();

        switch (tab) {
            case "OVERVIEW":
                tabOverviewBtn.getStyleClass().add("admin-tab-active");
                renderOverviewTab();
                break;
            case "FLIGHTS":
                tabFlightsBtn.getStyleClass().add("admin-tab-active");
                renderFlightsTab();
                break;
            case "BOOKINGS":
                tabBookingsBtn.getStyleClass().add("admin-tab-active");
                renderBookingsTab();
                break;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TAB 1: OVERVIEW & ANALYTICS
    // ─────────────────────────────────────────────────────────────────────────────

    private void renderOverviewTab() {
        int flightCount = AeroReserveApp.flightService.getFlightCount();
        int bookingCount = AeroReserveApp.bookingService.getBookingCount();

        // Calculate total confirmed revenue and capacity
        double totalRevenue = 0;
        int totalSeats = 0;
        int availableSeats = 0;

        for (Booking b : AeroReserveApp.bookingService.getAllBookings()) {
            if (b.getStatus() == BookingStatus.CONFIRMED) {
                totalRevenue += b.getTotalFare();
            }
        }

        for (Flight f : AeroReserveApp.flightService.getAllFlights()) {
            totalSeats += f.getSeats().size();
            availableSeats += f.getAvailableSeatCount();
        }

        // ── Metric Cards Row ──
        HBox statsGrid = new HBox(20);
        statsGrid.setAlignment(Pos.CENTER);
        statsGrid.setMaxWidth(1050);

        statsGrid.getChildren().addAll(
            createStatCard("✈", "Total Fleet Flights", String.valueOf(flightCount), "Active domestic routes"),
            createStatCard("🎟", "Total Bookings", String.valueOf(bookingCount), "Customer reservations"),
            createStatCard("₹", "Gross Revenue", "₹" + String.format("%,.0f", totalRevenue), "From confirmed bookings"),
            createStatCard("💺", "Inventory Seats", availableSeats + " / " + totalSeats, "Available / Capacity")
        );

        // ── Viva Technical Architecture & Key Details Card ──
        VBox vivaCard = new VBox(16);
        vivaCard.getStyleClass().add("card");
        vivaCard.setMaxWidth(1050);

        Label vivaTitle = new Label("System Architecture & Viva Talking Points");
        vivaTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1A1D36;");

        Label p1 = new Label("• Object-Oriented Design: Polymorphic User model (abstract class User -> Customer, Admin) ensures role-based UI access control.");
        Label p2 = new Label("• High-Performance Data Structures: HashMap<String, Flight> and HashMap<String, Booking> provide O(1) instantaneous lookups by flight number and PNR reference.");
        Label p3 = new Label("• CSV Data Persistence: All flights, users, bookings, and passengers persist across restarts in the data/ folder using BufferedReader and FileWriter.");
        Label p4 = new Label("• Automated Aircraft Generation: Adding a flight dynamically instantiates a 30-row × 6-column (180 seats) seat layout with realistic window/middle/aisle seat categorisation.");

        for (Label lbl : new Label[]{p1, p2, p3, p4}) {
            lbl.setStyle("-fx-font-size: 13; -fx-text-fill: #374151;");
            lbl.setWrapText(true);
        }

        // Quick action row
        HBox actionRow = new HBox(16);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button manageFlightsBtn = new Button("Manage Flight Fleet");
        manageFlightsBtn.getStyleClass().add("btn-primary");
        manageFlightsBtn.setOnAction(e -> switchTab("FLIGHTS"));

        Button viewBookingsBtn = new Button("View Customer Bookings");
        viewBookingsBtn.getStyleClass().add("btn-accent");
        viewBookingsBtn.setOnAction(e -> switchTab("BOOKINGS"));

        actionRow.getChildren().addAll(manageFlightsBtn, viewBookingsBtn);

        vivaCard.getChildren().addAll(vivaTitle, p1, p2, p3, p4, new Separator(), actionRow);

        mainContentArea.getChildren().addAll(statsGrid, vivaCard);
    }

    /**
     * Builds a single 3D elevated stat card.
     */
    private VBox createStatCard(String icon, String label, String value, String subtext) {
        VBox card = new VBox(8);
        card.getStyleClass().add("stat-card");
        card.setPrefWidth(240);
        card.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("stat-icon-badge");
        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("stat-icon-text");
        iconBox.getChildren().add(iconLbl);

        Label labelLbl = new Label(label);
        labelLbl.getStyleClass().add("stat-label");

        top.getChildren().addAll(iconBox, labelLbl);

        Label valLbl = new Label(value);
        valLbl.getStyleClass().add("stat-value");

        Label subLbl = new Label(subtext);
        subLbl.getStyleClass().add("stat-subtext");

        card.getChildren().addAll(top, valLbl, subLbl);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TAB 2: FLIGHT FLEET MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────────

    private void renderFlightsTab() {
        VBox flightsBox = new VBox(20);
        flightsBox.setMaxWidth(1050);
        flightsBox.setAlignment(Pos.TOP_CENTER);

        // ── Top Action Toolbar ──
        HBox toolbar = new HBox(16);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("card-sm");

        Label searchLbl = new Label("Search Flights:");
        searchLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");

        flightSearchField = new TextField();
        flightSearchField.setPromptText("Filter by flight number (e.g. 6E-2134) or airport code...");
        flightSearchField.getStyleClass().add("text-input-custom");
        flightSearchField.setPrefWidth(360);
        flightSearchField.textProperty().addListener((obs, oldVal, newVal) -> refreshFlightList());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button toggleAddFlightBtn = new Button("➕  Add New Flight");
        toggleAddFlightBtn.getStyleClass().add("btn-accent");
        toggleAddFlightBtn.setOnAction(e -> {
            boolean isVisible = addFlightFormCard.isVisible();
            addFlightFormCard.setVisible(!isVisible);
            addFlightFormCard.setManaged(!isVisible);
        });

        toolbar.getChildren().addAll(searchLbl, flightSearchField, spacer, toggleAddFlightBtn);

        // ── Add New Flight Form Card (Hidden by default) ──
        addFlightFormCard = buildAddFlightForm();
        addFlightFormCard.setVisible(false);
        addFlightFormCard.setManaged(false);

        // ── Flights List Container ──
        flightsContainer = new VBox(14);
        flightsContainer.setAlignment(Pos.TOP_CENTER);

        refreshFlightList();

        flightsBox.getChildren().addAll(toolbar, addFlightFormCard, flightsContainer);
        mainContentArea.getChildren().add(flightsBox);
    }

    /**
     * Builds the Add Flight collapsible form card.
     */
    private VBox buildAddFlightForm() {
        VBox form = new VBox(16);
        form.getStyleClass().add("card");
        form.setMaxWidth(1050);

        Label formTitle = new Label("Schedule New Domestic Flight");
        formTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1A1D36;");

        formErrorLabel = new Label();
        formErrorLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 13; -fx-font-weight: bold;");
        formErrorLabel.setVisible(false);
        formErrorLabel.setManaged(false);

        // Row 1: Flight No, Airline, Base Fare
        HBox row1 = new HBox(16);
        row1.setAlignment(Pos.CENTER_LEFT);

        VBox fnBox = new VBox(4);
        Label fnLbl = new Label("Flight Number *");
        fnLbl.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #374151;");
        flightNoField = new TextField();
        flightNoField.setPromptText("e.g. 6E-9099");
        flightNoField.getStyleClass().add("text-input-custom");
        flightNoField.setPrefWidth(180);
        fnBox.getChildren().addAll(fnLbl, flightNoField);

        VBox alBox = new VBox(4);
        Label alLbl = new Label("Airline *");
        alLbl.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #374151;");
        airlineCombo = new ComboBox<>();
        airlineCombo.getItems().addAll("IndiGo", "Air India", "SpiceJet", "Vistara", "Akasa Air");
        airlineCombo.setValue("IndiGo");
        airlineCombo.getStyleClass().add("combo-custom");
        airlineCombo.setPrefWidth(180);
        alBox.getChildren().addAll(alLbl, airlineCombo);

        VBox fareBox = new VBox(4);
        Label fareLbl = new Label("Base Fare (INR) *");
        fareLbl.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #374151;");
        priceField = new TextField();
        priceField.setPromptText("e.g. 4500");
        priceField.getStyleClass().add("text-input-custom");
        priceField.setPrefWidth(180);
        fareBox.getChildren().addAll(fareLbl, priceField);

        row1.getChildren().addAll(fnBox, alBox, fareBox);

        // Row 2: Origin, Destination, Travel Date
        HBox row2 = new HBox(16);
        row2.setAlignment(Pos.CENTER_LEFT);

        VBox fromBox = new VBox(4);
        Label fromLbl = new Label("Origin City *");
        fromLbl.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #374151;");
        fromCombo = new ComboBox<>();
        fromCombo.getItems().addAll("HYD", "BOM", "DEL", "BLR", "MAA", "CCU");
        fromCombo.setValue("HYD");
        fromCombo.getStyleClass().add("combo-custom");
        fromCombo.setPrefWidth(180);
        fromBox.getChildren().addAll(fromLbl, fromCombo);

        VBox toBox = new VBox(4);
        Label toLbl = new Label("Destination City *");
        toLbl.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #374151;");
        toCombo = new ComboBox<>();
        toCombo.getItems().addAll("BOM", "DEL", "BLR", "MAA", "CCU", "HYD");
        toCombo.setValue("BOM");
        toCombo.getStyleClass().add("combo-custom");
        toCombo.setPrefWidth(180);
        toBox.getChildren().addAll(toLbl, toCombo);

        VBox dateBox = new VBox(4);
        Label dateLbl = new Label("Travel Date *");
        dateLbl.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #374151;");
        flightDatePicker = new DatePicker(LocalDate.now().plusDays(1));
        flightDatePicker.getStyleClass().add("date-picker-custom");
        flightDatePicker.setPrefWidth(180);
        dateBox.getChildren().addAll(dateLbl, flightDatePicker);

        row2.getChildren().addAll(fromBox, toBox, dateBox);

        // Row 3: Departure Time, Arrival Time, Actions
        HBox row3 = new HBox(16);
        row3.setAlignment(Pos.CENTER_LEFT);

        VBox depBox = new VBox(4);
        Label depLbl = new Label("Departure Time (HH:MM) *");
        depLbl.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #374151;");
        depTimeField = new TextField();
        depTimeField.setPromptText("e.g. 08:30");
        depTimeField.getStyleClass().add("text-input-custom");
        depTimeField.setPrefWidth(180);
        depBox.getChildren().addAll(depLbl, depTimeField);

        VBox arrBox = new VBox(4);
        Label arrLbl = new Label("Arrival Time (HH:MM) *");
        arrLbl.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #374151;");
        arrTimeField = new TextField();
        arrTimeField.setPromptText("e.g. 10:45");
        arrTimeField.getStyleClass().add("text-input-custom");
        arrTimeField.setPrefWidth(180);
        arrBox.getChildren().addAll(arrLbl, arrTimeField);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button saveBtn = new Button("Save Flight & Generate Seats");
        saveBtn.getStyleClass().add("btn-accent");
        saveBtn.setOnAction(e -> handleAddFlight());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-outline");
        cancelBtn.setOnAction(e -> {
            addFlightFormCard.setVisible(false);
            addFlightFormCard.setManaged(false);
        });

        row3.getChildren().addAll(depBox, arrBox, spacer, cancelBtn, saveBtn);

        form.getChildren().addAll(formTitle, formErrorLabel, row1, row2, row3);
        return form;
    }

    /**
     * Validates input and adds flight through FlightService.
     */
    private void handleAddFlight() {
        formErrorLabel.setVisible(false);
        formErrorLabel.setManaged(false);

        String fn = flightNoField.getText() == null ? "" : flightNoField.getText().trim();
        String al = airlineCombo.getValue();
        String from = fromCombo.getValue();
        String to = toCombo.getValue();
        LocalDate d = flightDatePicker.getValue();
        String dt = depTimeField.getText() == null ? "" : depTimeField.getText().trim();
        String at = arrTimeField.getText() == null ? "" : arrTimeField.getText().trim();
        String pStr = priceField.getText() == null ? "" : priceField.getText().trim();

        if (fn.isEmpty() || dt.isEmpty() || at.isEmpty() || pStr.isEmpty() || d == null) {
            showFormError("Please fill in all mandatory fields marked with *.");
            return;
        }

        if (from.equalsIgnoreCase(to)) {
            showFormError("Origin and Destination cannot be the same city.");
            return;
        }

        double fare;
        try {
            fare = Double.parseDouble(pStr);
            if (fare <= 0) {
                showFormError("Base fare must be a positive number.");
                return;
            }
        } catch (NumberFormatException e) {
            showFormError("Invalid base fare. Please enter numbers only (e.g. 4500).");
            return;
        }

        try {
            Flight newFlight = new Flight(fn, al, from, to, d.toString(), dt, at, fare);
            AeroReserveApp.flightService.addFlight(newFlight);

            // Clear inputs
            flightNoField.clear();
            depTimeField.clear();
            arrTimeField.clear();
            priceField.clear();

            addFlightFormCard.setVisible(false);
            addFlightFormCard.setManaged(false);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Flight Created");
            alert.setHeaderText("Flight " + fn + " Successfully Scheduled");
            alert.setContentText("The flight has been registered with an initial 180-seat grid (30 rows × 6 seats) and persisted to data/flights.csv.");
            alert.showAndWait();

            refreshFlightList();
        } catch (InvalidInputException ex) {
            showFormError(ex.getMessage());
        } catch (Exception ex) {
            showFormError("Error scheduling flight: " + ex.getMessage());
        }
    }

    private void showFormError(String msg) {
        formErrorLabel.setText("⚠ " + msg);
        formErrorLabel.setVisible(true);
        formErrorLabel.setManaged(true);
    }

    /**
     * Refreshes the flight list with search filtering.
     */
    private void refreshFlightList() {
        flightsContainer.getChildren().clear();

        ArrayList<Flight> allFlights = AeroReserveApp.flightService.getAllFlights();
        String query = flightSearchField == null || flightSearchField.getText() == null
                ? "" : flightSearchField.getText().trim().toLowerCase();

        ArrayList<Flight> filtered = new ArrayList<>();
        for (Flight f : allFlights) {
            if (!query.isEmpty()) {
                boolean matchFn = f.getFlightNo().toLowerCase().contains(query);
                boolean matchAl = f.getAirline().toLowerCase().contains(query);
                boolean matchFrom = f.getFrom().toLowerCase().contains(query);
                boolean matchTo = f.getTo().toLowerCase().contains(query);

                if (!matchFn && !matchAl && !matchFrom && !matchTo) {
                    continue;
                }
            }
            filtered.add(f);
        }

        if (filtered.isEmpty()) {
            VBox empty = new VBox(12);
            empty.getStyleClass().add("card");
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(30));
            Label msg = new Label("No flights found matching \"" + query + "\"");
            msg.setStyle("-fx-font-size: 14; -fx-text-fill: #6B7280;");
            empty.getChildren().add(msg);
            flightsContainer.getChildren().add(empty);
            return;
        }

        for (Flight f : filtered) {
            flightsContainer.getChildren().add(buildFlightRowCard(f));
        }
    }

    /**
     * Builds a single flight card in the admin view.
     */
    private VBox buildFlightRowCard(Flight flight) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card-sm");

        HBox mainRow = new HBox(20);
        mainRow.setAlignment(Pos.CENTER_LEFT);

        // Airline & Number
        VBox fnBox = new VBox(3);
        fnBox.setPrefWidth(140);
        Label alBadge = new Label(flight.getAirline());
        alBadge.getStyleClass().add("airline-badge");
        Label fnLbl = new Label(flight.getFlightNo());
        fnLbl.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #111827;");
        fnBox.getChildren().addAll(alBadge, fnLbl);

        // Route & Schedule
        VBox routeBox = new VBox(3);
        routeBox.setPrefWidth(180);
        Label routeLbl = new Label(flight.getFrom() + " ──✈ " + flight.getTo());
        routeLbl.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #2B2D7C;");
        Label timingLbl = new Label(flight.getDepartureTime() + " - " + flight.getArrivalTime() + " (" + flight.getDurationString() + ")");
        timingLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #6B7280;");
        routeBox.getChildren().addAll(routeLbl, timingLbl);

        // Date
        VBox dateBox = new VBox(3);
        dateBox.setPrefWidth(130);
        Label dateTitle = new Label("Date");
        dateTitle.setStyle("-fx-font-size: 11; -fx-text-fill: #9CA3AF; -fx-font-weight: bold;");
        Label dateVal = new Label(flight.getDate());
        dateVal.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #374151;");
        dateBox.getChildren().addAll(dateTitle, dateVal);

        // Inventory Capacity
        VBox seatBox = new VBox(3);
        seatBox.setPrefWidth(160);
        Label seatTitle = new Label("Seat Availability");
        seatTitle.setStyle("-fx-font-size: 11; -fx-text-fill: #9CA3AF; -fx-font-weight: bold;");
        Label seatVal = new Label(flight.getAvailableSeatCount() + " / " + flight.getSeats().size() + " seats");
        seatVal.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: " + (flight.getAvailableSeatCount() < 20 ? "#DC2626" : "#15803D") + ";");
        seatBox.getChildren().addAll(seatTitle, seatVal);

        // Base Fare
        VBox fareBox = new VBox(3);
        fareBox.setPrefWidth(120);
        Label fareTitle = new Label("Base Fare");
        fareTitle.setStyle("-fx-font-size: 11; -fx-text-fill: #9CA3AF; -fx-font-weight: bold;");
        Label fareVal = new Label("₹" + String.format("%,.0f", flight.getPrice()));
        fareVal.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #111827;");
        fareBox.getChildren().addAll(fareTitle, fareVal);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Action: Remove
        Button removeBtn = new Button("Remove");
        removeBtn.getStyleClass().add("btn-danger");
        removeBtn.setOnAction(e -> handleRemoveFlight(flight));

        mainRow.getChildren().addAll(fnBox, routeBox, dateBox, seatBox, fareBox, spacer, removeBtn);
        card.getChildren().add(mainRow);
        return card;
    }

    /**
     * Handles flight removal with confirmation.
     */
    private void handleRemoveFlight(Flight flight) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Flight Removal");
        confirm.setHeaderText("Remove Flight " + flight.getFlightNo() + "?");
        confirm.setContentText(
            "Flight: " + flight.getFlightNo() + " (" + flight.getFrom() + " → " + flight.getTo() + ")\n" +
            "Scheduled on: " + flight.getDate() + " at " + flight.getDepartureTime() + "\n\n" +
            "Are you sure you want to remove this flight? This action cannot be undone."
        );

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try {
                    AeroReserveApp.flightService.removeFlight(flight.getFlightNo());
                    refreshFlightList();
                } catch (Exception ex) {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Error");
                    err.setContentText(ex.getMessage());
                    err.showAndWait();
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TAB 3: ALL CUSTOMER BOOKINGS
    // ─────────────────────────────────────────────────────────────────────────────

    private void renderBookingsTab() {
        VBox bookingsBox = new VBox(20);
        bookingsBox.setMaxWidth(1050);
        bookingsBox.setAlignment(Pos.TOP_CENTER);

        // Filter Bar
        HBox toolbar = new HBox(16);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("card-sm");

        Label searchLbl = new Label("Filter Ledger:");
        searchLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");

        bookingSearchField = new TextField();
        bookingSearchField.setPromptText("Search by PNR (ARB-00001) or customer email...");
        bookingSearchField.getStyleClass().add("text-input-custom");
        bookingSearchField.setPrefWidth(350);
        bookingSearchField.textProperty().addListener((obs, oldVal, newVal) -> refreshAllBookingsList());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLbl = new Label("Status:");
        statusLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");

        adminStatusFilter = new ComboBox<>();
        adminStatusFilter.getItems().addAll("All", "Confirmed", "Cancelled");
        adminStatusFilter.setValue("All");
        adminStatusFilter.getStyleClass().add("combo-custom");
        adminStatusFilter.setPrefWidth(150);
        adminStatusFilter.setOnAction(e -> refreshAllBookingsList());

        toolbar.getChildren().addAll(searchLbl, bookingSearchField, spacer, statusLbl, adminStatusFilter);

        // Bookings List Container
        allBookingsContainer = new VBox(14);
        allBookingsContainer.setAlignment(Pos.TOP_CENTER);

        refreshAllBookingsList();

        bookingsBox.getChildren().addAll(toolbar, allBookingsContainer);
        mainContentArea.getChildren().add(bookingsBox);
    }

    /**
     * Refreshes the all bookings ledger with real-time filtering.
     */
    private void refreshAllBookingsList() {
        allBookingsContainer.getChildren().clear();

        ArrayList<Booking> allBookings = AeroReserveApp.bookingService.getAllBookings();
        String query = bookingSearchField == null || bookingSearchField.getText() == null
                ? "" : bookingSearchField.getText().trim().toLowerCase();
        String selectedStatus = adminStatusFilter == null ? "All" : adminStatusFilter.getValue();

        ArrayList<Booking> filtered = new ArrayList<>();
        for (Booking b : allBookings) {
            // Status check
            if ("Confirmed".equals(selectedStatus) && b.getStatus() != BookingStatus.CONFIRMED) {
                continue;
            }
            if ("Cancelled".equals(selectedStatus) && b.getStatus() != BookingStatus.CANCELLED) {
                continue;
            }

            // Search query check
            if (!query.isEmpty()) {
                boolean matchPnr = b.getBookingRef().toLowerCase().contains(query);
                boolean matchEmail = b.getCustomerEmail().toLowerCase().contains(query);
                boolean matchFlight = b.getFlight().getFlightNo().toLowerCase().contains(query);

                if (!matchPnr && !matchEmail && !matchFlight) {
                    continue;
                }
            }
            filtered.add(b);
        }

        if (filtered.isEmpty()) {
            VBox empty = new VBox(12);
            empty.getStyleClass().add("card");
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(30));
            Label msg = new Label("No customer bookings found in system ledger.");
            msg.setStyle("-fx-font-size: 14; -fx-text-fill: #6B7280;");
            empty.getChildren().add(msg);
            allBookingsContainer.getChildren().add(empty);
            return;
        }

        for (Booking b : filtered) {
            allBookingsContainer.getChildren().add(buildAdminBookingCard(b));
        }
    }

    /**
     * Builds a single customer booking record row for the admin ledger.
     */
    private VBox buildAdminBookingCard(Booking booking) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card-sm");

        HBox topRow = new HBox(14);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label pnrBadge = new Label("PNR: " + booking.getBookingRef());
        pnrBadge.getStyleClass().add("badge-pnr");

        Label emailBadge = new Label("👤 " + booking.getCustomerEmail());
        emailBadge.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #374151; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4;");

        Label statusChip = new Label(booking.getStatus().name());
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            statusChip.getStyleClass().add("chip-confirmed");
        } else {
            statusChip.getStyleClass().add("chip-cancelled");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label fareLbl = new Label("₹" + String.format("%,.0f", booking.getTotalFare()));
        fareLbl.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #111827;");

        topRow.getChildren().addAll(pnrBadge, emailBadge, statusChip, spacer, fareLbl);

        // Middle Row: Flight & Seats
        HBox midRow = new HBox(16);
        midRow.setAlignment(Pos.CENTER_LEFT);

        Label flightLbl = new Label(booking.getFlight().getAirline() + " " + booking.getFlight().getFlightNo() + " (" + booking.getFlight().getFrom() + " → " + booking.getFlight().getTo() + ")");
        flightLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2B2D7C;");

        Label dateLbl = new Label("📅 " + booking.getFlight().getDate() + " (" + booking.getFlight().getDepartureTime() + ")");
        dateLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #6B7280;");

        StringBuilder seatsStr = new StringBuilder();
        for (Seat s : booking.getSeats()) {
            if (seatsStr.length() > 0) seatsStr.append(", ");
            seatsStr.append(s.getSeatNo());
        }

        Label seatLbl = new Label("Seats: " + seatsStr.toString());
        seatLbl.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #374151;");

        Label paxCountLbl = new Label("Pax: " + booking.getPassengers().size());
        paxCountLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #6B7280;");

        midRow.getChildren().addAll(flightLbl, dateLbl, seatLbl, paxCountLbl);

        // Action Row
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        Button viewTicketBtn = new Button("View E-Ticket");
        viewTicketBtn.getStyleClass().add("btn-outline");
        viewTicketBtn.setOnAction(e -> showTicketDialog(booking));

        actionRow.getChildren().add(viewTicketBtn);

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            Button cancelAdminBtn = new Button("Cancel Reservation");
            cancelAdminBtn.getStyleClass().add("btn-danger");
            cancelAdminBtn.setOnAction(e -> handleAdminCancel(booking));
            actionRow.getChildren().add(cancelAdminBtn);
        }

        card.getChildren().addAll(topRow, midRow, actionRow);
        return card;
    }

    /**
     * Admin cancellation handler that frees seats and saves state.
     */
    private void handleAdminCancel(Booking booking) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Admin Cancel Booking");
        confirm.setHeaderText("Cancel Booking " + booking.getBookingRef() + " for " + booking.getCustomerEmail() + "?");
        confirm.setContentText("This will immediately cancel the reservation, release the passenger seats back into the available pool, and persist changes.");

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try {
                    AeroReserveApp.bookingService.cancelBooking(booking.getBookingRef());
                    refreshAllBookingsList();
                } catch (Exception ex) {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Error");
                    err.setContentText(ex.getMessage());
                    err.showAndWait();
                }
            }
        });
    }

    /**
     * Shows modal ticket viewer dialog.
     */
    private void showTicketDialog(Booking booking) {
        String filePath = "data/ticket_" + booking.getBookingRef() + ".txt";
        StringBuilder sb = new StringBuilder();
        File file = new File(filePath);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } catch (IOException e) {
                sb.append("Error reading ticket file: ").append(e.getMessage());
            }
        } else {
            sb.append("Ticket Summary for ").append(booking.getBookingRef()).append(":\n\n");
            sb.append(booking.toString());
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Admin E-Ticket Viewer — " + booking.getBookingRef());

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #FFFFFF;");

        Label modalTitle = new Label("Passenger E-Ticket Archive (Admin View)");
        modalTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1A1D36;");

        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setPrefRowCount(18);
        textArea.setPrefColumnCount(55);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12;");

        HBox closeBar = new HBox();
        closeBar.setAlignment(Pos.CENTER_RIGHT);
        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-primary");
        closeBtn.setOnAction(e -> dialog.close());
        closeBar.getChildren().add(closeBtn);

        content.getChildren().addAll(modalTitle, textArea, closeBar);

        Scene scene = new Scene(content, 620, 480);
        String cssPath = getClass().getResource("/aeroreserve.css").toExternalForm();
        scene.getStylesheets().add(cssPath);

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /**
     * Returns the root ScrollPane node.
     *
     * @return ScrollPane
     */
    public ScrollPane getView() {
        return scrollPane;
    }
}
