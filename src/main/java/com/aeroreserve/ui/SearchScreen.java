package com.aeroreserve.ui;

import com.aeroreserve.model.Flight;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;

/**
 * SearchScreen allows passengers to search for domestic flights.
 * 
 * Key Features:
 *  - Deep indigo coloured hero banner with title.
 *  - Large search card with Origin (From), Destination (To), Swap button,
 *    Departure Date picker, and an interactive Passenger counter.
 *  - Inline error feedback (no alert popups).
 *  - One-click route shortcuts for fast viva demonstration.
 */
public class SearchScreen {

    private VBox root;

    // Search input controls
    private ComboBox<String> fromCombo;
    private ComboBox<String> toCombo;
    private DatePicker datePicker;
    private Label passengerCountLabel;
    private Label errorLabel;

    // Passenger counter state (default 1 passenger)
    private int passengers = 1;

    // Available domestic airport options
    private final String[] CITIES = {
        "HYD - Hyderabad",
        "BOM - Mumbai",
        "DEL - Delhi",
        "BLR - Bengaluru",
        "MAA - Chennai",
        "CCU - Kolkata"
    };

    /**
     * Constructor builds the search interface.
     */
    public SearchScreen() {
        createUI();
    }

    /**
     * Assembles the banner and the search card.
     */
    private void createUI() {
        root = new VBox(0);
        root.setAlignment(Pos.TOP_CENTER);

        // ── 1. Coloured Banner ───────────────────────────────────────────────
        VBox banner = new VBox(8);
        banner.getStyleClass().add("search-banner");
        banner.setAlignment(Pos.CENTER);

        Label bannerTitle = new Label("Search and Book Domestic Flights");
        bannerTitle.getStyleClass().add("search-banner-title");

        Label bannerSubtitle = new Label("Real-time seat availability across major Indian routes");
        bannerSubtitle.getStyleClass().add("search-banner-subtitle");

        banner.getChildren().addAll(bannerTitle, bannerSubtitle);

        // ── 2. Search Card Container ─────────────────────────────────────────
        VBox searchCardWrapper = new VBox(20);
        searchCardWrapper.setAlignment(Pos.CENTER);
        searchCardWrapper.setPadding(new Insets(-36, 40, 40, 40)); // Negative margin to overlap banner slightly

        VBox searchCard = new VBox(24);
        searchCard.getStyleClass().add("search-card");
        searchCard.setMaxWidth(1000);

        // Card Title & Trip Type Header
        HBox cardHeader = new HBox(20);
        cardHeader.setAlignment(Pos.CENTER_LEFT);

        Label cardTitle = new Label("Book Domestic Flights");
        cardTitle.getStyleClass().add("card-title");

        HBox tripTypeRow = new HBox(14);
        tripTypeRow.setAlignment(Pos.CENTER_LEFT);
        RadioButton oneWayRadio = new RadioButton("One Way");
        oneWayRadio.setSelected(true);
        RadioButton roundTripRadio = new RadioButton("Round Trip");
        roundTripRadio.setDisable(true);
        Tooltip.install(roundTripRadio, new Tooltip("Domestic one-way routes currently enabled"));
        tripTypeRow.getChildren().addAll(oneWayRadio, roundTripRadio);

        cardHeader.getChildren().addAll(cardTitle, tripTypeRow);

        // Input Fields Row (From, Swap, To, Date, Passengers)
        HBox inputsRow = createInputsRow();

        // Action Row with validation error and search button
        HBox actionRow = createActionRow();

        // Viva demo shortcuts row
        VBox shortcutsBox = createShortcutsBox();

        searchCard.getChildren().addAll(cardHeader, inputsRow, actionRow, shortcutsBox);
        searchCardWrapper.getChildren().add(searchCard);

        root.getChildren().addAll(banner, searchCardWrapper);
    }

    /**
     * Builds the horizontal row containing From, Swap Button, To, DatePicker, and Passenger Counter.
     * 
     * @return HBox holding all search inputs
     */
    private HBox createInputsRow() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER);

        // ── FROM Box ──
        VBox fromBox = new VBox(4);
        fromBox.getStyleClass().add("city-input-box");
        Label fromTitle = new Label("FROM");
        fromTitle.getStyleClass().add("city-label-small");
        fromCombo = new ComboBox<>();
        fromCombo.getItems().addAll(CITIES);
        fromCombo.setValue(CITIES[0]); // Default: HYD
        fromCombo.getStyleClass().add("combo-custom");
        fromBox.getChildren().addAll(fromTitle, fromCombo);
        HBox.setHgrow(fromBox, Priority.ALWAYS);

        // ── SWAP Button ──
        Button swapBtn = new Button("⇄");
        swapBtn.getStyleClass().add("swap-btn");
        swapBtn.setOnAction(e -> handleSwap());

        // ── TO Box ──
        VBox toBox = new VBox(4);
        toBox.getStyleClass().add("city-input-box");
        Label toTitle = new Label("TO");
        toTitle.getStyleClass().add("city-label-small");
        toCombo = new ComboBox<>();
        toCombo.getItems().addAll(CITIES);
        toCombo.setValue(CITIES[1]); // Default: BOM
        toCombo.getStyleClass().add("combo-custom");
        toBox.getChildren().addAll(toTitle, toCombo);
        HBox.setHgrow(toBox, Priority.ALWAYS);

        // ── DATE Box ──
        VBox dateBox = new VBox(4);
        dateBox.getStyleClass().add("city-input-box");
        Label dateTitle = new Label("DEPARTURE DATE");
        dateTitle.getStyleClass().add("city-label-small");
        datePicker = new DatePicker();
        datePicker.getStyleClass().add("date-picker-custom");
        // Default to sample data date (2025-12-15) so search yields immediate results
        datePicker.setValue(LocalDate.of(2025, 12, 15));
        dateBox.getChildren().addAll(dateTitle, datePicker);
        HBox.setHgrow(dateBox, Priority.ALWAYS);

        // ── PASSENGER COUNTER Box ──
        VBox passBox = new VBox(4);
        passBox.getStyleClass().add("passenger-box");
        Label passTitle = new Label("PASSENGERS");
        passTitle.getStyleClass().add("city-label-small");

        HBox counterBox = new HBox(8);
        counterBox.setAlignment(Pos.CENTER);

        Button minusBtn = new Button("-");
        minusBtn.getStyleClass().add("btn-outline");
        minusBtn.setPrefWidth(32);
        minusBtn.setPrefHeight(32);
        minusBtn.setOnAction(e -> {
            if (passengers > 1) {
                passengers--;
                passengerCountLabel.setText(String.valueOf(passengers));
            }
        });

        passengerCountLabel = new Label(String.valueOf(passengers));
        passengerCountLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-min-width: 24; -fx-alignment: center;");

        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().add("btn-outline");
        plusBtn.setPrefWidth(32);
        plusBtn.setPrefHeight(32);
        plusBtn.setOnAction(e -> {
            if (passengers < 6) { // Maximum 6 passengers per booking
                passengers++;
                passengerCountLabel.setText(String.valueOf(passengers));
            }
        });

        counterBox.getChildren().addAll(minusBtn, passengerCountLabel, plusBtn);
        passBox.getChildren().addAll(passTitle, counterBox);

        row.getChildren().addAll(fromBox, swapBtn, toBox, dateBox, passBox);
        return row;
    }

    /**
     * Swaps the selected Origin and Destination cities.
     */
    private void handleSwap() {
        String temp = fromCombo.getValue();
        fromCombo.setValue(toCombo.getValue());
        toCombo.setValue(temp);
    }

    /**
     * Assembles the bottom action row with error label and the Search button.
     * 
     * @return HBox containing actions
     */
    private HBox createActionRow() {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_RIGHT);

        errorLabel = new Label();
        errorLabel.getStyleClass().add("label-error");
        errorLabel.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button searchBtn = new Button("Search Flights");
        searchBtn.getStyleClass().add("btn-accent");
        searchBtn.setMinWidth(180);
        searchBtn.setOnAction(e -> handleSearch());

        row.getChildren().addAll(errorLabel, spacer, searchBtn);
        return row;
    }

    /**
     * Provides quick-click route buttons for fast viva examination.
     */
    private VBox createShortcutsBox() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12, 0, 0, 0));

        Label hintLabel = new Label("Quick Sample Routes (Date: 2025-12-15):");
        hintLabel.getStyleClass().add("text-muted");

        HBox pillsBox = new HBox(8);
        pillsBox.getChildren().addAll(
            createRoutePill("HYD → BOM", 0, 1),
            createRoutePill("BOM → DEL", 1, 2),
            createRoutePill("DEL → BLR", 2, 3),
            createRoutePill("BLR → MAA", 3, 4),
            createRoutePill("MAA → CCU", 4, 5)
        );

        box.getChildren().addAll(hintLabel, pillsBox);
        return box;
    }

    /**
     * Helper to create a clickable shortcut button for a route.
     */
    private Button createRoutePill(String label, int fromIndex, int toIndex) {
        Button btn = new Button(label);
        btn.getStyleClass().add("btn-outline");
        btn.setStyle("-fx-font-size: 11; -fx-padding: 4 10;");
        btn.setOnAction(e -> {
            fromCombo.setValue(CITIES[fromIndex]);
            toCombo.setValue(CITIES[toIndex]);
            datePicker.setValue(LocalDate.of(2025, 12, 15));
            errorLabel.setText("");
        });
        return btn;
    }

    /**
     * Extracts 3-letter IATA code from the combo value (e.g. "HYD - Hyderabad" -> "HYD").
     */
    private String extractCityCode(String comboValue) {
        if (comboValue == null || comboValue.length() < 3) return "";
        return comboValue.substring(0, 3).trim();
    }

    /**
     * Validates user inputs, queries FlightService, and transitions to the ResultsScreen.
     */
    private void handleSearch() {
        errorLabel.setText("");

        String fromCode = extractCityCode(fromCombo.getValue());
        String toCode = extractCityCode(toCombo.getValue());

        // Validate origin and destination are different
        if (fromCode.equalsIgnoreCase(toCode)) {
            errorLabel.setText("Origin and Destination cannot be the same airport.");
            return;
        }

        // Validate date
        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate == null) {
            errorLabel.setText("Please select a departure date.");
            return;
        }

        String dateStr = selectedDate.toString(); // YYYY-MM-DD

        // Query FlightService using ArrayList
        ArrayList<Flight> results = AeroReserveApp.flightService.searchFlights(fromCode, toCode, dateStr);

        // Save passenger count to shared state
        AeroReserveApp.passengerCount = passengers;

        // Transition to ResultsScreen
        ResultsScreen resultsScreen = new ResultsScreen(fromCode, toCode, dateStr, passengers, results);
        AeroReserveApp.showScreen(resultsScreen.getView());
    }

    /**
     * Returns the root node of this screen.
     */
    public Parent getView() {
        return root;
    }
}

