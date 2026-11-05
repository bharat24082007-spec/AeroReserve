package com.aeroreserve.ui;

import com.aeroreserve.model.User;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * HeaderBar represents the persistent top navigation bar displayed once logged in.
 * 
 * Demonstrates:
 *  - Visual hierarchy using the deep indigo #2B2D7C theme.
 *  - Dynamic navigation buttons based on polymorphic user role (Customer vs Admin).
 *  - Clean logout mechanism that clears session state.
 */
public class HeaderBar {

    private HBox bar;

    private Button searchNavBtn;
    private Button myBookingsBtn;
    private Button adminDashBtn;
    private Button customerViewBtn;

    /**
     * Constructs the header bar for the given logged-in user.
     * 
     * @param user the currently authenticated User
     */
    public HeaderBar(User user) {
        createUI(user);
    }

    /**
     * Assembles the header layout.
     * 
     * @param user current User
     */
    private void createUI(User user) {
        bar = new HBox(16);
        bar.getStyleClass().add("app-header");
        bar.setAlignment(Pos.CENTER_LEFT);

        // Logo (Clickable to return Home)
        Label logoLabel = new Label("✈  AeroReserve");
        logoLabel.getStyleClass().add("header-logo-text");
        logoLabel.setStyle("-fx-cursor: hand;");
        logoLabel.setOnMouseClicked(e -> {
            if ("ADMIN".equals(user.getRole())) {
                setActiveTab("ADMIN");
                AeroReserveApp.showScreen(new AdminScreen().getView());
            } else {
                setActiveTab("SEARCH");
                AeroReserveApp.showScreen(new SearchScreen().getView());
            }
        });

        // Customer and Admin navigation buttons
        HBox navBox = new HBox(10);
        navBox.setAlignment(Pos.CENTER_LEFT);
        if (user.getRole().equals("CUSTOMER")) {
            searchNavBtn = new Button("Search Flights");
            searchNavBtn.getStyleClass().addAll("header-nav-btn", "header-nav-btn-active");
            searchNavBtn.setOnAction(e -> {
                setActiveTab("SEARCH");
                AeroReserveApp.showScreen(new SearchScreen().getView());
            });

            myBookingsBtn = new Button("My Bookings");
            myBookingsBtn.getStyleClass().add("header-nav-btn");
            myBookingsBtn.setOnAction(e -> {
                setActiveTab("BOOKINGS");
                AeroReserveApp.showScreen(new MyBookingsScreen().getView());
            });

            navBox.getChildren().addAll(searchNavBtn, myBookingsBtn);
        } else if (user.getRole().equals("ADMIN")) {
            adminDashBtn = new Button("Admin Dashboard");
            adminDashBtn.getStyleClass().addAll("header-nav-btn", "header-nav-btn-active");
            adminDashBtn.setOnAction(e -> {
                setActiveTab("ADMIN");
                AeroReserveApp.showScreen(new AdminScreen().getView());
            });

            customerViewBtn = new Button("Customer View (Search)");
            customerViewBtn.getStyleClass().add("header-nav-btn");
            customerViewBtn.setOnAction(e -> {
                setActiveTab("CUSTOMER_VIEW");
                AeroReserveApp.showScreen(new SearchScreen().getView());
            });

            navBox.getChildren().addAll(adminDashBtn, customerViewBtn);
        }

        // Spacer between navigation and user controls
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // User profile chip
        HBox userChip = new HBox(8);
        userChip.setAlignment(Pos.CENTER);
        userChip.setStyle("-fx-background-color: rgba(255, 255, 255, 0.12); -fx-padding: 4 12; -fx-background-radius: 20;");

        Label roleBadge = new Label(user.getRole());
        roleBadge.setStyle("-fx-background-color: " + (user.getRole().equals("ADMIN") ? "#DC2626" : "#FF6D00") + "; -fx-text-fill: white; -fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 10;");

        Label userNameLabel = new Label(user.getName());
        userNameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold;");

        userChip.getChildren().addAll(roleBadge, userNameLabel);

        // Logout button
        Button logoutBtn = new Button("Log Out");
        logoutBtn.getStyleClass().add("btn-outline-white");
        logoutBtn.setOnAction(e -> AeroReserveApp.showLoginScreen());

        bar.getChildren().addAll(logoLabel, navBox, spacer, userChip, logoutBtn);
    }

    /**
     * Updates navigation button active styles.
     */
    public void setActiveTab(String tab) {
        if (searchNavBtn != null) searchNavBtn.getStyleClass().remove("header-nav-btn-active");
        if (myBookingsBtn != null) myBookingsBtn.getStyleClass().remove("header-nav-btn-active");
        if (adminDashBtn != null) adminDashBtn.getStyleClass().remove("header-nav-btn-active");
        if (customerViewBtn != null) customerViewBtn.getStyleClass().remove("header-nav-btn-active");

        if ("SEARCH".equals(tab) && searchNavBtn != null) {
            searchNavBtn.getStyleClass().add("header-nav-btn-active");
        } else if ("BOOKINGS".equals(tab) && myBookingsBtn != null) {
            myBookingsBtn.getStyleClass().add("header-nav-btn-active");
        } else if ("ADMIN".equals(tab) && adminDashBtn != null) {
            adminDashBtn.getStyleClass().add("header-nav-btn-active");
        } else if ("CUSTOMER_VIEW".equals(tab) && customerViewBtn != null) {
            customerViewBtn.getStyleClass().add("header-nav-btn-active");
        }
    }

    /**
     * Returns the HBox node representing the header.
     * 
     * @return HBox header
     */
    public HBox getView() {
        return bar;
    }
}

