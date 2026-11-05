package com.aeroreserve.ui;

import com.aeroreserve.exception.InvalidInputException;
import com.aeroreserve.exception.UserAlreadyExistsException;
import com.aeroreserve.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * LoginScreen provides the initial authentication interface.
 * 
 * It features a toggle tab between:
 *  1. Login (existing customers and admins)
 *  2. Create Account (new customer registration)
 * 
 * Strict error handling:
 *  - Inline error labels only (no alert popups, as specified in requirements).
 *  - Catches InvalidInputException and UserAlreadyExistsException.
 */
public class LoginScreen {

    // Main container
    private StackPane root;

    // Active tab state: true = login tab, false = register tab
    private boolean isLoginTab = true;

    // Login tab form controls
    private TextField loginEmailField;
    private PasswordField loginPasswordField;
    private Label loginErrorLabel;

    // Register tab form controls
    private TextField regNameField;
    private TextField regEmailField;
    private PasswordField regPasswordField;
    private Label regErrorLabel;
    private Label regSuccessLabel;

    // Form containers
    private VBox loginFormBox;
    private VBox registerFormBox;
    private Button loginTabBtn;
    private Button registerTabBtn;

    /**
     * Constructor sets up the UI components and layout.
     */
    public LoginScreen() {
        createUI();
    }

    /**
     * Builds the complete Login / Register UI tree.
     */
    private void createUI() {
        root = new StackPane();
        root.getStyleClass().add("login-bg");
        root.setAlignment(Pos.CENTER);

        // Center white card containing the branding and form
        VBox card = new VBox(20);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(460);
        card.setAlignment(Pos.CENTER);

        // Header branding
        Label logoLabel = new Label("✈  AeroReserve");
        logoLabel.getStyleClass().add("login-title");

        Label subtitleLabel = new Label("Advanced Flight Reservation Management System");
        subtitleLabel.getStyleClass().add("login-tagline");

        // Tab switcher (Login vs Create Account)
        HBox tabBar = createTabBar();

        // Forms
        loginFormBox = createLoginForm();
        registerFormBox = createRegisterForm();
        registerFormBox.setVisible(false);
        registerFormBox.setManaged(false);

        // Quick login helper buttons for examiner / viva demonstration
        VBox demoBox = createDemoAccountsBox();

        card.getChildren().addAll(logoLabel, subtitleLabel, tabBar, loginFormBox, registerFormBox, demoBox);
        root.getChildren().add(card);
    }

    /**
     * Builds the top toggle tab buttons ("Log In" and "Create Account").
     * 
     * @return HBox containing styled tab buttons
     */
    private HBox createTabBar() {
        HBox tabBar = new HBox(8);
        tabBar.getStyleClass().add("tab-bar");
        tabBar.setAlignment(Pos.CENTER);

        loginTabBtn = new Button("Log In");
        loginTabBtn.getStyleClass().addAll("tab-btn", "tab-btn-active");
        loginTabBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(loginTabBtn, Priority.ALWAYS);

        registerTabBtn = new Button("Create Account");
        registerTabBtn.getStyleClass().add("tab-btn");
        registerTabBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(registerTabBtn, Priority.ALWAYS);

        // Tab click actions
        loginTabBtn.setOnAction(e -> switchTab(true));
        registerTabBtn.setOnAction(e -> switchTab(false));

        tabBar.getChildren().addAll(loginTabBtn, registerTabBtn);
        return tabBar;
    }

    /**
     * Switches between Login and Register views.
     * 
     * @param toLogin true to show login form, false to show register form
     */
    private void switchTab(boolean toLogin) {
        this.isLoginTab = toLogin;

        // Clear previous error and success messages
        loginErrorLabel.setText("");
        regErrorLabel.setText("");
        regSuccessLabel.setText("");

        if (toLogin) {
            loginTabBtn.getStyleClass().add("tab-btn-active");
            registerTabBtn.getStyleClass().remove("tab-btn-active");

            loginFormBox.setVisible(true);
            loginFormBox.setManaged(true);
            registerFormBox.setVisible(false);
            registerFormBox.setManaged(false);
        } else {
            registerTabBtn.getStyleClass().add("tab-btn-active");
            loginTabBtn.getStyleClass().remove("tab-btn-active");

            registerFormBox.setVisible(true);
            registerFormBox.setManaged(true);
            loginFormBox.setVisible(false);
            loginFormBox.setManaged(false);
        }
    }

    /**
     * Creates the form fields and submit button for Login.
     * 
     * @return VBox holding the login form
     */
    private VBox createLoginForm() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER_LEFT);

        Label emailLabel = new Label("Email Address");
        emailLabel.getStyleClass().add("field-label");
        loginEmailField = new TextField();
        loginEmailField.setPromptText("e.g. demo@example.com");
        loginEmailField.getStyleClass().add("text-input-custom");

        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("field-label");
        loginPasswordField = new PasswordField();
        loginPasswordField.setPromptText("Enter your password");
        loginPasswordField.getStyleClass().add("text-input-custom");

        // Submit on pressing ENTER in password field
        loginPasswordField.setOnAction(e -> handleLogin());

        // Inline error message label
        loginErrorLabel = new Label();
        loginErrorLabel.getStyleClass().add("label-error");
        loginErrorLabel.setWrapText(true);

        Button submitBtn = new Button("Sign In");
        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setOnAction(e -> handleLogin());

        box.getChildren().addAll(emailLabel, loginEmailField, passLabel, loginPasswordField, loginErrorLabel, submitBtn);
        return box;
    }

    /**
     * Creates the form fields and submit button for Registration.
     * 
     * @return VBox holding the registration form
     */
    private VBox createRegisterForm() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label("Full Name");
        nameLabel.getStyleClass().add("field-label");
        regNameField = new TextField();
        regNameField.setPromptText("e.g. Rahul Sharma");
        regNameField.getStyleClass().add("text-input-custom");

        Label emailLabel = new Label("Email Address");
        emailLabel.getStyleClass().add("field-label");
        regEmailField = new TextField();
        regEmailField.setPromptText("e.g. rahul@example.com");
        regEmailField.getStyleClass().add("text-input-custom");

        Label passLabel = new Label("Create Password");
        passLabel.getStyleClass().add("field-label");
        regPasswordField = new PasswordField();
        regPasswordField.setPromptText("Minimum 6 characters");
        regPasswordField.getStyleClass().add("text-input-custom");

        regPasswordField.setOnAction(e -> handleRegister());

        // Inline feedback labels
        regErrorLabel = new Label();
        regErrorLabel.getStyleClass().add("label-error");
        regErrorLabel.setWrapText(true);

        regSuccessLabel = new Label();
        regSuccessLabel.getStyleClass().add("label-success");
        regSuccessLabel.setWrapText(true);

        Button submitBtn = new Button("Create Customer Account");
        submitBtn.getStyleClass().add("btn-accent");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setOnAction(e -> handleRegister());

        box.getChildren().addAll(
                nameLabel, regNameField,
                emailLabel, regEmailField,
                passLabel, regPasswordField,
                regErrorLabel, regSuccessLabel,
                submitBtn
        );
        return box;
    }

    /**
     * Provides quick-fill buttons for demo credentials.
     * Essential for fast, error-free viva demos.
     */
    private VBox createDemoAccountsBox() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12, 0, 0, 0));
        box.setAlignment(Pos.CENTER);

        Label hintLabel = new Label("Quick Demo Logins (Click to autofill):");
        hintLabel.getStyleClass().add("text-muted");

        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER);

        Button fillCustomerBtn = new Button("Demo Customer");
        fillCustomerBtn.getStyleClass().add("btn-outline");
        fillCustomerBtn.setOnAction(e -> {
            switchTab(true);
            loginEmailField.setText("demo@example.com");
            loginPasswordField.setText("demo123");
            loginErrorLabel.setText("");
        });

        Button fillAdminBtn = new Button("Demo Admin");
        fillAdminBtn.getStyleClass().add("btn-outline");
        fillAdminBtn.setOnAction(e -> {
            switchTab(true);
            loginEmailField.setText("admin@aeroreserve.com");
            loginPasswordField.setText("admin123");
            loginErrorLabel.setText("");
        });

        buttonsBox.getChildren().addAll(fillCustomerBtn, fillAdminBtn);
        box.getChildren().addAll(hintLabel, buttonsBox);
        return box;
    }

    /**
     * Handles the login action:
     * Validates input, queries AuthService, and on success moves to the dashboard.
     */
    private void handleLogin() {
        loginErrorLabel.setText("");

        String email = loginEmailField.getText();
        String pass = loginPasswordField.getText();

        try {
            // AuthService checks credentials and returns User (Customer or Admin)
            User user = AeroReserveApp.authService.login(email, pass);
            // Transition to dashboard
            AeroReserveApp.onLoginSuccess(user);
        } catch (InvalidInputException ex) {
            // Display inline error message without popups
            loginErrorLabel.setText(ex.getMessage());
        } catch (Exception ex) {
            loginErrorLabel.setText("Login failed: " + ex.getMessage());
        }
    }

    /**
     * Handles the registration action:
     * Validates input, calls AuthService.register, and gives inline feedback.
     */
    private void handleRegister() {
        regErrorLabel.setText("");
        regSuccessLabel.setText("");

        String name = regNameField.getText();
        String email = regEmailField.getText();
        String pass = regPasswordField.getText();

        try {
            // Register Customer account (saves to data/users.csv automatically)
            User newUser = AeroReserveApp.authService.register(name, email, pass);
            regSuccessLabel.setText("Account created! Logging you in...");

            // Switch to dashboard directly
            AeroReserveApp.onLoginSuccess(newUser);
        } catch (InvalidInputException | UserAlreadyExistsException ex) {
            // Catch custom exception and display clean inline error
            regErrorLabel.setText(ex.getMessage());
        } catch (Exception ex) {
            regErrorLabel.setText("Registration error: " + ex.getMessage());
        }
    }

    /**
     * Returns the root JavaFX node for display.
     * 
     * @return Parent root
     */
    public Parent getView() {
        return root;
    }
}

