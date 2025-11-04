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
        
        dbTypeBox.setItems(FXCollections.observableArrayList(
            "SQLITE", 
            "MYSQL", 
            "POSTGRESQL", 
            "FIREBIRD", 
            "CASSANDRA", 
            "COUCHDB"
        ));
        themeManager.styleComboBox(dbTypeBox);

        GridPane remoteFields = new GridPane();
        remoteFields.setHgap(10);
        remoteFields.setVgap(10);

        TextField hostField = themeManager.createStyledTextField("Host / IP Address");
        TextField portField = themeManager.createStyledTextField("Port (e.g., 3306, 5432)");
        TextField dbNameField = themeManager.createStyledTextField("Database Name / Path");
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
        remoteFields.add(new Label("DB Name/Path:") {
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
        
        if (!currentDbConfig.dbType().equals("SQLITE")) {
            try {
                MasterPassword.getKey(); // This will throw if not set
                String host = Encryption.decryptWithIV(currentDbConfig.host(), MasterPassword.getKey());
                String port = Encryption.decryptWithIV(currentDbConfig.port(), MasterPassword.getKey());
                String dbName = Encryption.decryptWithIV(currentDbConfig.dbName(), MasterPassword.getKey());
                String user = Encryption.decryptWithIV(currentDbConfig.user(), MasterPassword.getKey());
                String pass = Encryption.decryptWithIV(currentDbConfig.pass(), MasterPassword.getKey());
                displayConfig = new ConfigManager.DbConfig(currentDbConfig.dbType(), host, port, dbName, user, pass);
            } catch (Exception e) {
                System.err.println("Could not decrypt config to display in settings: " + e.getMessage());
                // Show blank fields if decryption fails
                displayConfig = new ConfigManager.DbConfig(currentDbConfig.dbType(), "", "", "", "", "");
            }
        }

        dbTypeBox.setValue(displayConfig.dbType());
        hostField.setText(displayConfig.host());
        portField.setText(displayConfig.port());
        dbNameField.setText(displayConfig.dbName());
        userField.setText(displayConfig.user());
        passField.setText(displayConfig.pass());

        remoteFields.setVisible(!currentDbConfig.dbType().equals("SQLITE"));
        
        CheckBox migrateDataCheck = themeManager.createStyledCheckBox("Copy data from current DB to new DB");
        migrateDataCheck.setVisible(false); 

        dbTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            remoteFields.setVisible(!newVal.equals("SQLITE"));
            migrateDataCheck.setVisible(!newVal.equals(currentDbConfig.dbType()));
            
            // Set default ports for convenience
            switch(newVal) {
                case "MYSQL":
                    portField.setText("3306");
                    break;
                case "POSTGRESQL":
                    portField.setText("5432");
                    break;
                case "FIREBIRD":
                    portField.setText("3050");
                    break;
                case "CASSANDRA":
                    portField.setText("9042");
                    break;
                case "COUCHDB":
                    portField.setText("5984");
                    break;
                default:
                    portField.clear();
            }
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
            
            mainGui.saveDbSettings(newConfig, migrateDataCheck.isSelected(), restartLabel);
        });
        
        pane.getChildren().addAll(
                title,
                dbTitle,
                new HBox(10, dbTypeLabel, dbTypeBox),
                remoteFields,
                migrateDataCheck, 
                saveButton,
                restartLabel
        );

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
        
        Button generateButton = themeManager.createGeneratorButton();
        generateButton.setOnAction(e -> showGeneratorDialog(passwordField));

        HBox passBox = new HBox(10, passStack, showHidePassButton, generateButton);
        HBox.setHgrow(passStack, Priority.ALWAYS);

        TextField urlField = themeManager.createStyledTextField("URL");
        TextArea notesField = new TextArea();
        notesField.setPromptText("Notes");
        themeManager.styleControl(notesField);
        notesField.setPrefHeight(80);
        notesField.setWrapText(true);


        Button addButton = themeManager.createStyledButton("Add Login");

        addButton.setOnAction(e -> mainGui.addLogin(nameField, usernameField, passwordField, urlField, notesField));

        form.getChildren().addAll(title, nameField, usernameField, passBox, urlField, notesField, addButton);
        return form;
    }
    
    private void showGeneratorDialog(PasswordField targetPasswordField) {
        PasswordGeneratorDialog dialog = new PasswordGeneratorDialog(themeManager, targetPasswordField);
        dialog.showDialog();
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