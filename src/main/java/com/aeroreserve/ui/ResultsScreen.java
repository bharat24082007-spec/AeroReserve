package com.aeroreserve.ui;

import com.aeroreserve.model.Flight;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * ResultsScreen displays available flights matching the user's search.
 * 
 * Key Features:
 *  - Top search summary bar with "Modify Search" button.
 *  - Left filter sidebar:
 *      * Maximum price slider
 *      * Departure time period checkboxes (Early Morning, Morning, Afternoon, Evening)
 *      * Airline checkboxes (IndiGo, Air India, SpiceJet, etc.)
 *  - Sort tabs:
 *      * Cheapest (lowest price first)
 *      * Fastest (shortest duration first)
 *      * Earliest (earliest departure time first)
 *  - Detailed flight cards showing departure, duration line with center dot,
 *    arrival, airline badge, available seats, and the warm orange Select button.
 */
public class ResultsScreen {

    private BorderPane root;

    // Search context
    private String fromCode;
    private String toCode;
    private String travelDate;
    private int passengerCount;

    // Original results and currently filtered list
    private ArrayList<Flight> masterFlights;
    private ArrayList<Flight> displayedFlights;

    // Filter controls
    private Slider priceSlider;
    private Label priceSliderValueLabel;
    private CheckBox earlyMorningCheck; // Before 06:00
    private CheckBox morningCheck;      // 06:00 - 12:00
    private CheckBox afternoonCheck;    // 12:00 - 18:00
    private CheckBox eveningCheck;      // After 18:00
    private ArrayList<CheckBox> airlineCheckBoxes;

    // Sort tabs
    private Button cheapestTab;
    private Button fastestTab;
    private Button earliestTab;
    private String currentSort = "CHEAPEST"; // CHEAPEST, FASTEST, EARLIEST

    // Results container
    private VBox flightsContainer;
    private Label resultsCountLabel;

    /**
     * Constructor receives search criteria and the matching flight results.
     * 
     * @param fromCode       origin airport code
     * @param toCode         destination airport code
     * @param travelDate     departure date string
     * @param passengerCount number of travellers
     * @param flights        search results from FlightService
     */
    public ResultsScreen(String fromCode, String toCode, String travelDate,
                         int passengerCount, ArrayList<Flight> flights) {
        this.fromCode = fromCode;
        this.toCode = toCode;
        this.travelDate = travelDate;
        this.passengerCount = passengerCount;
        this.masterFlights = new ArrayList<>(flights);
        this.displayedFlights = new ArrayList<>(flights);

        createUI();
        applyFiltersAndSort();
    }

    /**
     * Builds the main UI hierarchy.
     */
    private void createUI() {
        root = new BorderPane();
        root.setPadding(new Insets(24, 32, 24, 32));

        // ── 1. Top Summary Bar ──
        HBox topBar = createTopBar();
        root.setTop(topBar);

        // ── 2. Content Area (Left Sidebar + Right Results Column) ──
        HBox contentBox = new HBox(24);
        contentBox.setPadding(new Insets(20, 0, 0, 0));

        // Left Filter Sidebar
        VBox sidebar = createFilterSidebar();

        // Right Results Area (Sort Tabs + Scrollable Flight Cards)
        VBox resultsColumn = createResultsColumn();
        HBox.setHgrow(resultsColumn, Priority.ALWAYS);

        contentBox.getChildren().addAll(sidebar, resultsColumn);
        root.setCenter(contentBox);
    }

    /**
     * Creates the top summary bar showing the current route, date, and a "Modify Search" button.
     */
    private HBox createTopBar() {
        HBox topBar = new HBox(14);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("card-sm");

        // Route badge
        Label routeLabel = new Label(fromCode + "  →  " + toCode);
        routeLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #111827;");

        // Travel details pill
        HBox detailsPill = new HBox(8);
        detailsPill.setAlignment(Pos.CENTER);
        detailsPill.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 6; -fx-padding: 4 12;");

        Label dateLabel = new Label(travelDate);
        dateLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #4B5563; -fx-font-weight: bold;");

        Label separator = new Label("|");
        separator.setStyle("-fx-text-fill: #D1D5DB;");

        Label passLabel = new Label(passengerCount + " Traveller" + (passengerCount > 1 ? "s" : ""));
        passLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #4B5563; -fx-font-weight: bold;");

        detailsPill.getChildren().addAll(dateLabel, separator, passLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button modifyBtn = new Button("Modify Search");
        modifyBtn.getStyleClass().add("btn-outline");
        modifyBtn.setOnAction(e -> AeroReserveApp.showScreen(new SearchScreen().getView()));

        topBar.getChildren().addAll(routeLabel, detailsPill, spacer, modifyBtn);
        return topBar;
    }

    /**
     * Creates the filter sidebar containing price slider, departure time slots, and airlines.
     */
    private VBox createFilterSidebar() {
        VBox sidebar = new VBox(20);
        sidebar.getStyleClass().add("filter-sidebar");

        // Header with "Reset All"
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Filter Flights");
        title.getStyleClass().add("filter-section-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button resetBtn = new Button("Reset");
        resetBtn.getStyleClass().add("btn-link");
        resetBtn.setOnAction(e -> resetFilters());
        header.getChildren().addAll(title, spacer, resetBtn);

        // ── Price Filter ──
        VBox priceBox = new VBox(8);
        double maxFare = computeMaxFare();
        Label priceTitle = new Label("Maximum Fare");
        priceTitle.getStyleClass().add("field-label");

        priceSliderValueLabel = new Label("₹" + (int) maxFare);
        priceSliderValueLabel.getStyleClass().add("text-secondary");

        priceSlider = new Slider(1500, Math.max(10000, maxFare), maxFare);
        priceSlider.getStyleClass().add("slider-custom");
        priceSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            priceSliderValueLabel.setText("₹" + newVal.intValue());
            applyFiltersAndSort();
        });
        priceBox.getChildren().addAll(priceTitle, priceSliderValueLabel, priceSlider);

        // ── Departure Time Filter ──
        VBox timeBox = new VBox(8);
        Label timeTitle = new Label("Departure Time");
        timeTitle.getStyleClass().add("field-label");

        earlyMorningCheck = new CheckBox("Before 06:00 (Early)");
        morningCheck      = new CheckBox("06:00 - 12:00 (Morning)");
        afternoonCheck    = new CheckBox("12:00 - 18:00 (Afternoon)");
        eveningCheck      = new CheckBox("After 18:00 (Night)");

        // Listener triggers filtering when any checkbox changes
        earlyMorningCheck.setOnAction(e -> applyFiltersAndSort());
        morningCheck.setOnAction(e -> applyFiltersAndSort());
        afternoonCheck.setOnAction(e -> applyFiltersAndSort());
        eveningCheck.setOnAction(e -> applyFiltersAndSort());

        timeBox.getChildren().addAll(timeTitle, earlyMorningCheck, morningCheck, afternoonCheck, eveningCheck);

        // ── Airline Filter ──
        VBox airlineBox = new VBox(8);
        Label airlineTitle = new Label("Airlines");
        airlineTitle.getStyleClass().add("field-label");

        airlineCheckBoxes = new ArrayList<>();
        Set<String> airlines = new HashSet<>();
        for (Flight f : masterFlights) {
            airlines.add(f.getAirline());
        }
        // Fallback default set if no flights returned
        if (airlines.isEmpty()) {
            airlines.add("IndiGo");
            airlines.add("Air India");
            airlines.add("SpiceJet");
            airlines.add("Vistara");
        }

        airlineBox.getChildren().add(airlineTitle);
        for (String airline : airlines) {
            CheckBox cb = new CheckBox(airline);
            cb.setSelected(true); // checked by default
            cb.setOnAction(e -> applyFiltersAndSort());
            airlineCheckBoxes.add(cb);
            airlineBox.getChildren().add(cb);
        }

        sidebar.getChildren().addAll(header, priceBox, timeBox, airlineBox);
        return sidebar;
    }

    /**
     * Builds the right-hand column featuring sort tabs and the scrollable list of flights.
     */
    private VBox createResultsColumn() {
        VBox col = new VBox(16);

        // Header above sort tabs showing match count
        HBox countRow = new HBox();
        countRow.setAlignment(Pos.CENTER_LEFT);
        resultsCountLabel = new Label();
        resultsCountLabel.getStyleClass().add("text-secondary");
        countRow.getChildren().add(resultsCountLabel);

        // Sort tabs bar
        HBox sortBar = createSortTabBar();

        // Flights cards list inside a transparent ScrollPane
        flightsContainer = new VBox(12);
        ScrollPane scrollPane = new ScrollPane(flightsContainer);
        scrollPane.getStyleClass().add("scroll-transparent");
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        col.getChildren().addAll(countRow, sortBar, scrollPane);
        return col;
    }

    /**
     * Builds the sort tabs bar: [Cheapest] [Fastest] [Earliest].
     */
    private HBox createSortTabBar() {
        HBox bar = new HBox(8);
        bar.getStyleClass().add("sort-tab-bar");

        cheapestTab = new Button("Cheapest");
        cheapestTab.getStyleClass().addAll("sort-tab", "sort-tab-active");
        cheapestTab.setOnAction(e -> setSort("CHEAPEST"));

        fastestTab = new Button("Fastest");
        fastestTab.getStyleClass().add("sort-tab");
        fastestTab.setOnAction(e -> setSort("FASTEST"));

        earliestTab = new Button("Earliest");
        earliestTab.getStyleClass().add("sort-tab");
        earliestTab.setOnAction(e -> setSort("EARLIEST"));

        bar.getChildren().addAll(cheapestTab, fastestTab, earliestTab);
        return bar;
    }

    /**
     * Updates active sort tab and re-sorts the flight cards.
     */
    private void setSort(String sortType) {
        this.currentSort = sortType;

        cheapestTab.getStyleClass().remove("sort-tab-active");
        fastestTab.getStyleClass().remove("sort-tab-active");
        earliestTab.getStyleClass().remove("sort-tab-active");

        if (sortType.equals("CHEAPEST")) cheapestTab.getStyleClass().add("sort-tab-active");
        else if (sortType.equals("FASTEST")) fastestTab.getStyleClass().add("sort-tab-active");
        else if (sortType.equals("EARLIEST")) earliestTab.getStyleClass().add("sort-tab-active");

        applyFiltersAndSort();
    }

    /**
     * Resets all filters back to default.
     */
    private void resetFilters() {
        double maxFare = computeMaxFare();
        priceSlider.setValue(maxFare);
        earlyMorningCheck.setSelected(false);
        morningCheck.setSelected(false);
        afternoonCheck.setSelected(false);
        eveningCheck.setSelected(false);

        for (CheckBox cb : airlineCheckBoxes) {
            cb.setSelected(true);
        }
        applyFiltersAndSort();
    }

    /**
     * Helper to find maximum price among flights for the slider range.
     */
    private double computeMaxFare() {
        double max = 6000;
        for (Flight f : masterFlights) {
            if (f.getPrice() > max) max = f.getPrice();
        }
        return max;
    }

    /**
     * Filters master flights based on Price, Time, and Airline, then sorts them.
     */
    private void applyFiltersAndSort() {
        displayedFlights.clear();

        double maxPriceAllowed = priceSlider != null ? priceSlider.getValue() : Double.MAX_VALUE;

        // Check if any time filter is active
        boolean anyTimeFilter = earlyMorningCheck != null && (
            earlyMorningCheck.isSelected() || morningCheck.isSelected() ||
            afternoonCheck.isSelected() || eveningCheck.isSelected()
        );

        // Collect allowed airlines
        Set<String> allowedAirlines = new HashSet<>();
        if (airlineCheckBoxes != null) {
            for (CheckBox cb : airlineCheckBoxes) {
                if (cb.isSelected()) allowedAirlines.add(cb.getText());
            }
        }

        for (Flight f : masterFlights) {
            // 1. Price check
            if (f.getPrice() > maxPriceAllowed) continue;

            // 2. Airline check
            if (!allowedAirlines.isEmpty() && !allowedAirlines.contains(f.getAirline())) continue;

            // 3. Departure time check
            if (anyTimeFilter) {
                int depHour = Integer.parseInt(f.getDepartureTime().split(":")[0]);
                boolean matches = false;
                if (earlyMorningCheck.isSelected() && depHour < 6) matches = true;
                if (morningCheck.isSelected() && depHour >= 6 && depHour < 12) matches = true;
                if (afternoonCheck.isSelected() && depHour >= 12 && depHour < 18) matches = true;
                if (eveningCheck.isSelected() && depHour >= 18) matches = true;
                if (!matches) continue;
            }

            displayedFlights.add(f);
        }

        // Apply sorting
        if (currentSort.equals("CHEAPEST")) {
            Collections.sort(displayedFlights, Comparator.comparingDouble(Flight::getPrice));
        } else if (currentSort.equals("FASTEST")) {
            Collections.sort(displayedFlights, Comparator.comparingInt(Flight::getDurationMinutes));
        } else if (currentSort.equals("EARLIEST")) {
            Collections.sort(displayedFlights, Comparator.comparing(Flight::getDepartureTime));
        }

        // Re-render flight cards
        renderFlightCards();
    }

    /**
     * Renders flight cards or a "No Flights Found" placeholder into flightsContainer.
     */
    private void renderFlightCards() {
        flightsContainer.getChildren().clear();

        resultsCountLabel.setText("Showing " + displayedFlights.size() + " of " + masterFlights.size() + " flights");

        if (displayedFlights.isEmpty()) {
            VBox emptyCard = new VBox(16);
            emptyCard.getStyleClass().add("card");
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPadding(new Insets(40));

            Label noFlightsTitle = new Label("No flights match your filters");
            noFlightsTitle.getStyleClass().add("section-title");

            Label noFlightsMsg = new Label("Try adjusting your price range, departure time, or airline filters.");
            noFlightsMsg.getStyleClass().add("text-secondary");

            Button clearBtn = new Button("Clear Filters");
            clearBtn.getStyleClass().add("btn-outline");
            clearBtn.setOnAction(e -> resetFilters());

            emptyCard.getChildren().addAll(noFlightsTitle, noFlightsMsg, clearBtn);
            flightsContainer.getChildren().add(emptyCard);
            return;
        }

        // Render card for each matching flight
        for (Flight flight : displayedFlights) {
            flightsContainer.getChildren().add(createFlightCard(flight));
        }
    }

    /**
     * Builds an individual flight card per the prompt design specifications:
     *  - Large departure time, airport code
     *  - Center line with duration and dot
     *  - Large arrival time, airport code
     *  - Airline badge and flight number
     *  - Price and orange Select button
     */
    private HBox createFlightCard(Flight flight) {
        HBox card = new HBox(20);
        card.getStyleClass().add("flight-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // ── 1. Airline & Flight Number Column ──
        VBox airlineCol = new VBox(6);
        airlineCol.setAlignment(Pos.CENTER_LEFT);
        airlineCol.setMinWidth(110);

        Label airlineBadge = new Label(flight.getAirline());
        airlineBadge.getStyleClass().add("airline-badge");

        Label flightNoLabel = new Label(flight.getFlightNo());
        flightNoLabel.getStyleClass().add("text-secondary");

        airlineCol.getChildren().addAll(airlineBadge, flightNoLabel);

        // ── 2. Departure Column ──
        VBox depCol = new VBox(2);
        depCol.setAlignment(Pos.CENTER_LEFT);
        depCol.setMinWidth(80);

        Label depTime = new Label(flight.getDepartureTime());
        depTime.getStyleClass().add("flight-time");

        Label depAirport = new Label(flight.getFrom());
        depAirport.getStyleClass().add("flight-airport");

        depCol.getChildren().addAll(depTime, depAirport);

        // ── 3. Flight Track / Duration Line ──
        VBox trackCol = new VBox(4);
        trackCol.setAlignment(Pos.CENTER);
        trackCol.setMinWidth(140);

        Label durationLabel = new Label(flight.getDurationString());
        durationLabel.getStyleClass().add("flight-duration");

        HBox lineWithTrack = new HBox(6);
        lineWithTrack.setAlignment(Pos.CENTER);

        Region originDot = new Region();
        originDot.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #2B2D7C; -fx-border-width: 2; -fx-border-radius: 50; -fx-background-radius: 50; -fx-min-width: 8; -fx-min-height: 8; -fx-max-width: 8; -fx-max-height: 8;");

        Region line = new Region();
        line.getStyleClass().add("flight-line");
        HBox.setHgrow(line, Priority.ALWAYS);

        Label planeIcon = new Label("✈");
        planeIcon.setStyle("-fx-font-size: 11; -fx-text-fill: #2B2D7C;");

        Region line2 = new Region();
        line2.getStyleClass().add("flight-line");
        HBox.setHgrow(line2, Priority.ALWAYS);

        Region destDot = new Region();
        destDot.setStyle("-fx-background-color: #2B2D7C; -fx-border-radius: 50; -fx-background-radius: 50; -fx-min-width: 8; -fx-min-height: 8; -fx-max-width: 8; -fx-max-height: 8;");

        lineWithTrack.getChildren().addAll(originDot, line, planeIcon, line2, destDot);

        Label nonstopBadge = new Label("Non-stop");
        nonstopBadge.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #4B5563; -fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 4;");

        trackCol.getChildren().addAll(durationLabel, lineWithTrack, nonstopBadge);

        // ── 4. Arrival Column ──
        VBox arrCol = new VBox(2);
        arrCol.setAlignment(Pos.CENTER_LEFT);
        arrCol.setMinWidth(80);

        Label arrTime = new Label(flight.getArrivalTime());
        arrTime.getStyleClass().add("flight-time");

        Label arrAirport = new Label(flight.getTo());
        arrAirport.getStyleClass().add("flight-airport");

        arrCol.getChildren().addAll(arrTime, arrAirport);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // ── 5. Price & Select Action Column ──
        VBox actionCol = new VBox(6);
        actionCol.setAlignment(Pos.CENTER_RIGHT);
        actionCol.setMinWidth(140);

        Label seatsLeft = new Label(flight.getAvailableSeatCount() + " seats available");
        seatsLeft.setStyle("-fx-background-color: #ECFDF5; -fx-text-fill: #065F46; -fx-font-size: 11; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4;");

        Label priceLabel = new Label("₹" + (int) flight.getPrice());
        priceLabel.getStyleClass().add("flight-price");

        Label perPax = new Label("per passenger");
        perPax.getStyleClass().add("price-per-person");

        Button selectBtn = new Button("SELECT");
        selectBtn.getStyleClass().add("btn-accent");
        selectBtn.setOnAction(e -> handleSelectFlight(flight));

        actionCol.getChildren().addAll(seatsLeft, priceLabel, perPax, selectBtn);

        card.getChildren().addAll(airlineCol, depCol, trackCol, arrCol, spacer, actionCol);
        return card;
    }

    /**
     * Handles flight selection:
     * Stores selected flight and passenger count in AeroReserveApp,
     * and transitions directly to Passenger details & Seat map screen (Stage 5).
     */
    private void handleSelectFlight(Flight flight) {
        AeroReserveApp.selectedFlight = flight;
        AeroReserveApp.passengerCount = passengerCount;

        // Transition to Stage 5: Passenger Details & Interactive Seat Map
        PassengerAndSeatScreen seatScreen = new PassengerAndSeatScreen(flight, passengerCount);
        AeroReserveApp.showScreen(seatScreen.getView());
    }

    /**
     * Returns the root node of this screen.
     */
    public Parent getView() {
        return root;
    }
}

