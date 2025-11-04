package gui;

import core.ConfigManager;
import core.Encryption;
import core.MasterPassword;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GuiBuilder {

    private final MainGui mainGui;
    private final ThemeManager themeManager;

    public GuiBuilder(MainGui mainGui, ThemeManager themeManager) {
        this.mainGui = mainGui;
        this.themeManager = themeManager;
    }

    public VBox buildSettingsPane() {
        VBox pane = new VBox(15);
        pane.setPadding(new Insets(10));

        Label title = new Label("Settings");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");

        Label dbTitle = new Label("Database Configuration");
        dbTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        dbTitle.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");

        Label dbTypeLabel = new Label("Database Type:");
        dbTypeLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        ComboBox<String> dbTypeBox = new ComboBox<>();
        dbTypeBox.setItems(FXCollections.observableArrayList("SQLITE", "MYSQL"));
        themeManager.styleComboBox(dbTypeBox);

        GridPane remoteFields = new GridPane();
        remoteFields.setHgap(10);
        remoteFields.setVgap(10);

        TextField hostField = themeManager.createStyledTextField("Host (e.g., 127.0.0.1)");
        TextField portField = themeManager.createStyledTextField("Port (e.g., 3306)");
        TextField dbNameField = themeManager.createStyledTextField("Database Name");
        TextField userField = themeManager.createStyledTextField("Username");
        PasswordField passField = themeManager.createStyledPasswordField("Password");

        remoteFields.add(new Label("Host:") {
            {
                setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
            }
        }, 0, 0);
        remoteFields.add(hostField, 1, 0);
        remoteFields.add(new Label("Port:") {
            {
                setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
            }
        }, 0, 1);
        remoteFields.add(portField, 1, 1);
        remoteFields.add(new Label("DB Name:") {
            {
                setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
            }
        }, 0, 2);
        remoteFields.add(dbNameField, 1, 2);
        remoteFields.add(new Label("User:") {
            {
                setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
            }
        }, 0, 3);
        remoteFields.add(userField, 1, 3);
        remoteFields.add(new Label("Password:") {
            {
                setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
            }
        }, 0, 4);
        remoteFields.add(passField, 1, 4);

        ConfigManager.DbConfig currentDbConfig = mainGui.getDbConfig();
        ConfigManager.DbConfig displayConfig = currentDbConfig;
        if (currentDbConfig.dbType().equals("MYSQL")) {
            try {
                String host = Encryption.decryptWithIV(currentDbConfig.host(), MasterPassword.getKey());
                String port = Encryption.decryptWithIV(currentDbConfig.port(), MasterPassword.getKey());
                String dbName = Encryption.decryptWithIV(currentDbConfig.dbName(), MasterPassword.getKey());
                String user = Encryption.decryptWithIV(currentDbConfig.user(), MasterPassword.getKey());
                String pass = Encryption.decryptWithIV(currentDbConfig.pass(), MasterPassword.getKey());
                displayConfig = new ConfigManager.DbConfig("MYSQL", host, port, dbName, user, pass);
            } catch (Exception e) {
                System.err.println("Could not decrypt config to display in settings: " + e.getMessage());
                displayConfig = new ConfigManager.DbConfig("MYSQL", "", "", "", "", "");
            }
        }

        dbTypeBox.setValue(displayConfig.dbType());
        hostField.setText(displayConfig.host());
        portField.setText(displayConfig.port());
        dbNameField.setText(displayConfig.dbName());
        userField.setText(displayConfig.user());
        passField.setText(displayConfig.pass());

        remoteFields.setVisible(currentDbConfig.dbType().equals("MYSQL"));
        
        // --- NEW CHECKBOX FOR MIGRATION ---
        CheckBox migrateDataCheck = new CheckBox("Copy data from current DB to new DB");
        migrateDataCheck.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        // Hide by default. Only show if the DB type is changed.
        migrateDataCheck.setVisible(false); 

        dbTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            remoteFields.setVisible(newVal.equals("MYSQL"));
            // Show the migrate checkbox ONLY if the new value is different from the original config's value
            migrateDataCheck.setVisible(!newVal.equals(currentDbConfig.dbType()));
        });

        Label restartLabel = new Label("Restart required to apply database changes.");
        restartLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentErrorColor() + "; -fx-font-weight: bold;");
        restartLabel.setVisible(false);

        Button saveButton = themeManager.createStyledButton("Save Database Settings");
        saveButton.setOnAction(e -> {
            ConfigManager.DbConfig newConfig = new ConfigManager.DbConfig(
                    dbTypeBox.getValue(),
                    hostField.getText(),
                    portField.getText(),
                    dbNameField.getText(),
                    userField.getText(),
                    passField.getText());
            
            // --- UPDATED METHOD CALL ---
            // Pass the state of the new checkbox to the save method
            mainGui.saveDbSettings(newConfig, migrateDataCheck.isSelected(), restartLabel);
        });

        pane.getChildren().addAll(
                title,
                dbTitle,
                new HBox(10, dbTypeLabel, dbTypeBox),
                remoteFields,
                migrateDataCheck, // <-- ADDED CHECKBOX
                saveButton,
                restartLabel);

        hostField.setMaxWidth(Double.MAX_VALUE);
        portField.setMaxWidth(Double.MAX_VALUE);
        dbNameField.setMaxWidth(Double.MAX_VALUE);
        userField.setMaxWidth(Double.MAX_VALUE);
        passField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(hostField, Priority.ALWAYS);
        GridPane.setHgrow(portField, Priority.ALWAYS);
        GridPane.setHgrow(dbNameField, Priority.ALWAYS);
        GridPane.setHgrow(userField, Priority.ALWAYS);
        GridPane.setHgrow(passField, Priority.ALWAYS);

        return pane;
    }

    public VBox buildAddForm() {
        VBox form = new VBox(15);
        form.setPadding(new Insets(10));

        Label title = new Label("Add New Login");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");

        TextField nameField = themeManager.createStyledTextField("Name");
        TextField usernameField = themeManager.createStyledTextField("Username");

        PasswordField passwordField = themeManager.createStyledPasswordField("Password");
        TextField visiblePasswordField = themeManager.createStyledTextField("Password");
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        visiblePasswordField.setVisible(false);

        StackPane passStack = new StackPane(passwordField, visiblePasswordField);
        StackPane.setAlignment(visiblePasswordField, Pos.CENTER_LEFT);
        StackPane.setAlignment(passwordField, Pos.CENTER_LEFT);

        ToggleButton showHidePassButton = themeManager.createShowHideButton();
        showHidePassButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            visiblePasswordField.setVisible(newVal);
            passwordField.setVisible(!newVal);
        });

        HBox passBox = new HBox(10, passStack, showHidePassButton);
        HBox.setHgrow(passStack, Priority.ALWAYS);

        TextField urlField = themeManager.createStyledTextField("URL");
        TextField notesField = themeManager.createStyledTextField("Notes");
        notesField.setPrefHeight(80);

        Button addButton = themeManager.createStyledButton("Add Login");

        addButton.setOnAction(e -> mainGui.addLogin(nameField, usernameField, passwordField, urlField, notesField));

        form.getChildren().addAll(title, nameField, usernameField, passBox, urlField, notesField, addButton);
        return form;
    }

    public VBox buildDetailsForm(LoginEntry login) {
        VBox form = new VBox(15);
        form.setPadding(new Insets(10));

        Label title = new Label(login.getName());
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");

        TextField nameDisplay = themeManager.createStyledTextField("Name");
        nameDisplay.setText(login.getName());
        nameDisplay.setEditable(false);

        TextField userDisplay = themeManager.createStyledTextField("Username");
        userDisplay.setText(login.getUsername());
        userDisplay.setEditable(false);

        Button copyUserButton = themeManager.createCopyButton();
        copyUserButton.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(login.getUsername());
            clipboard.setContent(content);
            mainGui.getStatusLabel().setText("Username copied to clipboard.");
            mainGui.getStatusLabel().setStyle("-fx-text-fill: " + themeManager.getCurrentSuccessColor() + ";");
        });

        HBox userBox = new HBox(10, userDisplay, copyUserButton);
        userBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(userDisplay, Priority.ALWAYS);

        TextField passDisplay = themeManager.createStyledTextField("Password");
        passDisplay.setText("************");
        passDisplay.setEditable(false);

        ToggleButton showHideButton = themeManager.createShowHideButton();
        showHideButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                passDisplay.setText(login.getPassword());
            } else {
                passDisplay.setText("************");
            }
        });

        Button copyPassButton = themeManager.createCopyButton();
        copyPassButton.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(login.getPassword());
            clipboard.setContent(content);
            mainGui.getStatusLabel().setText("Password copied to clipboard.");
            mainGui.getStatusLabel().setStyle("-fx-text-fill: " + themeManager.getCurrentSuccessColor() + ";");
        });

        HBox passBox = new HBox(10, passDisplay, showHideButton, copyPassButton);
        passBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(passDisplay, Priority.ALWAYS);

        TextField urlDisplay = themeManager.createStyledTextField("URL");
        urlDisplay.setText(login.getUrl());
        urlDisplay.setEditable(false);

        TextArea notesDisplay = new TextArea(login.getNotes());
        notesDisplay.setPromptText("Notes");
        themeManager.styleControl(notesDisplay);
        notesDisplay.setEditable(false);
        notesDisplay.setWrapText(true);
        notesDisplay.setPrefHeight(80);

        Button deleteButton = themeManager.createStyledButton("Delete");
        deleteButton.setStyle(deleteButton.getStyle() + "-fx-text-fill: " + themeManager.getCurrentErrorColor() + ";");
        deleteButton.setOnAction(e -> mainGui.deleteLogin(login));

        HBox buttonBar = new HBox(10, deleteButton);

        form.getChildren().addAll(title, nameDisplay, userBox, passBox, urlDisplay, notesDisplay, buttonBar);
        return form;
    }
}