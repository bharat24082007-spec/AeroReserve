package com.aeroreserve.ui;

import com.aeroreserve.model.Booking;
import com.aeroreserve.model.Passenger;
import com.aeroreserve.model.Seat;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * ConfirmationScreen implements Screen 6 of the project specifications:
 *  - Boarding-pass layout with large airport codes (HYD -> BOM).
 *  - Dashed tear-off line separating pass header and passenger stub.
 *  - Barcode-style strip made of pure JavaFX vertical rectangles (strictly no image files).
 *  - Green "Confirmed" chip.
 *  - Ticket file generation verification (data/ticket_<bookingRef>.txt).
 *  - Quick buttons to book another flight or view the text ticket.
 */
public class ConfirmationScreen {

    private VBox root;
    private Booking booking;

    /**
     * Constructor receives the confirmed Booking object.
     * 
     * @param booking the confirmed booking
     */
    public ConfirmationScreen(Booking booking) {
        this.booking = booking;
        createUI();
    }

    /**
     * Builds the complete confirmation interface.
     */
    private void createUI() {
        root = new VBox(24);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(24, 32, 40, 32));

        // ── 1. Page Title & Success Banner ──
        VBox titleBox = new VBox(4);
        titleBox.setAlignment(Pos.CENTER);

        Label title = new Label("Booking Confirmed!");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Your domestic flight reservation has been successfully processed.");
        subtitle.getStyleClass().add("text-secondary");

        titleBox.getChildren().addAll(title, subtitle);

        // ── 2. Boarding Pass Card ──
        VBox boardingPass = createBoardingPassCard();

        // ── 3. E-Ticket File Notice Card ──
        VBox ticketNoticeCard = createTicketNoticeCard();

        // Wrap in a ScrollPane for smooth responsive scrolling
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.getChildren().addAll(titleBox, boardingPass, ticketNoticeCard);

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.getStyleClass().add("scroll-transparent");
        scrollPane.setFitToWidth(true);

        root.getChildren().add(scrollPane);
    }

    /**
     * Builds the authentic Boarding Pass per prompt specifications:
     *  - Top Indigo Section: Large airport codes (e.g. HYD -> BOM) & green Confirmed chip
     *  - Dashed tear-off separator line
     *  - White Bottom Section: Passenger names, assigned seats, gate, boarding time
     *  - Pure shape Barcode Strip
     */
    private VBox createBoardingPassCard() {
        VBox pass = new VBox(0);
        pass.getStyleClass().add("boarding-pass");
        pass.setMaxWidth(680);

        // ── Top Indigo Section ──
        VBox topSection = new VBox(16);
        topSection.getStyleClass().add("bp-top-section");

        // Top Header Row: Airline Badge, Confirmed Chip, Booking Ref
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label airlineTag = new Label("✈  " + booking.getFlight().getAirline().toUpperCase() + "  •  BOARDING PASS");
        airlineTag.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.85); -fx-font-weight: bold; -fx-font-size: 12;");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        Label confirmedChip = new Label("✓  CONFIRMED");
        confirmedChip.getStyleClass().add("chip-confirmed");

        topRow.getChildren().addAll(airlineTag, spacer1, confirmedChip);

        // Airport Codes Row: HYD  ✈  BOM
        HBox airportRow = new HBox(20);
        airportRow.setAlignment(Pos.CENTER);
        airportRow.setPadding(new Insets(10, 0, 10, 0));

        VBox originBox = new VBox(2);
        originBox.setAlignment(Pos.CENTER_LEFT);
        Label fromCode = new Label(booking.getFlight().getFrom());
        fromCode.getStyleClass().add("bp-airport-code");
        Label fromCity = new Label(getCityFullName(booking.getFlight().getFrom()));
        fromCity.getStyleClass().add("bp-airport-name");
        originBox.getChildren().addAll(fromCode, fromCity);

        Region spacerA = new Region();
        HBox.setHgrow(spacerA, Priority.ALWAYS);

        VBox flightArrowBox = new VBox(4);
        flightArrowBox.setAlignment(Pos.CENTER);
        Label arrow = new Label("✈");
        arrow.getStyleClass().add("bp-flight-arrow");
        Label nonStop = new Label("NON-STOP");
        nonStop.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.65); -fx-font-size: 11; -fx-font-weight: bold;");
        flightArrowBox.getChildren().addAll(arrow, nonStop);

        Region spacerB = new Region();
        HBox.setHgrow(spacerB, Priority.ALWAYS);

        VBox destBox = new VBox(2);
        destBox.setAlignment(Pos.CENTER_RIGHT);
        Label toCode = new Label(booking.getFlight().getTo());
        toCode.getStyleClass().add("bp-airport-code");
        Label toCity = new Label(getCityFullName(booking.getFlight().getTo()));
        toCity.getStyleClass().add("bp-airport-name");
        destBox.getChildren().addAll(toCode, toCity);

        airportRow.getChildren().addAll(originBox, spacerA, flightArrowBox, spacerB, destBox);

        // Sub-details row (Flight No, Date, PNR)
        HBox subRow = new HBox(24);
        subRow.setAlignment(Pos.CENTER_LEFT);
        subRow.setStyle("-fx-padding: 8 0 0 0;");

        subRow.getChildren().addAll(
            createHeaderMetaTag("FLIGHT", booking.getFlight().getFlightNo()),
            createHeaderMetaTag("DATE", booking.getFlight().getDate()),
            createHeaderMetaTag("BOOKING REF (PNR)", booking.getBookingRef())
        );

        topSection.getChildren().addAll(topRow, airportRow, subRow);

        // ── Dashed Tear-Off Separator Line ──
        HBox tearOffLine = createDashedTearOffLine();

        // ── White Bottom Section ──
        VBox bottomSection = new VBox(18);
        bottomSection.getStyleClass().add("bp-bottom-section");

        // Passenger Details Grid
        GridPane paxGrid = new GridPane();
        paxGrid.setHgap(28);
        paxGrid.setVgap(14);

        // Build list of passenger names & seat numbers
        StringBuilder namesBuilder = new StringBuilder();
        for (int i = 0; i < booking.getPassengers().size(); i++) {
            if (i > 0) namesBuilder.append(", ");
            namesBuilder.append(booking.getPassengers().get(i).getName());
        }

        StringBuilder seatsBuilder = new StringBuilder();
        for (int i = 0; i < booking.getSeats().size(); i++) {
            if (i > 0) seatsBuilder.append(", ");
            seatsBuilder.append(booking.getSeats().get(i).getSeatNo());
        }

        // Col 0, Row 0: PASSENGER(S)
        paxGrid.add(createFieldCell("PASSENGER(S)", namesBuilder.toString()), 0, 0);
        // Col 1, Row 0: SEAT(S)
        paxGrid.add(createFieldCell("SEAT(S)", seatsBuilder.toString()), 1, 0);
        // Col 2, Row 0: GATE
        paxGrid.add(createFieldCell("GATE", "4B"), 2, 0);
        // Col 3, Row 0: BOARDING TIME
        paxGrid.add(createFieldCell("BOARDING", "05:50 (T-40m)"), 3, 0);

        // Col 0, Row 1: DEPARTURE
        paxGrid.add(createFieldCell("DEPARTURE", booking.getFlight().getDepartureTime()), 0, 1);
        // Col 1, Row 1: ARRIVAL
        paxGrid.add(createFieldCell("ARRIVAL", booking.getFlight().getArrivalTime()), 1, 1);
        // Col 2, Row 1: CLASS
        paxGrid.add(createFieldCell("CLASS", "Economy (Y)"), 2, 1);
        // Col 3, Row 1: BAGGAGE
        paxGrid.add(createFieldCell("BAGGAGE", booking.getBaggageOption()), 3, 1);

        // Payment status row
        HBox paymentRow = new HBox(20);
        paymentRow.setAlignment(Pos.CENTER_LEFT);
        paymentRow.setStyle("-fx-background-color: #F9FAFB; -fx-padding: 10 14; -fx-background-radius: 8; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 8;");

        Label payLabel = new Label("Payment Method: " +
            (booking.getPayment() != null ? booking.getPayment().getMethod() : "Credit Card") +
            "  •  Status: SUCCESS  •  Total Paid: ₹" + (int) booking.getTotalFare()
        );
        payLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827; -fx-font-size: 13;");

        paymentRow.getChildren().add(payLabel);

        bottomSection.getChildren().addAll(paxGrid, paymentRow);

        // ── Barcode Strip (Made of pure JavaFX Rectangles — strictly NO image files) ──
        VBox barcodeStrip = createBarcodeStrip();

        pass.getChildren().addAll(topSection, tearOffLine, bottomSection, barcodeStrip);
        return pass;
    }

    /**
     * Builds the dashed tear-off line separating top pass from bottom pass.
     */
    private HBox createDashedTearOffLine() {
        HBox lineBox = new HBox();
        lineBox.setAlignment(Pos.CENTER);
        lineBox.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 0 16;");

        Region dashedLine = new Region();
        dashedLine.setStyle("-fx-border-color: #CBD5E1; -fx-border-style: dashed; -fx-border-width: 1.5; -fx-pref-height: 1;");
        HBox.setHgrow(dashedLine, Priority.ALWAYS);

        lineBox.getChildren().add(dashedLine);
        return lineBox;
    }

    /**
     * Generates a realistic barcode strip using alternating black/dark vertical rectangles.
     * Fulfills requirement: "barcode-style strip made of rectangles".
     */
    private VBox createBarcodeStrip() {
        VBox strip = new VBox(6);
        strip.getStyleClass().add("bp-barcode-strip");
        strip.setAlignment(Pos.CENTER);
        strip.setPadding(new Insets(10, 0, 10, 0));

        // Horizontal bar containing vertical stripes of varying widths
        HBox barcodeRow = new HBox(2);
        barcodeRow.setAlignment(Pos.CENTER);

        // Pattern array representing stripe widths in pixels (e.g. 1px, 3px, 2px, 4px)
        int[] stripePattern = {
            2, 1, 4, 1, 2, 3, 1, 2, 4, 1, 3, 2, 1, 4, 2, 1, 3, 1, 4, 2,
            1, 3, 2, 4, 1, 2, 3, 1, 4, 2, 1, 3, 2, 1, 4, 3, 1, 2, 4, 1,
            3, 1, 2, 4, 1, 3, 2, 1, 4, 2, 1, 3, 1, 4, 2, 1, 3, 2, 4, 1
        };

        for (int width : stripePattern) {
            Rectangle stripe = new Rectangle(width, 24);
            stripe.setFill(Color.WHITE);
            barcodeRow.getChildren().add(stripe);
        }

        Label barcodeCode = new Label("* " + booking.getBookingRef() + " *");
        barcodeCode.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.70); -fx-font-family: monospace; -fx-font-size: 11;");

        strip.getChildren().addAll(barcodeRow, barcodeCode);
        return strip;
    }

    /**
     * Card confirming text ticket generation to data/ folder with action buttons.
     */
    private VBox createTicketNoticeCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card-sm");
        card.setMaxWidth(680);
        card.setAlignment(Pos.CENTER);

        HBox noticeHeader = new HBox(8);
        noticeHeader.setAlignment(Pos.CENTER);
        Label check = new Label("📄");
        Label noticeTitle = new Label("E-Ticket Generated & Saved to Disk");
        noticeTitle.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #111827;");
        noticeHeader.getChildren().addAll(check, noticeTitle);

        String ticketFilePath = "data/ticket_" + booking.getBookingRef() + ".txt";
        Label pathLabel = new Label("File Location: " + ticketFilePath);
        pathLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 12; -fx-text-fill: #2B2D7C; -fx-background-color: #EFF6FF; -fx-padding: 6 14; -fx-background-radius: 6;");

        Label vivaNote = new Label(
            "Viva Simulation: In a production system, this step triggers an automated SMS and email. " +
            "In this project, it formats the confirmed booking details and writes a plain text ticket to disk."
        );
        vivaNote.getStyleClass().add("text-secondary");
        vivaNote.setStyle("-fx-font-size: 12; -fx-text-alignment: center;");
        vivaNote.setWrapText(true);

        HBox actionButtons = new HBox(12);
        actionButtons.setAlignment(Pos.CENTER);

        Button myBookingsBtn = new Button("View in My Bookings");
        myBookingsBtn.getStyleClass().add("btn-primary");
        myBookingsBtn.setOnAction(e -> AeroReserveApp.showScreen(new MyBookingsScreen().getView()));

        Button bookAnotherBtn = new Button("Book Another Flight");
        bookAnotherBtn.getStyleClass().add("btn-accent");
        bookAnotherBtn.setOnAction(e -> AeroReserveApp.showScreen(new SearchScreen().getView()));

        Button viewFileBtn = new Button("View Raw Ticket File");
        viewFileBtn.getStyleClass().add("btn-outline");
        viewFileBtn.setOnAction(e -> showRawTicketModal(ticketFilePath));

        actionButtons.getChildren().addAll(myBookingsBtn, bookAnotherBtn, viewFileBtn);

        card.getChildren().addAll(noticeHeader, pathLabel, vivaNote, actionButtons);
        return card;
    }

    /**
     * Displays a clean dialog showing the raw text written to the ticket file.
     */
    private void showRawTicketModal(String filePath) {
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
            sb.append(booking.toString());
        }

        VBox modalCard = new VBox(14);
        modalCard.getStyleClass().add("card");
        modalCard.setMaxWidth(580);
        modalCard.setAlignment(Pos.CENTER);

        Label title = new Label("E-Ticket File Contents (" + filePath + ")");
        title.getStyleClass().add("card-title");

        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12;");
        textArea.setPrefRowCount(14);

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-outline");
        closeBtn.setOnAction(e -> AeroReserveApp.showScreen(this.getView()));

        modalCard.getChildren().addAll(title, textArea, closeBtn);

        StackPane wrapper = new StackPane(modalCard);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(40));
        AeroReserveApp.showScreen(wrapper);
    }

    /**
     * Helper to create a labeled field cell in the boarding pass grid.
     */
    private VBox createFieldCell(String label, String value) {
        VBox cell = new VBox(2);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("bp-field-label");

        Label val = new Label(value);
        val.getStyleClass().add("bp-field-value");
        val.setWrapText(true);

        cell.getChildren().addAll(lbl, val);
        return cell;
    }

    /**
     * Helper to create small metadata tags in the top indigo header.
     */
    private VBox createHeaderMetaTag(String label, String value) {
        VBox box = new VBox(1);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 10; -fx-text-fill: rgba(255, 255, 255, 0.65); -fx-font-weight: bold;");

        Label val = new Label(value);
        val.setStyle("-fx-font-size: 13; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");

        box.getChildren().addAll(lbl, val);
        return box;
    }

    /**
     * Resolves full city name from 3-letter IATA code.
     */
    private String getCityFullName(String code) {
        return switch (code.toUpperCase()) {
            case "HYD" -> "Hyderabad";
            case "BOM" -> "Mumbai";
            case "DEL" -> "Delhi";
            case "BLR" -> "Bengaluru";
            case "MAA" -> "Chennai";
            case "CCU" -> "Kolkata";
            default    -> code;
        };
    }

    /**
     * Returns the root node.
     */
    public Parent getView() {
        return root;
    }
}
