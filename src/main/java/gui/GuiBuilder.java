package gui;

import core.ConfigManager;
import core.Encryption;
import core.MasterPassword;
import core.PasswordGenerator;
import javafx.application.Platform;
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
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.StageStyle;

import java.util.Optional;
import java.util.function.UnaryOperator; // Added for TextFormatter

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
        
        // --- MODIFIED ---
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

        // --- MODIFIED prompt text for clarity ---
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
        remoteFields.add(new Label("DB Name/Path:") { // Updated label
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
        
        // --- MODIFIED LOGIC ---
        // Decrypt if *not* SQLite
        if (!currentDbConfig.dbType().equals("SQLITE")) {
            try {
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

        // --- MODIFIED LOGIC ---
        // Show remote fields if not SQLite
        remoteFields.setVisible(!currentDbConfig.dbType().equals("SQLITE"));
        
        CheckBox migrateDataCheck = new CheckBox("Copy data from current DB to new DB");
        migrateDataCheck.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        migrateDataCheck.setVisible(false); 

        dbTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            // --- MODIFIED LOGIC ---
            // Show remote fields if not SQLite
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

    // ... (rest of GuiBuilder.java is unchanged) ...
    
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
        TextField notesField = themeManager.createStyledTextField("Notes");
        notesField.setPrefHeight(80);

        Button addButton = themeManager.createStyledButton("Add Login");

        addButton.setOnAction(e -> mainGui.addLogin(nameField, usernameField, passwordField, urlField, notesField));

        form.getChildren().addAll(title, nameField, usernameField, passBox, urlField, notesField, addButton);
        return form;
    }
    
    // --- UPDATED METHOD ---
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
        
        // --- Filter for numeric input ---
        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) { // Allow empty or all digits
                return change;
            }
            return null; // Reject the change
        };

        // --- Password Tab ---
        VBox passwordTabContent = new VBox(15);
        passwordTabContent.setPadding(new Insets(10));
        
        Label lengthLabel = new Label("Length:");
        lengthLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        
        TextField lengthField = themeManager.createStyledTextField("16");
        lengthField.setText("16"); // Default value
        lengthField.setPrefWidth(70);
        lengthField.setTextFormatter(new TextFormatter<>(integerFilter));

        HBox lengthBox = new HBox(10, lengthLabel, lengthField);
        lengthBox.setAlignment(Pos.CENTER_LEFT);
        
        CheckBox upperCheck = themeManager.createStyledCheckBox("Uppercase (A-Z)");
        upperCheck.setSelected(true);
        CheckBox digitsCheck = themeManager.createStyledCheckBox("Digits (0-9)");
        digitsCheck.setSelected(true);
        CheckBox symbolsCheck = themeManager.createStyledCheckBox("Symbols (!@#...)");
        symbolsCheck.setSelected(true);
        
        Label minDigitsLabel = new Label("Min Digits:");
        minDigitsLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        Spinner<Integer> minDigitsSpinner = new Spinner<>(0, 10, 1);
        minDigitsSpinner.setPrefWidth(70);
        minDigitsSpinner.disableProperty().bind(digitsCheck.selectedProperty().not());
        
        Label minSymbolsLabel = new Label("Min Symbols:");
        minSymbolsLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        Spinner<Integer> minSymbolsSpinner = new Spinner<>(0, 10, 1);
        minSymbolsSpinner.setPrefWidth(70);
        minSymbolsSpinner.disableProperty().bind(symbolsCheck.selectedProperty().not());
        
        HBox minBox = new HBox(10, minDigitsLabel, minDigitsSpinner, minSymbolsLabel, minSymbolsSpinner);
        minBox.setAlignment(Pos.CENTER_LEFT);
        
        Button generatePasswordButton = themeManager.createStyledButton("Generate Password");
        generatePasswordButton.setOnAction(e -> {
            int minLower = 1;
            int minUpper = upperCheck.isSelected() ? 1 : 0;
            int minDigits = digitsCheck.isSelected() ? minDigitsSpinner.getValue() : 0;
            int minSymbols = symbolsCheck.isSelected() ? minSymbolsSpinner.getValue() : 0;
            int totalMin = minLower + minUpper + minDigits + minSymbols;

            int length = 16; // Default
            try {
                length = Integer.parseInt(lengthField.getText());
            } catch (NumberFormatException ex) {
                // Keep default
            }

            if (length < Math.max(8, totalMin)) {
                length = Math.max(8, totalMin);
                lengthField.setText(String.valueOf(length)); // Update field
            }
            
            resultField.setText(PasswordGenerator.generatePassword(
                length,
                upperCheck.isSelected(),
                digitsCheck.isSelected(),
                symbolsCheck.isSelected(),
                minDigitsSpinner.getValue(),
                minSymbolsSpinner.getValue()
            ));
        });
        
        passwordTabContent.getChildren().addAll(
            lengthBox,
            upperCheck, digitsCheck, symbolsCheck,
            minBox,
            generatePasswordButton
        );
        passwordTabContent.setStyle("-fx-background-color: " + themeManager.getCurrentBaseColor() + ";");

        // --- Passphrase Tab ---
        VBox passphraseTabContent = new VBox(15);
        passphraseTabContent.setPadding(new Insets(10));
        
        Label wordsLabel = new Label("Words:");
        wordsLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        
        TextField wordsField = themeManager.createStyledTextField("4");
        wordsField.setText("4"); // Default value
        wordsField.setPrefWidth(70);
        wordsField.setTextFormatter(new TextFormatter<>(integerFilter));

        HBox wordsBox = new HBox(10, wordsLabel, wordsField);
        wordsBox.setAlignment(Pos.CENTER_LEFT);

        Label separatorLabel = new Label("Separator:");
        separatorLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        TextField separatorField = themeManager.createStyledTextField("-");
        separatorField.setMaxWidth(100);
        HBox separatorBox = new HBox(10, separatorLabel, separatorField);
        separatorBox.setAlignment(Pos.CENTER_LEFT);
        
        Button generatePassphraseButton = themeManager.createStyledButton("Generate Passphrase");
        generatePassphraseButton.setOnAction(e -> {
            int numWords = 4; // Default
            try {
                numWords = Integer.parseInt(wordsField.getText());
            } catch (NumberFormatException ex) {
                // Keep default
            }
            
            if (numWords < 3) {
                numWords = 3;
                wordsField.setText("3"); // Update field
            }
            
            resultField.setText(PasswordGenerator.generatePassphrase(
                numWords,
                separatorField.getText()
            ));
        });
        
        passphraseTabContent.getChildren().addAll(
            wordsBox,
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
            lengthField.requestFocus();
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