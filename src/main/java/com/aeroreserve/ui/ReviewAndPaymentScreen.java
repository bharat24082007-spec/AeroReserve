package com.aeroreserve.ui;

import com.aeroreserve.exception.InvalidInputException;
import com.aeroreserve.exception.PaymentFailedException;
import com.aeroreserve.exception.SeatNotAvailableException;
import com.aeroreserve.model.*;
import com.aeroreserve.model.Payment.PaymentMethod;
import com.aeroreserve.service.BookingService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;

/**
 * ReviewAndPaymentScreen implements Screen 5 of the specification:
 *  - Left side: Full booking review details (flight, passengers, seats, baggage)
 *    and payment mode selection (Credit Card, Debit Card, Net Banking, Wallet).
 *  - Right side: Sticky fare summary card with base fare, extra legroom fee,
 *    baggage surcharge, and total amount.
 *  - Simulated transaction:
 *      * ~15% simulated failure via PaymentService.
 *      * Handles PaymentFailedException gracefully with inline retry feedback (no alert popups).
 *  - On success: Creates confirmed booking, saves ticket to text file, and proceeds to ConfirmationScreen.
 */
public class ReviewAndPaymentScreen {

    private BorderPane root;

    // Data references from shared state
    private Flight flight;
    private ArrayList<Passenger> passengers;
    private ArrayList<String> seatNos;
    private String baggageOption;
    private double totalFare;

    // Selected payment mode (default: CREDIT_CARD)
    private PaymentMethod selectedMethod = PaymentMethod.CREDIT_CARD;

    // Payment method buttons for toggle highlighting
    private Button creditCardBtn;
    private Button debitCardBtn;
    private Button netBankingBtn;
    private Button walletBtn;

    // Payment dynamic input fields
    private VBox paymentDetailsBox;
    private TextField cardNumberField;
    private TextField cardHolderField;
    private TextField cardExpiryField;
    private TextField cardCvvField;
    private ComboBox<String> bankCombo;
    private TextField upiIdField;

    // Feedback and action controls
    private Label inlineStatusLabel;
    private Button payBtn;
    private Button retryBtn;

    /**
     * Constructor reads shared session state and sets up UI.
     */
    public ReviewAndPaymentScreen() {
        this.flight = AeroReserveApp.selectedFlight;
        this.passengers = AeroReserveApp.pendingPassengers;
        this.seatNos = AeroReserveApp.pendingSeatNos;
        this.baggageOption = AeroReserveApp.pendingBaggageOption;

        // Calculate accurate total fare
        ArrayList<Seat> selectedSeats = new ArrayList<>();
        for (String sNo : seatNos) {
            Seat s = flight.getSeatByNumber(sNo);
            if (s != null) selectedSeats.add(s);
        }
        this.totalFare = BookingService.calculateFare(flight, selectedSeats, baggageOption);

        createUI();
    }

    /**
     * Assembles the 2-column layout (Review & Payment on left, Sticky Fare on right).
     */
    private void createUI() {
        root = new BorderPane();
        root.setPadding(new Insets(20, 32, 20, 32));

        // ── Top Header ──
        HBox topBar = createTopHeader();
        root.setTop(topBar);

        // ── Main Content: 2 Columns ──
        HBox contentBox = new HBox(24);
        contentBox.setPadding(new Insets(16, 0, 0, 0));

        // Left Column: Review Details & Payment Selection (Scrollable)
        VBox leftColumn = createLeftColumn();
        HBox.setHgrow(leftColumn, Priority.ALWAYS);

        // Right Column: Sticky Fare Breakdown
        VBox rightColumn = createStickyFareCard();
        rightColumn.setMinWidth(330);
        rightColumn.setMaxWidth(330);

        contentBox.getChildren().addAll(leftColumn, rightColumn);
        root.setCenter(contentBox);
    }

    /**
     * Top header bar with navigation back to Passenger and Seat selection.
     */
    private HBox createTopHeader() {
        HBox topBar = new HBox(16);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("card-sm");

        Button backBtn = new Button("← Back to Seats & Passengers");
        backBtn.getStyleClass().add("btn-outline");
        backBtn.setOnAction(e -> {
            AeroReserveApp.showScreen(new PassengerAndSeatScreen(flight, passengers.size()).getView());
        });

        Label title = new Label("Review Booking & Payment");
        title.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label stepIndicator = new Label("Step 3 of 4: Checkout");
        stepIndicator.getStyleClass().add("text-secondary");
        stepIndicator.setStyle("-fx-font-weight: bold;");

        topBar.getChildren().addAll(backBtn, title, spacer, stepIndicator);
        return topBar;
    }

    /**
     * Builds the left column containing Flight Review Card, Passenger List Card,
     * Payment Methods Card, and Pay Action Button.
     */
    private VBox createLeftColumn() {
        VBox col = new VBox(16);

        VBox cardsContainer = new VBox(16);

        // 1. Flight Review Card
        VBox flightCard = createFlightReviewCard();

        // 2. Passengers & Seats Review Card
        VBox paxCard = createPassengersReviewCard();

        // 3. Payment Method Selection Card
        VBox paymentCard = createPaymentCard();

        cardsContainer.getChildren().addAll(flightCard, paxCard, paymentCard);

        ScrollPane scrollPane = new ScrollPane(cardsContainer);
        scrollPane.getStyleClass().add("scroll-transparent");
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        col.getChildren().add(scrollPane);
        return col;
    }

    /**
     * Card summarizing the flight schedule and route.
     */
    private VBox createFlightReviewCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label airlineBadge = new Label(flight.getAirline());
        airlineBadge.getStyleClass().add("airline-badge");

        Label flightNoLabel = new Label(flight.getFlightNo());
        flightNoLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dateLabel = new Label(flight.getDate());
        dateLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #4B5563;");

        headerRow.getChildren().addAll(airlineBadge, flightNoLabel, spacer, dateLabel);

        // Timings & Route Row
        HBox routeRow = new HBox(20);
        routeRow.setAlignment(Pos.CENTER_LEFT);
        routeRow.setStyle("-fx-padding: 8 0;");

        VBox depBox = new VBox(2);
        Label depTime = new Label(flight.getDepartureTime());
        depTime.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label depAirport = new Label(flight.getFrom() + " (Terminal 2)");
        depAirport.getStyleClass().add("text-secondary");
        depBox.getChildren().addAll(depTime, depAirport);

        VBox trackBox = new VBox(4);
        trackBox.setAlignment(Pos.CENTER);
        Label duration = new Label(flight.getDurationString() + " • Non-stop");
        duration.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #6B7280;");
        Region line = new Region();
        line.setStyle("-fx-background-color: #D1D5DB; -fx-pref-height: 2; -fx-pref-width: 140;");
        trackBox.getChildren().addAll(duration, line);

        VBox arrBox = new VBox(2);
        Label arrTime = new Label(flight.getArrivalTime());
        arrTime.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label arrAirport = new Label(flight.getTo() + " (Terminal 1)");
        arrAirport.getStyleClass().add("text-secondary");
        arrBox.getChildren().addAll(arrTime, arrAirport);

        routeRow.getChildren().addAll(depBox, trackBox, arrBox);

        // Baggage selection indicator
        HBox baggageRow = new HBox(8);
        baggageRow.setAlignment(Pos.CENTER_LEFT);
        Label bagIcon = new Label("🧳");
        Label bagText = new Label("Baggage Allowance: " + baggageOption + " included");
        bagText.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #065F46; -fx-background-color: #ECFDF5; -fx-padding: 3 8; -fx-background-radius: 4;");
        baggageRow.getChildren().addAll(bagIcon, bagText);

        card.getChildren().addAll(headerRow, new Separator(), routeRow, baggageRow);
        return card;
    }

    /**
     * Card summarizing all passengers and their assigned seats.
     */
    private VBox createPassengersReviewCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label title = new Label("Traveller Details (" + passengers.size() + " Passenger" + (passengers.size() > 1 ? "s" : "") + ")");
        title.getStyleClass().add("card-title");
        card.getChildren().add(title);

        for (int i = 0; i < passengers.size(); i++) {
            Passenger p = passengers.get(i);
            String seatNo = (i < seatNos.size()) ? seatNos.get(i) : "-";
            Seat seatObj = flight.getSeatByNumber(seatNo);
            String seatTypeStr = (seatObj != null) ? seatObj.getType().name() : "";

            HBox paxRow = new HBox(12);
            paxRow.setAlignment(Pos.CENTER_LEFT);
            paxRow.setStyle("-fx-background-color: #F9FAFB; -fx-padding: 10 14; -fx-background-radius: 8; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 8;");

            Label indexLabel = new Label((i + 1) + ".");
            indexLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #6B7280;");

            VBox infoBox = new VBox(2);
            Label nameLabel = new Label(p.getName() + " (" + p.getGender() + ", " + p.getAge() + " yrs)");
            nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827; -fx-font-size: 13;");

            Label contactLabel = new Label("📞 " + p.getPhone() + "  •  ✉ " + p.getEmail());
            contactLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #6B7280;");
            infoBox.getChildren().addAll(nameLabel, contactLabel);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            // Seat Badge
            VBox seatBox = new VBox(2);
            seatBox.setAlignment(Pos.CENTER_RIGHT);
            Label seatBadge = new Label("Seat " + seatNo);
            seatBadge.setStyle("-fx-background-color: #FF6D00; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12; -fx-padding: 3 8; -fx-background-radius: 4;");
            Label seatTypeLabel = new Label(seatTypeStr);
            seatTypeLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #6B7280;");
            seatBox.getChildren().addAll(seatBadge, seatTypeLabel);

            paxRow.getChildren().addAll(indexLabel, infoBox, seatBox);
            card.getChildren().add(paxRow);
        }

        return card;
    }

    /**
     * Card for selecting payment mode and entering simulated details.
     */
    private VBox createPaymentCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label title = new Label("Select Payment Method");
        title.getStyleClass().add("card-title");

        // 4 Payment Mode Buttons
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        creditCardBtn = createPaymentMethodButton("💳  Credit Card", PaymentMethod.CREDIT_CARD);
        debitCardBtn  = createPaymentMethodButton("🏧  Debit Card", PaymentMethod.DEBIT_CARD);
        netBankingBtn = createPaymentMethodButton("🏛  Net Banking", PaymentMethod.NET_BANKING);
        walletBtn     = createPaymentMethodButton("📱  UPI / Mobile Wallet", PaymentMethod.WALLET);

        grid.add(creditCardBtn, 0, 0);
        grid.add(debitCardBtn, 1, 0);
        grid.add(netBankingBtn, 0, 1);
        grid.add(walletBtn, 1, 1);

        // Highlight default selected (Credit Card)
        updatePaymentMethodUI();

        // Dynamic Payment Fields Container
        paymentDetailsBox = new VBox(12);
        paymentDetailsBox.setStyle("-fx-background-color: #F9FAFB; -fx-padding: 16; -fx-background-radius: 8; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 8;");
        renderPaymentFields();

        // Inline status and error message
        inlineStatusLabel = new Label();
        inlineStatusLabel.setWrapText(true);
        inlineStatusLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");

        // Payment Buttons
        HBox actionsRow = new HBox(12);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        payBtn = new Button("Pay ₹" + (int) totalFare + " & Confirm Booking");
        payBtn.getStyleClass().add("btn-accent");
        payBtn.setMinWidth(260);
        payBtn.setOnAction(e -> executePaymentSimulation());

        retryBtn = new Button("🔄 Retry Payment");
        retryBtn.getStyleClass().add("btn-primary");
        retryBtn.setVisible(false);
        retryBtn.setManaged(false);
        retryBtn.setOnAction(e -> executePaymentSimulation());

        actionsRow.getChildren().addAll(payBtn, retryBtn);

        card.getChildren().addAll(title, grid, paymentDetailsBox, inlineStatusLabel, actionsRow);
        return card;
    }

    /**
     * Helper to create a toggle button for a payment mode.
     */
    private Button createPaymentMethodButton(String text, PaymentMethod method) {
        Button btn = new Button(text);
        btn.getStyleClass().add("payment-method-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btn, Priority.ALWAYS);
        btn.setOnAction(e -> {
            selectedMethod = method;
            updatePaymentMethodUI();
            renderPaymentFields();
            inlineStatusLabel.setText("");
        });
        return btn;
    }

    /**
     * Updates active toggle styles across payment buttons.
     */
    private void updatePaymentMethodUI() {
        creditCardBtn.getStyleClass().remove("payment-method-btn-selected");
        debitCardBtn.getStyleClass().remove("payment-method-btn-selected");
        netBankingBtn.getStyleClass().remove("payment-method-btn-selected");
        walletBtn.getStyleClass().remove("payment-method-btn-selected");

        switch (selectedMethod) {
            case CREDIT_CARD -> creditCardBtn.getStyleClass().add("payment-method-btn-selected");
            case DEBIT_CARD  -> debitCardBtn.getStyleClass().add("payment-method-btn-selected");
            case NET_BANKING -> netBankingBtn.getStyleClass().add("payment-method-btn-selected");
            case WALLET      -> walletBtn.getStyleClass().add("payment-method-btn-selected");
        }
    }

    /**
     * Dynamically renders input fields based on the chosen payment method.
     */
    private void renderPaymentFields() {
        paymentDetailsBox.getChildren().clear();

        if (selectedMethod == PaymentMethod.CREDIT_CARD || selectedMethod == PaymentMethod.DEBIT_CARD) {
            Label fieldTitle = new Label(selectedMethod == PaymentMethod.CREDIT_CARD ? "Enter Credit Card Details" : "Enter Debit Card Details");
            fieldTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

            VBox numBox = new VBox(4);
            numBox.getChildren().addAll(new Label("Card Number"), cardNumberField = new TextField("4532 8901 2345 6789"));
            cardNumberField.getStyleClass().add("text-input-custom");

            HBox row = new HBox(12);
            VBox nameBox = new VBox(4);
            nameBox.getChildren().addAll(new Label("Name on Card"), cardHolderField = new TextField("Rahul Sharma"));
            cardHolderField.getStyleClass().add("text-input-custom");
            HBox.setHgrow(nameBox, Priority.ALWAYS);

            VBox expBox = new VBox(4);
            expBox.getChildren().addAll(new Label("Expiry (MM/YY)"), cardExpiryField = new TextField("12/28"));
            cardExpiryField.getStyleClass().add("text-input-custom");
            expBox.setPrefWidth(110);

            VBox cvvBox = new VBox(4);
            cvvBox.getChildren().addAll(new Label("CVV"), cardCvvField = new PasswordField());
            cardCvvField.setText("123");
            cardCvvField.getStyleClass().add("text-input-custom");
            cvvBox.setPrefWidth(80);

            row.getChildren().addAll(nameBox, expBox, cvvBox);
            paymentDetailsBox.getChildren().addAll(fieldTitle, numBox, row);

        } else if (selectedMethod == PaymentMethod.NET_BANKING) {
            Label fieldTitle = new Label("Select Your Bank");
            fieldTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

            bankCombo = new ComboBox<>();
            bankCombo.getItems().addAll("HDFC Bank", "State Bank of India (SBI)", "ICICI Bank", "Axis Bank", "Kotak Mahindra Bank");
            bankCombo.setValue("HDFC Bank");
            bankCombo.getStyleClass().add("combo-custom");
            bankCombo.setMaxWidth(Double.MAX_VALUE);

            paymentDetailsBox.getChildren().addAll(fieldTitle, bankCombo);

        } else if (selectedMethod == PaymentMethod.WALLET) {
            Label fieldTitle = new Label("UPI Virtual Payment Address (VPA) / Wallet ID");
            fieldTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

            upiIdField = new TextField("rahul@okaxis");
            upiIdField.setPromptText("e.g. mobile@upi or username@bank");
            upiIdField.getStyleClass().add("text-input-custom");

            Label hint = new Label("Supports Google Pay, PhonePe, Paytm, and BHIM UPI");
            hint.getStyleClass().add("text-muted");

            paymentDetailsBox.getChildren().addAll(fieldTitle, upiIdField, hint);
        }
    }

    /**
     * Sticky Fare Summary Card on the right-hand side.
     */
    private VBox createStickyFareCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("fare-summary-card");

        Label title = new Label("Fare Breakdown");
        title.getStyleClass().add("card-title");

        // 1. Base Fare Row
        double baseTotal = flight.getPrice() * passengers.size();
        HBox baseRow = createFareLine(
            "Base Fare (" + passengers.size() + " Traveller" + (passengers.size() > 1 ? "s" : "") + ")",
            "₹" + (int) baseTotal
        );

        // 2. Extra Legroom Surcharge
        int legroomCount = 0;
        for (String sNo : seatNos) {
            Seat s = flight.getSeatByNumber(sNo);
            if (s != null && s.getType() == Seat.SeatType.EXTRA_LEGROOM) legroomCount++;
        }
        HBox legroomRow = createFareLine(
            "Extra Legroom (" + legroomCount + " seat" + (legroomCount > 1 ? "s" : "") + ")",
            "₹" + (legroomCount * 500)
        );

        // 3. Baggage Allowance
        double bagCost = 0;
        if (baggageOption.contains("15 kg")) bagCost = 500;
        else if (baggageOption.contains("20 kg")) bagCost = 800;
        else if (baggageOption.contains("25 kg")) bagCost = 1200;
        HBox baggageRow = createFareLine("Baggage (" + baggageOption + ")", "₹" + (int) bagCost);

        // 4. Taxes & Airport Fees
        HBox taxRow = createFareLine("Taxes & Convenience Fee", "Included");

        Separator sep = new Separator();

        // 5. Total Fare
        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        Label totalLabel = new Label("Total Payable");
        totalLabel.getStyleClass().add("fare-total-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label totalVal = new Label("₹" + (int) totalFare);
        totalVal.getStyleClass().add("fare-total-value");
        totalRow.getChildren().addAll(totalLabel, spacer, totalVal);

        // Security Assurance
        HBox secureRow = new HBox(6);
        secureRow.setAlignment(Pos.CENTER);
        secureRow.setStyle("-fx-padding: 8 0 0 0;");
        Label lockIcon = new Label("🔒");
        Label secureText = new Label("256-bit Secure Bank Encryption");
        secureText.getStyleClass().add("text-muted");
        secureRow.getChildren().addAll(lockIcon, secureText);

        card.getChildren().addAll(title, baseRow, legroomRow, baggageRow, taxRow, sep, totalRow, secureRow);
        return card;
    }

    /**
     * Helper to create a single row in the fare summary card.
     */
    private HBox createFareLine(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("fare-row-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label val = new Label(value);
        val.getStyleClass().add("fare-row-value");
        row.getChildren().addAll(lbl, spacer, val);
        return row;
    }

    /**
     * Executes the simulated payment transaction via PaymentService:
     *  - Validates card/UPI inputs.
     *  - Simulates processing delay.
     *  - Catches PaymentFailedException (~15% rate) and offers Retry.
     *  - On success: Calls BookingService to create booking and generate ticket file,
     *    then transitions to ConfirmationScreen.
     */
    private void executePaymentSimulation() {
        inlineStatusLabel.setText("");

        // ── Input Validation ──
        if (selectedMethod == PaymentMethod.CREDIT_CARD || selectedMethod == PaymentMethod.DEBIT_CARD) {
            String cardNum = cardNumberField != null && cardNumberField.getText() != null 
                    ? cardNumberField.getText().replaceAll("\\s+", "") : "";
            if (!cardNum.matches("\\d{16}")) {
                showPaymentError("Please enter a valid 16-digit card number (no letters).");
                return;
            }
            String exp = cardExpiryField != null && cardExpiryField.getText() != null ? cardExpiryField.getText().trim() : "";
            if (!exp.matches("(0[1-9]|1[0-2])/\\d{2}")) {
                showPaymentError("Please enter a valid expiry date in MM/YY format (e.g. 08/28).");
                return;
            }
            String cvv = cardCvvField != null && cardCvvField.getText() != null ? cardCvvField.getText().trim() : "";
            if (!cvv.matches("\\d{3}")) {
                showPaymentError("CVV must be exactly 3 numeric digits.");
                return;
            }
        } else if (selectedMethod == PaymentMethod.WALLET) {
            String upi = upiIdField != null && upiIdField.getText() != null ? upiIdField.getText().trim() : "";
            if (!upi.contains("@") || upi.length() < 4) {
                showPaymentError("Please enter a valid UPI Virtual Payment Address (e.g. mobile@upi).");
                return;
            }
        }

        inlineStatusLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2B2D7C;");
        inlineStatusLabel.setText("Connecting to Payment Gateway... Please wait.");
        payBtn.setDisable(true);
        retryBtn.setVisible(false);
        retryBtn.setManaged(false);

        // Run transaction simulation in background thread so UI doesn't freeze during the 500ms sleep
        new Thread(() -> {
            try {
                // Call simulated payment service (15% random failure rate)
                Payment payment = AeroReserveApp.paymentService.processPayment(selectedMethod, totalFare);

                // Update UI on JavaFX Application Thread
                Platform.runLater(() -> {
                    try {
                        // 1. Create confirmed booking via BookingService
                        String customerEmail = (AeroReserveApp.currentUser != null)
                                ? AeroReserveApp.currentUser.getEmail()
                                : "demo@example.com";

                        Booking booking = AeroReserveApp.bookingService.createBooking(
                            customerEmail, flight, passengers, seatNos, baggageOption
                        );

                        // 2. Attach successful payment and save ticket text file
                        AeroReserveApp.bookingService.attachPayment(booking.getBookingRef(), payment);

                        // 3. Transition to Stage 6 Boarding Pass Confirmation Screen
                        AeroReserveApp.showScreen(new ConfirmationScreen(booking).getView());

                    } catch (SeatNotAvailableException | InvalidInputException ex) {
                        inlineStatusLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #DC2626;");
                        inlineStatusLabel.setText("Booking Error: " + ex.getMessage());
                        payBtn.setDisable(false);
                    }
                });

            } catch (PaymentFailedException ex) {
                // 15% Simulated Failure caught here!
                Platform.runLater(() -> {
                    inlineStatusLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #DC2626;");
                    inlineStatusLabel.setText("❌ " + ex.getMessage() + " (Simulated 15% Gateway Failure)");
                    payBtn.setDisable(false);
                    retryBtn.setVisible(true);
                    retryBtn.setManaged(true);
                });
            }
        }).start();
    }

    private void showPaymentError(String msg) {
        inlineStatusLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #DC2626;");
        inlineStatusLabel.setText("⚠ " + msg);
        payBtn.setDisable(false);
    }

    /**
     * Returns the root node.
     */
    public Parent getView() {
        return root;
    }
}

