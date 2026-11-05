package com.aeroreserve.ui;

import com.aeroreserve.exception.InvalidInputException;
import com.aeroreserve.model.Flight;
import com.aeroreserve.model.Passenger;
import com.aeroreserve.model.Seat;
import com.aeroreserve.service.BookingService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * PassengerAndSeatScreen implements Stage 5:
 *  1. Passenger personal details form for each passenger.
 *  2. Baggage options selector (No baggage, 15kg, 20kg, 25kg).
 *  3. Interactive aircraft seat map:
 *      - Aircraft outline with cockpit and aisle gap.
 *      - Row numbers and column headers (A, B, C | D, E, F).
 *      - Real-time seat legend (Available, Selected, Booked, Extra Legroom).
 *      - Selection logic strictly enforced to match passenger count.
 *  4. Live fare calculation using BookingService.calculateFare.
 */
public class PassengerAndSeatScreen {

    private BorderPane root;

    // Flight and passenger count from previous stages
    private Flight flight;
    private int passengerCount;

    // List of passenger form rows
    private ArrayList<PassengerFormGroup> passengerForms;

    // Baggage selection
    private ToggleGroup baggageToggleGroup;
    private String selectedBaggage = "15 kg";

    // Selected seat numbers (e.g. ["12A", "12B"])
    private ArrayList<String> selectedSeatNos;

    // Map of seatNo -> Button to easily update seat styles on click
    private HashMap<String, Button> seatButtonMap;

    // UI feedback elements
    private Label seatSelectionSummaryLabel;
    private Label liveFareLabel;
    private Label inlineErrorLabel;

    /**
     * Constructor accepts the chosen flight and passenger count.
     * 
     * @param flight         the Flight selected on ResultsScreen
     * @param passengerCount number of seats and passenger forms needed
     */
    public PassengerAndSeatScreen(Flight flight, int passengerCount) {
        this.flight = flight;
        this.passengerCount = passengerCount;
        this.selectedSeatNos = new ArrayList<>();
        this.seatButtonMap = new HashMap<>();
        this.passengerForms = new ArrayList<>();

        createUI();
        updateFareSummary();
    }

    /**
     * Assembles the 2-column layout (Passenger Details on left, Aircraft Seat Map on right).
     */
    private void createUI() {
        root = new BorderPane();
        root.setPadding(new Insets(20, 32, 20, 32));

        // ── Top Header Bar ──
        HBox topBar = createTopHeader();
        root.setTop(topBar);

        // ── Main Content: 2-Column Split ──
        HBox columns = new HBox(24);
        columns.setPadding(new Insets(16, 0, 0, 0));

        // Left Column: Scrollable Passenger Forms + Baggage Options + Submit
        VBox leftColumn = createLeftColumn();
        HBox.setHgrow(leftColumn, Priority.ALWAYS);

        // Right Column: Aircraft-shaped Interactive Seat Map
        VBox rightColumn = createSeatMapColumn();
        rightColumn.setMinWidth(480);
        rightColumn.setMaxWidth(480);

        columns.getChildren().addAll(leftColumn, rightColumn);
        root.setCenter(columns);
    }

    /**
     * Top header showing flight recap and back button.
     */
    private HBox createTopHeader() {
        HBox topBar = new HBox(16);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("card-sm");

        Button backBtn = new Button("← Back to Results");
        backBtn.getStyleClass().add("btn-outline");
        backBtn.setOnAction(e -> {
            // Re-query results with same parameters
            ArrayList<Flight> results = AeroReserveApp.flightService.searchFlights(
                flight.getFrom(), flight.getTo(), flight.getDate()
            );
            AeroReserveApp.showScreen(new ResultsScreen(
                flight.getFrom(), flight.getTo(), flight.getDate(), passengerCount, results
            ).getView());
        });

        Label title = new Label("Passenger Details & Seat Map");
        title.getStyleClass().add("section-title");

        Label recap = new Label(
            flight.getFlightNo() + " (" + flight.getAirline() + ")  •  " +
            flight.getFrom() + " → " + flight.getTo() + "  •  " +
            flight.getDate() + "  •  " + passengerCount + " Passenger" + (passengerCount > 1 ? "s" : "")
        );
        recap.getStyleClass().add("text-secondary");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(backBtn, title, recap, spacer);
        return topBar;
    }

    /**
     * Builds the left column: Passenger Forms, Baggage Options, and Action Button.
     */
    private VBox createLeftColumn() {
        VBox leftBox = new VBox(16);

        // ScrollPane so multiple passenger forms scroll smoothly
        VBox formContainer = new VBox(16);

        // Quick-Fill Demo Details for viva examination
        HBox quickFillBox = new HBox(12);
        quickFillBox.setAlignment(Pos.CENTER_LEFT);
        Label quickFillLabel = new Label("Viva Quick-Fill:");
        quickFillLabel.getStyleClass().add("text-muted");
        Button fillBtn = new Button("Autofill Passenger Details");
        fillBtn.getStyleClass().add("btn-outline");
        fillBtn.setStyle("-fx-font-size: 11; -fx-padding: 4 10;");
        fillBtn.setOnAction(e -> quickFillDemoPassengers());
        quickFillBox.getChildren().addAll(quickFillLabel, fillBtn);
        formContainer.getChildren().add(quickFillBox);

        // Create a passenger form group for each passenger
        for (int i = 1; i <= passengerCount; i++) {
            PassengerFormGroup group = new PassengerFormGroup(i);
            passengerForms.add(group);
            formContainer.getChildren().add(group.getCard());
        }

        // Baggage Options Card
        VBox baggageCard = createBaggageCard();
        formContainer.getChildren().add(baggageCard);

        // Wrap forms in ScrollPane
        ScrollPane scrollPane = new ScrollPane(formContainer);
        scrollPane.getStyleClass().add("scroll-transparent");
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Bottom Action Card
        VBox actionCard = createActionCard();

        leftBox.getChildren().addAll(scrollPane, actionCard);
        return leftBox;
    }

    /**
     * Card for selecting baggage options with live surcharge updates.
     */
    private VBox createBaggageCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label title = new Label("Select Baggage Allowance");
        title.getStyleClass().add("card-title");

        baggageToggleGroup = new ToggleGroup();

        RadioButton opt0  = new RadioButton("No Check-in Baggage (7 kg Cabin Only) — Free");
        RadioButton opt15 = new RadioButton("15 kg Standard Check-in — +₹500 per booking");
        RadioButton opt20 = new RadioButton("20 kg Extra Allowance — +₹800 per booking");
        RadioButton opt25 = new RadioButton("25 kg Heavy Baggage — +₹1,200 per booking");

        opt0.setToggleGroup(baggageToggleGroup);
        opt15.setToggleGroup(baggageToggleGroup);
        opt20.setToggleGroup(baggageToggleGroup);
        opt25.setToggleGroup(baggageToggleGroup);

        opt0.setUserData("No Baggage");
        opt15.setUserData("15 kg");
        opt20.setUserData("20 kg");
        opt25.setUserData("25 kg");

        // Default: 15 kg
        opt15.setSelected(true);

        baggageToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedBaggage = (String) newVal.getUserData();
                updateFareSummary();
            }
        });

        card.getChildren().addAll(title, opt0, opt15, opt20, opt25);
        return card;
    }

    /**
     * Card with live total fare, selected seats recap, inline errors, and Continue button.
     */
    private VBox createActionCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card-sm");

        HBox summaryRow = new HBox(16);
        summaryRow.setAlignment(Pos.CENTER_LEFT);

        VBox seatsInfo = new VBox(2);
        Label seatsLabel = new Label("Selected Seats:");
        seatsLabel.getStyleClass().add("field-label");
        seatSelectionSummaryLabel = new Label("None selected (0 of " + passengerCount + ")");
        seatSelectionSummaryLabel.getStyleClass().add("text-secondary");
        seatsInfo.getChildren().addAll(seatsLabel, seatSelectionSummaryLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox fareInfo = new VBox(2);
        fareInfo.setAlignment(Pos.CENTER_RIGHT);
        Label fareTitle = new Label("Estimated Fare:");
        fareTitle.getStyleClass().add("field-label");
        liveFareLabel = new Label("₹0");
        liveFareLabel.getStyleClass().add("flight-price");
        fareInfo.getChildren().addAll(fareTitle, liveFareLabel);

        summaryRow.getChildren().addAll(seatsInfo, spacer, fareInfo);

        // Inline error message label
        inlineErrorLabel = new Label();
        inlineErrorLabel.getStyleClass().add("label-error");
        inlineErrorLabel.setWrapText(true);

        Button continueBtn = new Button("Continue to Review & Payment");
        continueBtn.getStyleClass().add("btn-accent");
        continueBtn.setMaxWidth(Double.MAX_VALUE);
        continueBtn.setOnAction(e -> handleContinue());

        card.getChildren().addAll(summaryRow, inlineErrorLabel, continueBtn);
        return card;
    }

    /**
     * Builds the right-hand column: Aircraft outline and interactive seat grid.
     */
    private VBox createSeatMapColumn() {
        VBox col = new VBox(12);
        col.getStyleClass().add("card");
        col.setAlignment(Pos.TOP_CENTER);

        // 1. Header & Title
        Label mapTitle = new Label("Aircraft Seat Map");
        mapTitle.getStyleClass().add("card-title");

        // 2. Legend
        HBox legendBox = createLegendBox();

        // 3. Aircraft Body Container
        VBox aircraftBox = new VBox(10);
        aircraftBox.getStyleClass().add("aircraft-container");
        aircraftBox.setAlignment(Pos.TOP_CENTER);

        // Cockpit Nose Indicator
        Label cockpitLabel = new Label("✈  FRONT / COCKPIT");
        cockpitLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2B2D7C; -fx-font-size: 11; -fx-padding: 4 0;");

        // Column Letters Header: A B C   [AISLE]   D E F
        HBox colHeader = new HBox(8);
        colHeader.setAlignment(Pos.CENTER);
        Label rowBlank = new Label("");
        rowBlank.setMinWidth(24);

        colHeader.getChildren().add(rowBlank);
        colHeader.getChildren().add(createColumnHeaderLabel("A"));
        colHeader.getChildren().add(createColumnHeaderLabel("B"));
        colHeader.getChildren().add(createColumnHeaderLabel("C"));

        Region aisleSpacer = new Region();
        aisleSpacer.setMinWidth(20);
        colHeader.getChildren().add(aisleSpacer);

        colHeader.getChildren().add(createColumnHeaderLabel("D"));
        colHeader.getChildren().add(createColumnHeaderLabel("E"));
        colHeader.getChildren().add(createColumnHeaderLabel("F"));

        // Scrollable 30 rows of seats
        VBox rowsGrid = new VBox(6);
        rowsGrid.setAlignment(Pos.TOP_CENTER);

        for (int r = 1; r <= 30; r++) {
            if (r == 4) {
                Label economyDivider = new Label("── Standard Economy ──");
                economyDivider.setStyle("-fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: #9CA3AF; -fx-padding: 4 0;");
                rowsGrid.getChildren().add(economyDivider);
            }
            HBox rowBox = createSeatRow(r);
            rowsGrid.getChildren().add(rowBox);
        }

        ScrollPane seatScroll = new ScrollPane(rowsGrid);
        seatScroll.getStyleClass().add("scroll-transparent");
        seatScroll.setFitToWidth(true);
        seatScroll.setPrefHeight(480);

        aircraftBox.getChildren().addAll(cockpitLabel, colHeader, seatScroll);
        col.getChildren().addAll(mapTitle, legendBox, aircraftBox);
        return col;
    }

    /**
     * Builds a single row of 6 seats with center aisle gap and row number label.
     */
    private HBox createSeatRow(int rowNumber) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER);

        // Row number on the left
        Label rowLabel = new Label(String.valueOf(rowNumber));
        rowLabel.getStyleClass().add("row-number-label");
        row.getChildren().add(rowLabel);

        // Left side: A, B, C
        row.getChildren().add(buildSeatButton(rowNumber + "A"));
        row.getChildren().add(buildSeatButton(rowNumber + "B"));
        row.getChildren().add(buildSeatButton(rowNumber + "C"));

        // Aisle Gap
        Region aisle = new Region();
        aisle.setMinWidth(20);
        row.getChildren().add(aisle);

        // Right side: D, E, F
        row.getChildren().add(buildSeatButton(rowNumber + "D"));
        row.getChildren().add(buildSeatButton(rowNumber + "E"));
        row.getChildren().add(buildSeatButton(rowNumber + "F"));

        return row;
    }

    /**
     * Creates a button for an individual seat and attaches click handlers.
     */
    private Button buildSeatButton(String seatNo) {
        Seat seat = flight.getSeatByNumber(seatNo);
        Button btn = new Button(seatNo);
        btn.getStyleClass().add("seat-btn");

        seatButtonMap.put(seatNo, btn);

        if (seat == null || seat.isBooked()) {
            // Already booked by another passenger
            btn.getStyleClass().add("seat-booked");
            btn.setDisable(true);
            Tooltip.install(btn, new Tooltip("Seat " + seatNo + " is already booked"));
        } else if (seat.getType() == Seat.SeatType.EXTRA_LEGROOM) {
            // Rows 1-3 Extra Legroom
            btn.getStyleClass().add("seat-extra-legroom");
            Tooltip.install(btn, new Tooltip("Seat " + seatNo + " (Extra Legroom +₹500)"));
            btn.setOnAction(e -> handleSeatClick(seatNo));
        } else {
            // Standard available seat
            btn.getStyleClass().add("seat-available");
            Tooltip.install(btn, new Tooltip("Seat " + seatNo + " (Available)"));
            btn.setOnAction(e -> handleSeatClick(seatNo));
        }

        return btn;
    }

    /**
     * Handles clicking a seat button to select or unselect.
     */
    private void handleSeatClick(String seatNo) {
        inlineErrorLabel.setText("");

        Seat seat = flight.getSeatByNumber(seatNo);
        if (seat == null || seat.isBooked()) return;

        Button btn = seatButtonMap.get(seatNo);

        if (selectedSeatNos.contains(seatNo)) {
            // Unselect seat
            selectedSeatNos.remove(seatNo);
            btn.getStyleClass().remove("seat-selected");
            if (seat.getType() == Seat.SeatType.EXTRA_LEGROOM) {
                btn.getStyleClass().add("seat-extra-legroom");
            } else {
                btn.getStyleClass().add("seat-available");
            }
        } else {
            // Check if limit reached
            if (selectedSeatNos.size() >= passengerCount) {
                inlineErrorLabel.setText("You have already selected " + passengerCount + " seat(s). Click a selected seat to change it.");
                return;
            }

            // Select seat
            selectedSeatNos.add(seatNo);
            btn.getStyleClass().removeAll("seat-available", "seat-extra-legroom");
            btn.getStyleClass().add("seat-selected");
        }

        updateFareSummary();
    }

    /**
     * Updates seat recap label and recalculates estimated fare.
     */
    private void updateFareSummary() {
        if (selectedSeatNos.isEmpty()) {
            seatSelectionSummaryLabel.setText("None selected (0 of " + passengerCount + ")");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < selectedSeatNos.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(selectedSeatNos.get(i));
            }
            sb.append(" (").append(selectedSeatNos.size()).append(" of ").append(passengerCount).append(")");
            seatSelectionSummaryLabel.setText(sb.toString());
        }

        // Calculate total fare using BookingService
        ArrayList<Seat> tempSeats = new ArrayList<>();
        for (String sNo : selectedSeatNos) {
            Seat s = flight.getSeatByNumber(sNo);
            if (s != null) tempSeats.add(s);
        }
        // If no seats selected yet, calculate baseline for passenger count
        while (tempSeats.size() < passengerCount) {
            tempSeats.add(new Seat("TEMP", Seat.SeatType.MIDDLE));
        }

        double total = BookingService.calculateFare(flight, tempSeats, selectedBaggage);
        liveFareLabel.setText("₹" + (int) total);
    }

    /**
     * Builds the seat map visual legend.
     */
    private HBox createLegendBox() {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(4, 0, 8, 0));

        box.getChildren().addAll(
            createLegendItem("seat-available", "Available"),
            createLegendItem("seat-selected", "Selected"),
            createLegendItem("seat-booked", "Booked"),
            createLegendItem("seat-extra-legroom", "Extra Legroom (+₹500)")
        );
        return box;
    }

    /**
     * Helper to create one legend swatch and label.
     */
    private HBox createLegendItem(String styleClass, String text) {
        HBox item = new HBox(4);
        item.setAlignment(Pos.CENTER);

        Region swatch = new Region();
        swatch.getStyleClass().addAll("seat-btn", styleClass);
        swatch.setMinSize(16, 16);
        swatch.setMaxSize(16, 16);

        Label label = new Label(text);
        label.setStyle("-fx-font-size: 11; -fx-text-fill: #374151;");

        item.getChildren().addAll(swatch, label);
        return item;
    }

    /**
     * Column header helper (A, B, C...).
     */
    private Label createColumnHeaderLabel(String letter) {
        Label lbl = new Label(letter);
        lbl.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-min-width: 32; -fx-alignment: center; -fx-text-fill: #6B7280;");
        return lbl;
    }

    /**
     * Viva shortcut: Populates realistic test data for all passenger forms and selects adjacent seats.
     */
    private void quickFillDemoPassengers() {
        String[][] demoData = {
            {"Rahul Sharma", "28", "Male", "9876543210", "rahul@example.com"},
            {"Priya Sharma", "26", "Female", "9876543211", "priya@example.com"},
            {"Amit Kumar",   "32", "Male", "9876543212", "amit@example.com"},
            {"Neha Patel",   "29", "Female", "9876543213", "neha@example.com"},
            {"Vikas Singh",  "40", "Male", "9876543214", "vikas@example.com"},
            {"Ananya Rao",   "22", "Female", "9876543215", "ananya@example.com"}
        };

        for (int i = 0; i < passengerForms.size(); i++) {
            if (i < demoData.length) {
                passengerForms.get(i).fill(
                    demoData[i][0], demoData[i][1], demoData[i][2], demoData[i][3], demoData[i][4]
                );
            }
        }

        // Also auto-select seats if none selected yet
        if (selectedSeatNos.isEmpty()) {
            int row = 5;
            String[] cols = {"A", "B", "C", "D", "E", "F"};
            for (int i = 0; i < passengerCount; i++) {
                String seatNo = row + cols[i % cols.length];
                Seat s = flight.getSeatByNumber(seatNo);
                if (s != null && !s.isBooked()) {
                    handleSeatClick(seatNo);
                }
            }
        }
        inlineErrorLabel.setText("");
    }

    /**
     * Validates all inputs and transitions to the Review & Payment screen (Stage 6).
     */
    private void handleContinue() {
        inlineErrorLabel.setText("");

        // 1. Validate Seats
        if (selectedSeatNos.size() != passengerCount) {
            inlineErrorLabel.setText(
                "Please select exactly " + passengerCount + " seat(s) on the aircraft seat map " +
                "(currently selected: " + selectedSeatNos.size() + ")."
            );
            return;
        }

        // 2. Validate Passengers
        ArrayList<Passenger> passengers = new ArrayList<>();
        for (PassengerFormGroup form : passengerForms) {
            try {
                passengers.add(form.validateAndGet());
            } catch (InvalidInputException ex) {
                inlineErrorLabel.setText(ex.getMessage());
                return;
            }
        }

        // Save shared state in AeroReserveApp
        AeroReserveApp.pendingPassengers = passengers;
        AeroReserveApp.pendingSeatNos = new ArrayList<>(selectedSeatNos);
        AeroReserveApp.pendingBaggageOption = selectedBaggage;

        // Transition to Review & Payment Screen
        AeroReserveApp.showScreen(new ReviewAndPaymentScreen().getView());
    }

    /**
     * Returns the root node.
     */
    public Parent getView() {
        return root;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper Inner Class: Form group for one passenger
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * PassengerFormGroup manages input controls and validation for one traveller.
     */
    private static class PassengerFormGroup {
        private int passengerIndex;
        private VBox card;
        private TextField nameField;
        private TextField ageField;
        private ComboBox<String> genderCombo;
        private TextField phoneField;
        private TextField emailField;

        public PassengerFormGroup(int index) {
            this.passengerIndex = index;
            createCard();
        }

        private void createCard() {
            card = new VBox(10);
            card.getStyleClass().add("card");

            Label title = new Label("Passenger " + passengerIndex + (passengerIndex == 1 ? " (Lead Traveller)" : ""));
            title.getStyleClass().add("card-title");

            // Row 1: Full Name & Age & Gender
            HBox row1 = new HBox(12);
            VBox nameBox = new VBox(4);
            Label nameLbl = new Label("Full Name *");
            nameLbl.getStyleClass().add("field-label");
            nameField = new TextField();
            nameField.setPromptText("As per Govt. ID");
            nameField.getStyleClass().add("text-input-custom");
            nameBox.getChildren().addAll(nameLbl, nameField);
            HBox.setHgrow(nameBox, Priority.ALWAYS);

            VBox ageBox = new VBox(4);
            Label ageLbl = new Label("Age *");
            ageLbl.getStyleClass().add("field-label");
            ageField = new TextField();
            ageField.setPromptText("Age");
            ageField.getStyleClass().add("text-input-custom");
            ageBox.getChildren().addAll(ageLbl, ageField);
            ageBox.setPrefWidth(100);

            VBox genderBox = new VBox(4);
            Label genderLbl = new Label("Gender *");
            genderLbl.getStyleClass().add("field-label");
            genderCombo = new ComboBox<>();
            genderCombo.getItems().addAll("Male", "Female", "Other");
            genderCombo.setValue("Male");
            genderCombo.getStyleClass().add("combo-custom");
            genderBox.getChildren().addAll(genderLbl, genderCombo);
            genderBox.setPrefWidth(120);

            row1.getChildren().addAll(nameBox, ageBox, genderBox);

            // Row 2: Phone & Email
            HBox row2 = new HBox(12);
            VBox phoneBox = new VBox(4);
            Label phoneLbl = new Label("Mobile Number *");
            phoneLbl.getStyleClass().add("field-label");
            phoneField = new TextField();
            phoneField.setPromptText("10-digit number");
            phoneField.getStyleClass().add("text-input-custom");
            phoneBox.getChildren().addAll(phoneLbl, phoneField);
            HBox.setHgrow(phoneBox, Priority.ALWAYS);

            VBox emailBox = new VBox(4);
            Label emailLbl = new Label("Email Address *");
            emailLbl.getStyleClass().add("field-label");
            emailField = new TextField();
            emailField.setPromptText("For e-ticket");
            emailField.getStyleClass().add("text-input-custom");
            emailBox.getChildren().addAll(emailLbl, emailField);
            HBox.setHgrow(emailBox, Priority.ALWAYS);

            row2.getChildren().addAll(phoneBox, emailBox);

            card.getChildren().addAll(title, row1, row2);
        }

        public void fill(String name, String age, String gender, String phone, String email) {
            nameField.setText(name);
            ageField.setText(age);
            genderCombo.setValue(gender);
            phoneField.setText(phone);
            emailField.setText(email);
        }

        public Passenger validateAndGet() throws InvalidInputException {
            String name = nameField.getText() != null ? nameField.getText().trim() : "";
            if (name.isEmpty() || name.length() < 2) {
                throw new InvalidInputException("Passenger " + passengerIndex + ": Please enter a valid full name.");
            }

            int age;
            try {
                age = Integer.parseInt(ageField.getText().trim());
                if (age <= 0 || age > 120) {
                    throw new InvalidInputException("Passenger " + passengerIndex + ": Age must be between 1 and 120.");
                }
            } catch (NumberFormatException e) {
                throw new InvalidInputException("Passenger " + passengerIndex + ": Please enter a valid numeric age.");
            }

            String gender = genderCombo.getValue();

            String phone = phoneField.getText() != null ? phoneField.getText().trim() : "";
            if (!phone.matches("\\d{10}")) {
                throw new InvalidInputException("Passenger " + passengerIndex + ": Mobile number must be exactly 10 digits.");
            }

            String email = emailField.getText() != null ? emailField.getText().trim() : "";
            if (!email.contains("@") || !email.contains(".")) {
                throw new InvalidInputException("Passenger " + passengerIndex + ": Please enter a valid email address.");
            }

            return new Passenger(name, age, gender, phone, email);
        }

        public VBox getCard() {
            return card;
        }
    }
}

