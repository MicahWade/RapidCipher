package gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

import bridge.CommandRouter; // ADDED
import bridge.BridgeHost;   // ADDED
import core.Database;
import core.Encryption;
import core.MasterPassword;
import core.ConfigManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List; // Added import

public class MainGui extends Application {
    private static MainGui instance;
    private Database database;
    private ObservableList<LoginEntry> loginData;

    private ThemeManager themeManager;
    private GuiBuilder guiBuilder;
    
    private CommandRouter commandRouter; // ADDED

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

    // Fields to manage the bridge server thread
    private bridge.BridgeHost bridgeHost;
    private Thread bridgeThread;

    private double xOffset = 0;
    private double yOffset = 0;

    private Stage primaryStage;

    private ConfigManager.DbConfig dbConfig;

    public MainGui() {
        instance = this;
        this.themeManager = new ThemeManager();
        this.dbConfig = ConfigManager.loadConfig();
        loginData = FXCollections.observableArrayList();
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

        themeManager.styleThemeToggle(themeToggle, themeIcon);
        themeManager.styleIconButton(newLoginButton, MaterialDesign.MDI_PLUS);
        themeManager.styleIconButton(settingsButton, MaterialDesign.MDI_SETTINGS);
        themeManager.styleIconButton(logoutButton, MaterialDesign.MDI_LOGOUT);

        themeManager.styleWindowButton(minimizeButton, false);
        themeManager.styleWindowButton(closeButton, true);

        // This logic re-builds the settings pane or details form to apply new theme styles
        if (settingsPane != null && detailsPane.getChildren().size() > 0 && detailsPane.getChildren().get(0) == settingsPane) {
            settingsPane = guiBuilder.buildSettingsPane();
            detailsPane.getChildren().setAll(settingsPane);
        } else if (loginListView.getSelectionModel().getSelectedItem() == null) {
            // Rebuild Add Form
            addForm = guiBuilder.buildAddForm();
            detailsPane.getChildren().setAll(addForm);
        } else {
            // Rebuild Details Form
            currentDetailsForm = guiBuilder.buildDetailsForm(loginListView.getSelectionModel().getSelectedItem());
            detailsPane.getChildren().setAll(currentDetailsForm);
        }

        loginListView.refresh();
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        themeManager.loadThemePreference();

        AuthManager authManager = new AuthManager(themeManager);
        this.commandRouter = new CommandRouter(themeManager); // INITIALIZE CommandRouter

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

        try {
            database = new Database(dbConfig);
        } catch (Exception e) {
            e.printStackTrace();
            themeManager.showErrorAlert("Database Connection Failed",
                    "Could not connect to the database: " + e.getMessage() + "\n" +
                            "Please check your settings in config.properties or the Settings menu. " +
                            "If you just changed settings, please restart.");
            // Don't exit, allow user to access settings
        }

        primaryStage.setTitle("Rapid Cipher");
        primaryStage.initStyle(StageStyle.TRANSPARENT);

        loadDataFromDatabase(); // Try to load, might be empty if DB connection failed

        loginListView = new ListView<>();
        loginListView.setItems(loginData);
        loginListView.setCellFactory(lv -> new LoginListCell(themeManager));
        loginListView.setPrefWidth(280);
        loginListView.setMinWidth(200);

        detailsPane = new VBox(15);
        detailsPane.setPadding(new Insets(20));

        statusLabel = new Label("Welcome to RapidCipher!");
        statusLabel.setPadding(new Insets(0, 0, 0, 10));

        this.guiBuilder = new GuiBuilder(this, themeManager);

        showAddForm();

        loginListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                currentDetailsForm = guiBuilder.buildDetailsForm(newSelection);
                detailsPane.getChildren().setAll(currentDetailsForm);
            } else {
                showAddForm();
            }
        });

        titleLabel = new Label("RapidCipher");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        themeIcon = new FontIcon();
        themeToggle = new ToggleButton();
        themeToggle.setGraphic(themeIcon);
        themeToggle.setSelected(themeManager.isDarkMode());

        newLoginButton = themeManager.createNewLoginButton();
        settingsButton = themeManager.createSettingsButton();
        logoutButton = themeManager.createLogoutButton();

        themeToggle.setOnAction(e -> {
            themeManager.setDarkMode(themeToggle.isSelected());
            themeManager.updateThemeStyles();
            themeManager.saveThemePreference();
            updateAllStyles();
        });

        newLoginButton.setOnAction(e -> showAddForm());
        settingsButton.setOnAction(e -> showSettingsPane());
        logoutButton.setOnAction(e -> logout());

        minimizeButton = new Button(" _ ");
        minimizeButton.setOnAction(e -> primaryStage.setIconified(true));

        closeButton = new Button(" X ");
        // --- UPDATED ---
        closeButton.setOnAction(e -> {
            stopBridge(); // Stop the bridge server thread
            if (database != null) {
                database.closeConnection();
            }
            primaryStage.close();
            Platform.exit(); // Ensure application exits
            System.exit(0); // Force exit just in case
        });

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
        
        // Handle window close request (e.g., Alt+F4)
        primaryStage.setOnCloseRequest(e -> {
            stopBridge();
            if (database != null) {
                database.closeConnection();
            }
            Platform.exit();
            System.exit(0);
        });

        updateAllStyles();

        primaryStage.show();
        
        // Start the bridge if it's enabled in settings
        if (themeManager.isBridgeEnabled()) {
            startBridge();
        }
    }

    private void showAddForm() {
        loginListView.getSelectionModel().clearSelection();
        addForm = guiBuilder.buildAddForm();
        detailsPane.getChildren().setAll(addForm);
    }

    private void showSettingsPane() {
        loginListView.getSelectionModel().clearSelection();
        settingsPane = guiBuilder.buildSettingsPane();
        detailsPane.getChildren().setAll(settingsPane);
    }

    private void loadDataFromDatabase() {
        if (database == null) {
            System.err.println("Database is null, cannot load data.");
            return;
        }

        loginData.clear();
        ResultSet rs = null;
        try {
            rs = database.searchLogins();
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
                    System.err.println("Failed to decrypt login ID " + id + ": " + e.getMessage());
                }

                loginData.add(new LoginEntry(
                        id,
                        plainTextName,
                        plainTextUser,
                        plainTextPass,
                        plainTextUrl,
                        plainTextNotes));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            themeManager.showErrorAlert("Database Load Failed", "Could not load data from database: " + e.getMessage());
        } finally {
            if (rs != null) {
                try { rs.getStatement().close(); rs.close(); } catch (SQLException e) { /* ignore */ }
            }
        }
    }

    // This method is called by GuiBuilder
    void addLogin(TextField nameField, TextField usernameField, PasswordField passwordField, TextField urlField, TextArea notesField) {
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
        
        if (database == null) {
            themeManager.showErrorAlert("Database Error", "Database is not connected. Check settings.");
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

    void deleteLogin(LoginEntry login) {
        if (database == null) {
            themeManager.showErrorAlert("Database Error", "Database is not connected. Check settings.");
            return;
        }
        
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

    void saveDbSettings(ConfigManager.DbConfig newConfig, boolean isMigrationChecked, Label restartLabel) {
        
        // New Migration Logic
        if (isMigrationChecked) {
            if (database == null) {
                 themeManager.showErrorAlert("Migration Failed", "Cannot migrate data, source database is not connected.");
                 return;
            }
            
            Database sourceDb = null;
            Database destDb = null;
            try {
                System.out.println("Starting migration: Connecting to source DB...");
                sourceDb = this.database; // Use the already open connection
                
                System.out.println("Connecting to destination DB...");
                destDb = new Database(newConfig);

                System.out.println("Reading data from source...");
                List<LoginEntry> entries = sourceDb.getAllLoginEntries(MasterPassword.getKey());
                
                System.out.println("Wiping destination database...");
                destDb.deleteAllLogins();
                
                System.out.println("Writing " + entries.size() + " entries to destination...");
                for (LoginEntry entry : entries) {
                    destDb.createLogin(
                        entry.getName(), 
                        entry.getUsername(), 
                        entry.getPassword(), 
                        entry.getUrl(), 
                        entry.getNotes()
                    );
                }
                
                System.out.println("Migration successful.");
                
            } catch (Exception e) {
                e.printStackTrace();
                themeManager.showErrorAlert("Migration Failed", "Could not migrate data: " + e.getMessage());
                // Stop! Don't save the config if migration failed.
                return; 
            } finally {
                // Close the *new* database connection, but leave the source (main) one open
                if (destDb != null) {
                    destDb.closeConnection();
                }
            }
        }

        // --- Original saveDbSettings logic ---
        ConfigManager.saveConfig(newConfig);
        this.dbConfig = ConfigManager.loadConfig();
        restartLabel.setVisible(true);
        statusLabel.setText("Settings saved. Please restart.");
        statusLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentSuccessColor() + ";");
    }

    private void logout() {
        MasterPassword.setKey(null);
        loginData.clear();
        loginListView.refresh();
        showAddForm();
        
        // Stop the bridge on logout
        stopBridge();

        primaryStage.hide();

        if (database != null) {
            database.closeConnection();
        }
        database = null;

        AuthManager authManager = new AuthManager(themeManager);
        this.commandRouter = new CommandRouter(themeManager); // Re-create router
        
        boolean proceed = false;
        try {
            proceed = authManager.showMasterPasswordPrompt();
        } catch (Exception e) {
            e.printStackTrace();
            themeManager.showErrorAlert("Fatal Error", "Failed to initialize encryption settings.\n" + e.getMessage());
        }

        if (proceed) {
            try {
                this.dbConfig = ConfigManager.loadConfig();
                database = new Database(dbConfig); 
            } catch (Exception e) {
                e.printStackTrace();
                themeManager.showErrorAlert("Database Connection Failed", "Could not reconnect to database: " + e.getMessage());
            }

            loadDataFromDatabase();
            primaryStage.show();
            
            // Restart the bridge if it was enabled
            if (themeManager.isBridgeEnabled()) {
                startBridge();
            }
        } else {
            Platform.exit();
            System.exit(0);
        }
    }

    // --- Methods to control the bridge server thread ---

    /**
     * Toggles the bridge thread on or off based on the setting.
     * @param enable True to start the bridge, false to stop it.
     */
    public void toggleBridge(boolean enable) {
        if (enable) {
            startBridge();
        } else {
            stopBridge();
        }
    }

    /**
     * Starts the native messaging bridge server on a new daemon thread.
     */
    private void startBridge() {
        if (bridgeThread != null && bridgeThread.isAlive()) {
            System.out.println("Bridge thread is already running.");
            return;
        }
        
        // Check if user is logged in
        try {
            MasterPassword.getKey(); // This will throw if locked
        } catch (IllegalStateException e) {
            statusLabel.setText("Please unlock the vault to enable the bridge.");
            statusLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentErrorColor() + ";");
            
            if (settingsPane != null && detailsPane.getChildren().size() > 0 && detailsPane.getChildren().get(0) == settingsPane) {
                showSettingsPane(); // This will rebuild the pane with the checkbox unchecked
            }
            return; 
        }
        
        System.out.println("Starting Native Messaging Bridge server thread...");
        bridgeHost = new bridge.BridgeHost(this.commandRouter); // Pass the router
        bridgeThread = new Thread(bridgeHost, "RapidCipher-Bridge-Server-Thread");
        bridgeThread.setDaemon(true); // Daemon thread will exit when JVM exits
        bridgeThread.start();
        statusLabel.setText("Browser bridge enabled.");
        statusLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentSuccessColor() + ";");
    }

    /**
     * Stops the native messaging bridge server thread.
     */
    private void stopBridge() {
        if (bridgeHost != null) {
            System.out.println("Stopping Native Messaging Bridge server thread...");
            bridgeHost.stop(); // This will close the ServerSocket
            bridgeHost = null;
        }
        if (bridgeThread != null) {
            bridgeThread.interrupt(); // Interrupt the thread
            bridgeThread = null;
        }
        System.out.println("Bridge server thread stopped.");
        
        if (statusLabel != null && primaryStage != null && !primaryStage.isIconified()) {
           statusLabel.setText("Browser bridge disabled.");
           statusLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentMutedTextColor() + ";");
        }
    }

    // --- Getters for GuiBuilder ---
    public Label getStatusLabel() {
        return statusLabel;
    }

    public ConfigManager.DbConfig getDbConfig() {
        return dbConfig;
    }
    
    public Database getDatabase() {
        return database;
    }
    
    public ObservableList<LoginEntry> getLoginData() {
        return loginData;
    }
    
    public ListView<LoginEntry> getLoginListView() {
        return loginListView;
    }
}


