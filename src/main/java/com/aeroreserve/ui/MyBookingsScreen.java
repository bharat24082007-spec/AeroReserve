package com.aeroreserve.ui;

import com.aeroreserve.model.Booking;
import com.aeroreserve.model.Booking.BookingStatus;
import com.aeroreserve.model.Passenger;
import com.aeroreserve.model.Seat;
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
import java.util.ArrayList;

/**
 * MyBookingsScreen implements the customer itinerary dashboard (Stage 7).
 *
 * Features:
 *  - Displays all bookings associated with the currently authenticated customer.
 *  - Real-time search/filter by PNR reference code or destination airport.
 *  - Rich booking cards with route trajectory, seat assignments, and status badges.
 *  - Action: "View Boarding Pass" navigating to ConfirmationScreen.
 *  - Action: "View E-Ticket" displaying the generated file data/ticket_<bookingRef>.txt.
 *  - Action: "Cancel Booking" which releases seats back to inventory and updates CSV.
 *  - Clean empty-state view with direct link to search domestic flights.
 */
public class MyBookingsScreen {

    private ScrollPane scrollPane;
    private VBox bookingsListContainer;
    private TextField searchField;
    private ComboBox<String> statusFilter;
    private Label countBadge;

    /**
     * Constructs the My Bookings screen.
     */
    public MyBookingsScreen() {
        if (AeroReserveApp.headerBar != null) {
            AeroReserveApp.headerBar.setActiveTab("BOOKINGS");
        }
        createUI();
    }

    /**
     * Builds the main layout.
     */
    private void createUI() {
        VBox root = new VBox(24);
        root.setPadding(new Insets(28, 48, 48, 48));
        root.setMaxWidth(1100);
        root.setAlignment(Pos.TOP_CENTER);

        // ── 1. Page Header ──
        VBox headerBox = new VBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setMaxWidth(1000);

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label pageTitle = new Label("My Bookings");
        pageTitle.getStyleClass().add("page-title");

        countBadge = new Label("0 Bookings");
        countBadge.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #1D4ED8; -fx-font-size: 13; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 12;");

        titleRow.getChildren().addAll(pageTitle, countBadge);

        Label pageSubtitle = new Label("Manage your upcoming journeys, view boarding passes, and download e-tickets.");
        pageSubtitle.getStyleClass().add("text-secondary");

        headerBox.getChildren().addAll(titleRow, pageSubtitle);

        // ── 2. Filter & Search Controls Bar ──
        HBox filterBar = new HBox(16);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setMaxWidth(1000);
        filterBar.getStyleClass().add("card-sm");

        Label searchLabel = new Label("Find Booking:");
        searchLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");

        searchField = new TextField();
        searchField.setPromptText("Search by PNR (e.g. ARB-00001) or City Code...");
        searchField.getStyleClass().add("text-input-custom");
        searchField.setPrefWidth(320);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshBookings());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label filterLabel = new Label("Status:");
        filterLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All Statuses", "Confirmed Only", "Cancelled Only");
        statusFilter.setValue("All Statuses");
        statusFilter.getStyleClass().add("combo-custom");
        statusFilter.setPrefWidth(170);
        statusFilter.setOnAction(e -> refreshBookings());

        Button bookNewBtn = new Button("✈  Book New Flight");
        bookNewBtn.getStyleClass().add("btn-accent");
        bookNewBtn.setOnAction(e -> AeroReserveApp.showScreen(new SearchScreen().getView()));

        filterBar.getChildren().addAll(searchLabel, searchField, spacer, filterLabel, statusFilter, bookNewBtn);

        // ── 3. Bookings List Container ──
        bookingsListContainer = new VBox(16);
        bookingsListContainer.setMaxWidth(1000);
        bookingsListContainer.setAlignment(Pos.TOP_CENTER);

        // Initial populate
        refreshBookings();

        root.getChildren().addAll(headerBox, filterBar, bookingsListContainer);

        scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-transparent");
    }

    /**
     * Queries BookingService, filters results, and dynamically populates cards.
     */
    private void refreshBookings() {
        bookingsListContainer.getChildren().clear();

        if (AeroReserveApp.currentUser == null) {
            return;
        }

        String userEmail = AeroReserveApp.currentUser.getEmail();
        ArrayList<Booking> userBookings = AeroReserveApp.bookingService.getBookingsByCustomer(userEmail);

        // Apply search query and status filter
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String selectedStatus = statusFilter.getValue();

        ArrayList<Booking> filtered = new ArrayList<>();
        for (Booking b : userBookings) {
            // Status check
            if ("Confirmed Only".equals(selectedStatus) && b.getStatus() != BookingStatus.CONFIRMED) {
                continue;
            }
            if ("Cancelled Only".equals(selectedStatus) && b.getStatus() != BookingStatus.CANCELLED) {
                continue;
            }

            // Search query check (matches PNR, flight number, origin, or destination)
            if (!query.isEmpty()) {
                boolean matchPnr = b.getBookingRef().toLowerCase().contains(query);
                boolean matchFlight = b.getFlight().getFlightNo().toLowerCase().contains(query);
                boolean matchFrom = b.getFlight().getFrom().toLowerCase().contains(query);
                boolean matchTo = b.getFlight().getTo().toLowerCase().contains(query);
                boolean matchAirline = b.getFlight().getAirline().toLowerCase().contains(query);

                if (!matchPnr && !matchFlight && !matchFrom && !matchTo && !matchAirline) {
                    continue;
                }
            }
            filtered.add(b);
        }

        // Update count badge
        countBadge.setText(filtered.size() + (filtered.size() == 1 ? " Booking" : " Bookings"));

        // Render empty state if nothing found
        if (filtered.isEmpty()) {
            bookingsListContainer.getChildren().add(buildEmptyState(userBookings.isEmpty()));
            return;
        }

        // Render cards
        for (Booking booking : filtered) {
            bookingsListContainer.getChildren().add(buildBookingCard(booking));
        }
    }

    /**
     * Builds a single booking card row.
     *
     * @param booking the Booking object to render
     * @return VBox card
     */
    private VBox buildBookingCard(Booking booking) {
        VBox card = new VBox(14);
        card.getStyleClass().add("booking-row-card");

        // ── Top Header Row ──
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label pnrBadge = new Label("PNR: " + booking.getBookingRef());
        pnrBadge.getStyleClass().add("badge-pnr");

        Label statusChip = new Label(booking.getStatus().name());
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            statusChip.getStyleClass().add("chip-confirmed");
        } else {
            statusChip.getStyleClass().add("chip-cancelled");
        }

        Region spacerTop = new Region();
        HBox.setHgrow(spacerTop, Priority.ALWAYS);

        VBox fareBox = new VBox(2);
        fareBox.setAlignment(Pos.CENTER_RIGHT);
        Label fareTitle = new Label("Total Paid");
        fareTitle.setStyle("-fx-font-size: 11; -fx-text-fill: #9CA3AF; -fx-font-weight: bold;");
        Label fareVal = new Label("₹" + String.format("%,.0f", booking.getTotalFare()));
        fareVal.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #111827;");
        fareBox.getChildren().addAll(fareTitle, fareVal);

        topRow.getChildren().addAll(pnrBadge, statusChip, spacerTop, fareBox);

        // Divider
        Separator sep = new Separator();

        // ── Middle Flight Route Row ──
        HBox routeRow = new HBox(20);
        routeRow.setAlignment(Pos.CENTER_LEFT);

        // Airline pill
        Label airlineBadge = new Label(booking.getFlight().getAirline() + " • " + booking.getFlight().getFlightNo());
        airlineBadge.getStyleClass().add("airline-badge");

        // Departure Info
        VBox depBox = new VBox(2);
        depBox.setAlignment(Pos.CENTER_LEFT);
        Label depTime = new Label(booking.getFlight().getDepartureTime());
        depTime.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label depCity = new Label(booking.getFlight().getFrom());
        depCity.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #6B7280;");
        depBox.getChildren().addAll(depTime, depCity);

        // Flight duration line
        VBox trackBox = new VBox(2);
        trackBox.setAlignment(Pos.CENTER);
        trackBox.setPrefWidth(120);
        Label durationLbl = new Label(booking.getFlight().getDurationString());
        durationLbl.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #6B7280;");
        Label lineLbl = new Label("────────✈");
        lineLbl.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12;");
        trackBox.getChildren().addAll(durationLbl, lineLbl);

        // Arrival Info
        VBox arrBox = new VBox(2);
        arrBox.setAlignment(Pos.CENTER_LEFT);
        Label arrTime = new Label(booking.getFlight().getArrivalTime());
        arrTime.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label arrCity = new Label(booking.getFlight().getTo());
        arrCity.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #6B7280;");
        arrBox.getChildren().addAll(arrTime, arrCity);

        // Travel Date Badge
        Label dateBadge = new Label("📅 Travel Date: " + booking.getFlight().getDate());
        dateBadge.getStyleClass().add("badge-route");

        routeRow.getChildren().addAll(airlineBadge, depBox, trackBox, arrBox, dateBadge);

        // ── Passenger & Seat Details ──
        HBox paxBox = new HBox(8);
        paxBox.setAlignment(Pos.CENTER_LEFT);

        Label paxLabel = new Label("Travellers & Assigned Seats:");
        paxLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #4B5563;");
        paxBox.getChildren().add(paxLabel);

        ArrayList<Passenger> passengers = booking.getPassengers();
        ArrayList<Seat> seats = booking.getSeats();

        for (int i = 0; i < passengers.size(); i++) {
            String pName = passengers.get(i).getName();
            String sNo = (i < seats.size()) ? seats.get(i).getSeatNo() : "N/A";

            Label pChip = new Label("👤 " + pName + " (" + sNo + ")");
            pChip.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #1F2937; -fx-font-size: 11; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4;");
            paxBox.getChildren().add(pChip);
        }

        Label bagChip = new Label("🧳 " + booking.getBaggageOption());
        bagChip.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-font-size: 11; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4;");
        paxBox.getChildren().add(bagChip);

        // ── Action Buttons Row ──
        HBox actionsRow = new HBox(12);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            Button boardingPassBtn = new Button("View Boarding Pass");
            boardingPassBtn.getStyleClass().add("btn-primary");
            boardingPassBtn.setOnAction(e -> AeroReserveApp.showScreen(new ConfirmationScreen(booking).getView()));

            Button eTicketBtn = new Button("View E-Ticket");
            eTicketBtn.getStyleClass().add("btn-outline");
            eTicketBtn.setOnAction(e -> showTicketDialog(booking));

            Button cancelBtn = new Button("Cancel Booking");
            cancelBtn.getStyleClass().add("btn-danger");
            cancelBtn.setOnAction(e -> handleCancellation(booking));

            actionsRow.getChildren().addAll(boardingPassBtn, eTicketBtn, cancelBtn);
        } else {
            Label cancelledNote = new Label("❌ Reservation cancelled. Seats were freed for other travellers.");
            cancelledNote.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12; -fx-font-weight: bold;");

            Region cancelSpacer = new Region();
            HBox.setHgrow(cancelSpacer, Priority.ALWAYS);

            Button eTicketBtn = new Button("View Archive Ticket");
            eTicketBtn.getStyleClass().add("btn-outline");
            eTicketBtn.setOnAction(e -> showTicketDialog(booking));

            actionsRow.getChildren().addAll(cancelledNote, cancelSpacer, eTicketBtn);
        }

        card.getChildren().addAll(topRow, sep, routeRow, paxBox, actionsRow);
        return card;
    }

    /**
     * Handles cancellation confirmation and inventory release.
     *
     * @param booking the booking to cancel
     */
    private void handleCancellation(Booking booking) {
        StringBuilder seatList = new StringBuilder();
        for (Seat s : booking.getSeats()) {
            if (seatList.length() > 0) seatList.append(", ");
            seatList.append(s.getSeatNo());
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Booking Cancellation");
        confirmAlert.setHeaderText("Cancel Booking " + booking.getBookingRef() + "?");
        confirmAlert.setContentText(
            "Flight: " + booking.getFlight().getFlightNo() + " (" + booking.getFlight().getFrom() + " → " + booking.getFlight().getTo() + ")\n" +
            "Seats to release: " + seatList.toString() + "\n\n" +
            "Are you sure you want to cancel this booking? This will immediately return your seats back to inventory."
        );

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    AeroReserveApp.bookingService.cancelBooking(booking.getBookingRef());

                    Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                    infoAlert.setTitle("Booking Cancelled");
                    infoAlert.setHeaderText("Cancellation Successful");
                    infoAlert.setContentText("Booking " + booking.getBookingRef() + " has been cancelled. Seats " + seatList.toString() + " are now available for booking.");
                    infoAlert.showAndWait();

                    refreshBookings();
                } catch (Exception ex) {
                    Alert errAlert = new Alert(Alert.AlertType.ERROR);
                    errAlert.setTitle("Cancellation Failed");
                    errAlert.setHeaderText("Error cancelling booking");
                    errAlert.setContentText(ex.getMessage());
                    errAlert.showAndWait();
                }
            }
        });
    }

    /**
     * Shows modal dialog displaying the raw ticket file content.
     *
     * @param booking the booking whose ticket file should be shown
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
            sb.append("Generated E-Ticket summary for ").append(booking.getBookingRef()).append(":\n\n");
            sb.append(booking.toString());
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("E-Ticket File Viewer — " + booking.getBookingRef());

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #FFFFFF;");

        Label modalTitle = new Label("Passenger E-Ticket Archive");
        modalTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1A1D36;");

        Label pathInfo = new Label("Source File: " + filePath);
        pathInfo.setStyle("-fx-font-size: 11; -fx-text-fill: #6B7280;");

        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setPrefRowCount(18);
        textArea.setPrefColumnCount(55);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12;");

        HBox closeBar = new HBox();
        closeBar.setAlignment(Pos.CENTER_RIGHT);
        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-primary");
        closeBtn.setOnAction(e -> dialog.close());
        closeBar.getChildren().add(closeBtn);

        content.getChildren().addAll(modalTitle, pathInfo, textArea, closeBar);

        Scene dialogScene = new Scene(content, 620, 480);
        String cssPath = getClass().getResource("/aeroreserve.css").toExternalForm();
        dialogScene.getStylesheets().add(cssPath);

        dialog.setScene(dialogScene);
        dialog.showAndWait();
    }

    /**
     * Builds the empty state node when no bookings match.
     *
     * @param isCompletelyEmpty true if customer has zero bookings overall
     * @return VBox empty card
     */
    private VBox buildEmptyState(boolean isCompletelyEmpty) {
        VBox emptyCard = new VBox(16);
        emptyCard.getStyleClass().add("empty-state-card");
        emptyCard.setMaxWidth(800);

        Label iconLbl = new Label("✈");
        iconLbl.getStyleClass().add("empty-state-icon");

        Label titleLbl = new Label(isCompletelyEmpty ? "No Bookings Yet" : "No Matching Bookings Found");
        titleLbl.getStyleClass().add("empty-state-title");

        Label descLbl = new Label(isCompletelyEmpty
            ? "You haven't booked any flights yet. Search India's premier domestic routes and reserve your seats in minutes!"
            : "No bookings match your current search criteria. Try modifying your filter or clear the search text."
        );
        descLbl.getStyleClass().add("empty-state-desc");
        descLbl.setWrapText(true);

        emptyCard.getChildren().addAll(iconLbl, titleLbl, descLbl);

        if (isCompletelyEmpty) {
            Button bookBtn = new Button("Search & Book Flights");
            bookBtn.getStyleClass().add("btn-accent");
            bookBtn.setOnAction(e -> AeroReserveApp.showScreen(new SearchScreen().getView()));
            emptyCard.getChildren().add(bookBtn);
        }

        return emptyCard;
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
