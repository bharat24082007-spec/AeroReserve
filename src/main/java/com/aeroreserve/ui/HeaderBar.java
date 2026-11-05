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

        // Logo
        Label logoLabel = new Label("✈  AeroReserve");
        logoLabel.getStyleClass().add("header-logo-text");

        // Customer and Admin navigation buttons
        HBox navBox = new HBox(10);
        navBox.setAlignment(Pos.CENTER_LEFT);
        if (user.getRole().equals("CUSTOMER")) {
            Button searchNavBtn = new Button("Search Flights");
            searchNavBtn.getStyleClass().add("header-nav-btn");
            searchNavBtn.setOnAction(e -> AeroReserveApp.showScreen(new SearchScreen().getView()));

            Button myBookingsBtn = new Button("My Bookings");
            myBookingsBtn.getStyleClass().add("header-nav-btn");
            myBookingsBtn.setOnAction(e -> AeroReserveApp.showScreen(new MyBookingsScreen().getView()));

            navBox.getChildren().addAll(searchNavBtn, myBookingsBtn);
        } else if (user.getRole().equals("ADMIN")) {
            Button adminDashBtn = new Button("Admin Dashboard");
            adminDashBtn.getStyleClass().addAll("header-nav-btn", "header-nav-btn-active");
            adminDashBtn.setOnAction(e -> AeroReserveApp.showScreen(new AdminScreen().getView()));

            Button searchNavBtn = new Button("Customer View (Search)");
            searchNavBtn.getStyleClass().add("header-nav-btn");
            searchNavBtn.setOnAction(e -> AeroReserveApp.showScreen(new SearchScreen().getView()));

            navBox.getChildren().addAll(adminDashBtn, searchNavBtn);
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
     * Returns the HBox node representing the header.
     * 
     * @return HBox header
     */
    public HBox getView() {
        return bar;
    }
}

