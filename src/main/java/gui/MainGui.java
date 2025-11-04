package gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign; 

import core.Database;
import core.Encryption;
import core.MasterPassword;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MainGui extends Application {
    private static MainGui instance;
    private Database database;
    private ObservableList<LoginEntry> loginData;
    
    // Add a ThemeManager instance
    private ThemeManager themeManager;

    private ListView<LoginEntry> loginListView;
    private VBox detailsPane;
    private VBox addForm;
    private VBox currentDetailsForm;
    private VBox settingsPane; 
    private Label statusLabel;
    private StackPane root;
    private VBox mainLayout;
    private SplitPane splitPane;
    private HBox topBar;
    
    private ToggleButton themeToggle;
    private FontIcon themeIcon;
    
    private Label titleLabel;
    private Button newLoginButton; 
    private Button settingsButton; 
    private Button logoutButton; 
    private Button minimizeButton;
    private Button closeButton;
    
    private double xOffset = 0;
    private double yOffset = 0;
    
    private Stage primaryStage; 


    public MainGui() {
        instance = this;
        this.themeManager = new ThemeManager(); 
        
        try {
            database = Database.getInstance();
            loginData = FXCollections.observableArrayList();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static synchronized MainGui getInstance() {
        return instance;
    }

    private void updateAllStyles() {
        
        root.setStyle("-fx-background-color: " + themeManager.getCurrentBaseSemiTransparent() + "; -fx-background-radius: 20;");
        mainLayout.setStyle("-fx-background-color: " + themeManager.getCurrentBaseColor() + "; -fx-background-radius: 15;");
        mainLayout.setEffect(themeManager.getLightOuterShadow());
        
        splitPane.setStyle("-fx-background-color: " + themeManager.getCurrentBaseColor() + ";");
        loginListView.setStyle("-fx-background-color: " + themeManager.getCurrentBaseColor() + ";");
        detailsPane.setStyle("-fx-background-color: " + themeManager.getCurrentBaseColor() + ";");
        
        statusLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentMutedTextColor() + ";");
        
        titleLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentMutedTextColor() + ";");
        
        // --- THIS IS THE FIX ---
        // Re-style all top-bar icon buttons
        themeManager.styleThemeToggle(themeToggle, themeIcon);
        themeManager.styleIconButton(newLoginButton, MaterialDesign.MDI_PLUS);
        themeManager.styleIconButton(settingsButton, MaterialDesign.MDI_SETTINGS);
        themeManager.styleIconButton(logoutButton, MaterialDesign.MDI_LOGOUT);
        // --- END OF FIX ---
        
        themeManager.styleWindowButton(minimizeButton, false);
        themeManager.styleWindowButton(closeButton, true);

        // UPDATED: Rebuild the visible pane to apply new styles
        if (settingsPane != null && detailsPane.getChildren().get(0) == settingsPane) {
            settingsPane = buildSettingsPane(); // Rebuild
            detailsPane.getChildren().setAll(settingsPane); // Re-set
        } else if (loginListView.getSelectionModel().getSelectedItem() == null) {
            // Check if addForm is the one visible, or just default to it
            if (addForm == null || detailsPane.getChildren().get(0) == addForm) {
                addForm = buildAddForm(); // Rebuild
                detailsPane.getChildren().setAll(addForm); // Re-set
            }
        } else {
            currentDetailsForm = buildDetailsForm(loginListView.getSelectionModel().getSelectedItem()); // Rebuild
            detailsPane.getChildren().setAll(currentDetailsForm); // Re-set
        }

        loginListView.refresh();
    }

    @Override
    public void start(Stage primaryStage) {
        
        this.primaryStage = primaryStage; 
        
        themeManager.loadThemePreference();

        
        AuthManager authManager = new AuthManager(themeManager);
        
        boolean proceed = false;
        try {
            proceed = authManager.showMasterPasswordPrompt();
        } catch (Exception e) {
            e.printStackTrace();
            themeManager.showErrorAlert("Fatal Error", "Failed to initialize encryption settings.\n" + e.getMessage());
        }
        
        if (!proceed) {
            Platform.exit();
            return;
        }

        primaryStage.setTitle("Rapid Cipher");
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        
        loadDataFromDatabase();

        loginListView = new ListView<>();
        loginListView.setItems(loginData);

        loginListView.setCellFactory(lv -> new LoginListCell(themeManager)); 
        loginListView.setPrefWidth(280);
        loginListView.setMinWidth(200);

        detailsPane = new VBox(15);
        detailsPane.setPadding(new Insets(20));
        
        showAddForm();

        loginListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                currentDetailsForm = buildDetailsForm(newSelection);
                detailsPane.getChildren().setAll(currentDetailsForm);
            } else {
                showAddForm();
            }
        });

        titleLabel = new Label("RapidCipher");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        // --- Create buttons using the ThemeManager ---
        themeIcon = new FontIcon();
        themeToggle = new ToggleButton();
        themeToggle.setGraphic(themeIcon);
        themeToggle.setSelected(themeManager.isDarkMode());
        
        newLoginButton = themeManager.createNewLoginButton();
        settingsButton = themeManager.createSettingsButton();
        logoutButton = themeManager.createLogoutButton();
        
        // --- Set Actions ---
        themeToggle.setOnAction(e -> {
            themeManager.setDarkMode(themeToggle.isSelected());
            themeManager.updateThemeStyles();
            themeManager.saveThemePreference();
            updateAllStyles(); // Re-style the GUI
        });
        
        newLoginButton.setOnAction(e -> showAddForm());
        settingsButton.setOnAction(e -> showSettingsPane());
        logoutButton.setOnAction(e -> logout());

        minimizeButton = new Button(" _ ");
        minimizeButton.setOnAction(e -> primaryStage.setIconified(true));
        
        closeButton = new Button(" X ");
        closeButton.setOnAction(e -> primaryStage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar = new HBox(10, titleLabel, spacer, newLoginButton, themeToggle, settingsButton, logoutButton, minimizeButton, closeButton);
        topBar.setPadding(new Insets(5));
        topBar.setAlignment(Pos.CENTER);

        topBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        
        topBar.setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() - xOffset);
            primaryStage.setY(event.getScreenY() - yOffset);
        });

        splitPane = new SplitPane(loginListView, detailsPane);
        splitPane.setDividerPositions(0.35);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        statusLabel = new Label("Welcome to RapidCipher!");
        statusLabel.setPadding(new Insets(0, 0, 0, 10));

        mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(10));
        mainLayout.getChildren().addAll(topBar, splitPane, statusLabel);
        mainLayout.setMaxWidth(1000);
        mainLayout.setMaxHeight(700);

        root = new StackPane(mainLayout);
        root.setPadding(new Insets(20));
        
        Scene scene = new Scene(root, 1000, 700);
        scene.setFill(Color.TRANSPARENT);
                
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        
        // --- Initial Style ---
        updateAllStyles();
        
        primaryStage.show();
    }
    
    private void showAddForm() {
        loginListView.getSelectionModel().clearSelection();
        addForm = buildAddForm(); // Rebuild to get fresh button styles
        detailsPane.getChildren().setAll(addForm);
    }
    
    private void showSettingsPane() {
        loginListView.getSelectionModel().clearSelection();
        settingsPane = buildSettingsPane(); // Always rebuild in case styles changed
        detailsPane.getChildren().setAll(settingsPane);
    }
    
    private VBox buildSettingsPane() {
        VBox pane = new VBox(20);
        pane.setPadding(new Insets(10));
        
        Label title = new Label("Settings");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        
        Label placeholder = new Label("More settings will be available here in the future.");
        placeholder.setStyle("-fx-text-fill: " + themeManager.getCurrentMutedTextColor() + ";");
        
        pane.getChildren().addAll(title, placeholder); 
        return pane;
    }
    
    private VBox buildAddForm() {
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

        // This button is created fresh, so it will get the new theme colors
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
        
        addButton.setOnAction(e -> addLogin(nameField, usernameField, passwordField, urlField, notesField));
        
        form.getChildren().addAll(title, nameField, usernameField, passBox, urlField, notesField, addButton);
        return form;
    }

    private VBox buildDetailsForm(LoginEntry login) {
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
        
        // This button is created fresh, so it will get the new theme colors
        Button copyUserButton = themeManager.createCopyButton(); 
        copyUserButton.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(login.getUsername());
            clipboard.setContent(content);
            statusLabel.setText("Username copied to clipboard.");
            statusLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentSuccessColor() + ";");
        });
        
        HBox userBox = new HBox(10, userDisplay, copyUserButton);
        userBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(userDisplay, Priority.ALWAYS); 

        TextField passDisplay = themeManager.createStyledTextField("Password");
        passDisplay.setText("************");
        passDisplay.setEditable(false);
        
        // This button is created fresh, so it will get the new theme colors
        ToggleButton showHideButton = themeManager.createShowHideButton(); 
        showHideButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                passDisplay.setText(login.getPassword());
            } else {
                passDisplay.setText("************");
            }
        });
        
        // This button is created fresh, so it will get the new theme colors
        Button copyPassButton = themeManager.createCopyButton(); 
        copyPassButton.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(login.getPassword()); 
            clipboard.setContent(content);
            statusLabel.setText("Password copied to clipboard.");
            statusLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentSuccessColor() + ";");
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
        deleteButton.setOnAction(e -> deleteLogin(login));
        
        HBox buttonBar = new HBox(10, deleteButton);
        
        form.getChildren().addAll(title, nameDisplay, userBox, passBox, urlDisplay, notesDisplay, buttonBar);
        return form;
    }
    
    private void loadDataFromDatabase() {
        loginData.clear();
        try {
            ResultSet rs = database.searchLogins(); 
            while (rs != null && rs.next()) {
                
                long id = rs.getLong("id");
                String plainTextName = "!!DECRYPT_ERROR!!";
                String plainTextUser = "!!DECRYPT_ERROR!!";
                String plainTextPass = "!!DECRYPT_ERROR!!";
                String plainTextUrl = "!!DECRYPT_ERROR!!";
                String plainTextNotes = "!!DECRYPT_ERROR!!";

                try {
                    plainTextName = Encryption.decryptWithIV(rs.getString("name"), MasterPassword.getKey());
                    plainTextUser = Encryption.decryptWithIV(rs.getString("username"), MasterPassword.getKey());
                    plainTextPass = Encryption.decryptWithIV(rs.getString("password"), MasterPassword.getKey());
                    plainTextUrl = Encryption.decryptWithIV(rs.getString("url"), MasterPassword.getKey());
                    plainTextNotes = Encryption.decryptWithIV(rs.getString("notes"), MasterPassword.getKey());

                } catch (Exception e) {
                    e.printStackTrace();
                }

                loginData.add(new LoginEntry(
                        id,
                        plainTextName,
                        plainTextUser,
                        plainTextPass,
                        plainTextUrl,
                        plainTextNotes
                ));
            }
            if (rs != null) rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addLogin(TextField nameField, TextField usernameField, PasswordField passwordField, TextField urlField, TextField notesField) {
        String name = nameField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();
        String url = urlField.getText();
        String notes = notesField.getText();

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Name, Username, and Password are mandatory.");
            statusLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentErrorColor() + ";");
            return;
        }

        long newId = database.createLogin(name, username, password, url, notes);
        
        if (newId != -1) {
            statusLabel.setText("Login added successfully.");
            statusLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentSuccessColor() + ";");
            
            LoginEntry newLogin = new LoginEntry(newId, name, username, password, url, notes); 
            loginData.add(newLogin);
            loginListView.getSelectionModel().select(newLogin);
            
            nameField.clear();
            usernameField.clear();
            passwordField.clear();
            urlField.clear();
            notesField.clear();
        } else {
            statusLabel.setText("Failed to add login.");
            statusLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentErrorColor() + ";");
        }
    }
    
    private void deleteLogin(LoginEntry login) {
        boolean success = database.deleteLogin(login.getId());
        if (success) {
            statusLabel.setText("Login deleted successfully.");
            statusLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentSuccessColor() + ";");
            loginData.remove(login);
            loginListView.getSelectionModel().clearSelection();
        } else {
            statusLabel.setText("Failed to delete login.");
            statusLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentErrorColor() + ";");
        }
    }
    
    private void logout() {
        // 1. Clear the sensitive data
        MasterPassword.setKey(null); // Clear the static key
        loginData.clear(); // Clear the observable list
        loginListView.refresh();
        showAddForm(); // Reset view to default
        
        // 2. Hide the main window
        primaryStage.hide();

        // 3. Re-show the authentication prompt
        AuthManager authManager = new AuthManager(themeManager);
        boolean proceed = false;
        try {
            proceed = authManager.showMasterPasswordPrompt();
        } catch (Exception e) {
            e.printStackTrace();
            themeManager.showErrorAlert("Fatal Error", "Failed to initialize encryption settings.\n" + e.getMessage());
        }
        
        // 4. Decide what to do
        if (proceed) {
            // Re-load data and show the window
            loadDataFromDatabase();
            primaryStage.show();
        } else {
            // User cancelled login, so exit the app
            Platform.exit();
        }
    }
}