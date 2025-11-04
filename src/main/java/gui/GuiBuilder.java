package gui;

import core.ConfigManager;
import core.Encryption;
import core.MasterPassword;
import core.PasswordGenerator; // Added
import javafx.application.Platform; // Added
import javafx.beans.binding.Bindings; // Added
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
import javafx.scene.paint.Color; // Added
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.StageStyle; // Added

import java.util.Optional; // Added

public class GuiBuilder {

    private final MainGui mainGui;
    private final ThemeManager themeManager;

    public GuiBuilder(MainGui mainGui, ThemeManager themeManager) {
        this.mainGui = mainGui;
        this.themeManager = themeManager;
    }

    public VBox buildSettingsPane() {
        // ... (existing code)
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
        
        CheckBox migrateDataCheck = new CheckBox("Copy data from current DB to new DB");
        migrateDataCheck.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        migrateDataCheck.setVisible(false); 

        dbTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            remoteFields.setVisible(newVal.equals("MYSQL"));
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
            
            mainGui.saveDbSettings(newConfig, migrateDataCheck.isSelected(), restartLabel);
        });

        pane.getChildren().addAll(
                title,
                dbTitle,
                new HBox(10, dbTypeLabel, dbTypeBox),
                remoteFields,
                migrateDataCheck, 
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
        
        // --- NEW ---
        Button generateButton = themeManager.createGeneratorButton();
        generateButton.setOnAction(e -> showGeneratorDialog(passwordField));

        HBox passBox = new HBox(10, passStack, showHidePassButton, generateButton); // Added generateButton
        HBox.setHgrow(passStack, Priority.ALWAYS);
        // --- END NEW ---

        TextField urlField = themeManager.createStyledTextField("URL");
        TextField notesField = themeManager.createStyledTextField("Notes");
        notesField.setPrefHeight(80);

        Button addButton = themeManager.createStyledButton("Add Login");

        addButton.setOnAction(e -> mainGui.addLogin(nameField, usernameField, passwordField, urlField, notesField));

        form.getChildren().addAll(title, nameField, usernameField, passBox, urlField, notesField, addButton);
        return form;
    }
    
    // --- NEW METHOD ---
    private void showGeneratorDialog(PasswordField targetPasswordField) {
        Dialog<String> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.getDialogPane().getScene().setFill(Color.TRANSPARENT);
        dialog.getDialogPane().setStyle("-fx-background-color: " + themeManager.getCurrentBaseSemiTransparent() + "; -fx-background-radius: 15;");
        dialog.getDialogPane().setEffect(themeManager.getLightOuterShadow());
        dialog.getDialogPane().setPrefWidth(400);

        dialog.setTitle("Password Generator");

        // --- Result Area ---
        TextField resultField = themeManager.createStyledTextField("Generated Password");
        resultField.setEditable(false);
        Button copyButton = themeManager.createCopyButton();
        copyButton.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(resultField.getText());
            clipboard.setContent(content);
        });
        HBox resultBox = new HBox(10, resultField, copyButton);
        HBox.setHgrow(resultField, Priority.ALWAYS);
        
        // --- Password Tab ---
        VBox passwordTabContent = new VBox(15);
        passwordTabContent.setPadding(new Insets(10));
        
        Label lengthLabel = new Label("Length: 16");
        lengthLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        Slider lengthSlider = new Slider(8, 64, 16);
        lengthSlider.setBlockIncrement(1);
        lengthSlider.setMajorTickUnit(8);
        lengthSlider.setMinorTickCount(7);
        lengthSlider.setShowTickMarks(true);
        lengthLabel.textProperty().bind(Bindings.format("Length: %.0f", lengthSlider.valueProperty()));
        themeManager.styleSlider(lengthSlider);
        
        CheckBox upperCheck = themeManager.createStyledCheckBox("Uppercase (A-Z)");
        upperCheck.setSelected(true);
        CheckBox digitsCheck = themeManager.createStyledCheckBox("Digits (0-9)");
        digitsCheck.setSelected(true);
        CheckBox symbolsCheck = themeManager.createStyledCheckBox("Symbols (!@#...)");
        symbolsCheck.setSelected(true);
        
        Button generatePasswordButton = themeManager.createStyledButton("Generate Password");
        generatePasswordButton.setOnAction(e -> {
            resultField.setText(PasswordGenerator.generatePassword(
                (int) lengthSlider.getValue(),
                upperCheck.isSelected(),
                digitsCheck.isSelected(),
                symbolsCheck.isSelected()
            ));
        });
        
        passwordTabContent.getChildren().addAll(
            new VBox(5, lengthLabel, lengthSlider),
            upperCheck, digitsCheck, symbolsCheck,
            generatePasswordButton
        );
        passwordTabContent.setStyle("-fx-background-color: " + themeManager.getCurrentBaseColor() + ";");

        // --- Passphrase Tab ---
        VBox passphraseTabContent = new VBox(15);
        passphraseTabContent.setPadding(new Insets(10));
        
        Label wordsLabel = new Label("Words: 4");
        wordsLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        Slider wordsSlider = new Slider(3, 10, 4);
        wordsSlider.setBlockIncrement(1);
        wordsSlider.setMajorTickUnit(1);
        wordsSlider.setSnapToTicks(true);
        wordsLabel.textProperty().bind(Bindings.format("Words: %.0f", wordsSlider.valueProperty()));
        themeManager.styleSlider(wordsSlider);

        Label separatorLabel = new Label("Separator:");
        separatorLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        TextField separatorField = themeManager.createStyledTextField("-");
        separatorField.setMaxWidth(100);
        HBox separatorBox = new HBox(10, separatorLabel, separatorField);
        separatorBox.setAlignment(Pos.CENTER_LEFT);
        
        Button generatePassphraseButton = themeManager.createStyledButton("Generate Passphrase");
        generatePassphraseButton.setOnAction(e -> {
            resultField.setText(PasswordGenerator.generatePassphrase(
                (int) wordsSlider.getValue(),
                separatorField.getText()
            ));
        });
        
        passphraseTabContent.getChildren().addAll(
            new VBox(5, wordsLabel, wordsSlider),
            separatorBox,
            generatePassphraseButton
        );
        passphraseTabContent.setStyle("-fx-background-color: " + themeManager.getCurrentBaseColor() + ";");
        
        // --- Tab Pane ---
        TabPane tabPane = new TabPane();
        Tab passwordTab = new Tab("Password", passwordTabContent);
        Tab passphraseTab = new Tab("Passphrase", passphraseTabContent);
        passwordTab.setClosable(false);
        passphraseTab.setClosable(false);
        tabPane.getTabs().addAll(passwordTab, passphraseTab);
        themeManager.styleTabPane(tabPane);
        
        // --- Main Layout ---
        VBox layout = new VBox(15, tabPane, resultBox);
        layout.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(layout);

        ButtonType useButtonType = new ButtonType("Use This", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(useButtonType, ButtonType.CANCEL);
        
        // Style buttons
        Button useButton = (Button) dialog.getDialogPane().lookupButton(useButtonType);
        useButton.setStyle(themeManager.createStyledButton("Use This", themeManager.getCurrentSuccessColor()).getStyle());
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setStyle(themeManager.createStyledButton("Cancel").getStyle());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == useButtonType) {
                return resultField.getText();
            }
            return null;
        });
        
        // Generate a password on open
        Platform.runLater(() -> {
            generatePasswordButton.fire();
            lengthSlider.requestFocus();
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(password -> {
            if (password != null && !password.isEmpty()) {
                targetPasswordField.setText(password);
            }
        });
    }

    public VBox buildDetailsForm(LoginEntry login) {
        // ... (existing code)
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