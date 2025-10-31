package gui;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import java.util.Properties;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class MainGui extends Application {
    private static MainGui instance;
    private Database database;
    private ObservableList<LoginEntry> loginData;

    private ListView<LoginEntry> loginListView;
    private VBox detailsPane;
    private VBox addForm;
    private VBox currentDetailsForm;
    private Label statusLabel;
    private StackPane root;
    private VBox mainLayout;
    private SplitPane splitPane;
    private HBox topBar;
    private ToggleButton themeToggle;
    private FontIcon themeIcon;
    private Label titleLabel;
    private Button minimizeButton;
    private Button closeButton;

    private boolean isDarkMode = isSystemDarkMode();
    
    private String currentBaseColor;
    private String currentBaseSemiTransparent;
    private String currentDarkShadowColor;
    private String currentLightShadowColor;
    private String currentControlInnerBase;
    private String currentTextColor;
    private String currentMutedTextColor;
    private String currentAccentColor = "#007aff";
    private String currentErrorColor = "#d93025";
    private String currentSuccessColor = "#1e8e3e";

    private DropShadow lightOuterShadow;
    private InnerShadow lightInnerShadow;
    
    private double xOffset = 0;
    private double yOffset = 0;
    
    private static final String DB_DIR_PATH = System.getProperty("user.home") + "/Documents/RapidCipher";
    private static final Path SALT_FILE = Paths.get(DB_DIR_PATH, "salt.bin");
    private static final Path KEY_CHECK_FILE = Paths.get(DB_DIR_PATH, "key_check.bin");
    private static final String KEY_CHECK_STRING = "RapidCipher-OK";
    
    private static final Path SETTINGS_FILE = Paths.get(DB_DIR_PATH, "settings.properties");


    public MainGui() {
        instance = this;
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

    // --- NEW: Tries to detect system dark mode ---
    private boolean isSystemDarkMode() {
        String os = System.getProperty("os.name").toLowerCase();
        String command;
        boolean isDark = false; // Default to light

        try {
            if (os.contains("win")) {
                // Windows: Checks registry. 0 = Dark, 1 = Light
                command = "reg query \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize\" /v AppsUseLightTheme";
                Process process = Runtime.getRuntime().exec(command);
                String result = new BufferedReader(new InputStreamReader(process.getInputStream()))
                        .lines().filter(line -> line.contains("AppsUseLightTheme")).findFirst().orElse("");
                
                process.waitFor(1, TimeUnit.SECONDS);
                if (result.contains("0x0")) {
                    isDark = true;
                }
            } else if (os.contains("mac")) {
                // macOS: Checks AppleInterfaceStyle. "Dark" = Dark, non-existent = Light
                command = "defaults read -g AppleInterfaceStyle";
                Process process = Runtime.getRuntime().exec(command);
                String result = new BufferedReader(new InputStreamReader(process.getInputStream()))
                        .lines().findFirst().orElse("");
                
                process.waitFor(1, TimeUnit.SECONDS);
                if (result.trim().equals("Dark")) {
                    isDark = true;
                }
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux (GTK/GNOME): Checks gsettings. 'prefer-dark' = Dark
                command = "gsettings get org.gnome.desktop.interface color-scheme";
                Process process = Runtime.getRuntime().exec(command);
                String result = new BufferedReader(new InputStreamReader(process.getInputStream()))
                        .lines().findFirst().orElse("");
                
                process.waitFor(1, TimeUnit.SECONDS);
                if (result.contains("prefer-dark")) {
                    isDark = true;
                }
            }
        } catch (Exception e) {
            System.err.println("Could not detect system theme: " + e.getMessage());
            // Falls back to default 'isDark = false'
        }
        
        return isDark;
    }


    private void updateThemeStyles() {
        if (isDarkMode) {
            currentBaseColor = "#383e46";
            currentBaseSemiTransparent = "rgba(56, 62, 70, 0.85)";
            currentDarkShadowColor = "#2a2e34";
            currentLightShadowColor = "#464e58";
            currentControlInnerBase = "#32383e";
            currentTextColor = "#e0e5ec";
            currentMutedTextColor = "#a3b1c6";
        } else {
            currentBaseColor = "#e0e5ec";
            currentBaseSemiTransparent = "rgba(224, 229, 236, 0.85)";
            currentDarkShadowColor = "#a3b1c6";
            currentLightShadowColor = "#ffffff";
            currentControlInnerBase = "#E3E9F0";
            currentTextColor = "#333333";
            currentMutedTextColor = "#555555";
        }

        DropShadow darkOuterShadow = new DropShadow(10, 5, 5, Color.web(currentDarkShadowColor));
        darkOuterShadow.setOffsetX(5);
        darkOuterShadow.setOffsetY(5);

        lightOuterShadow = new DropShadow(10, 5, 5, Color.web(currentLightShadowColor));
        lightOuterShadow.setOffsetX(-5);
        lightOuterShadow.setOffsetY(-5);
        lightOuterShadow.setInput(darkOuterShadow);
        
        InnerShadow darkInnerShadow = new InnerShadow(10, 2, 2, Color.web(currentDarkShadowColor));
        darkInnerShadow.setOffsetX(2);
        darkInnerShadow.setOffsetY(2);

        lightInnerShadow = new InnerShadow(10, 2, 2, Color.web(currentLightShadowColor));
        lightInnerShadow.setOffsetX(-2);
        lightInnerShadow.setOffsetY(-2);
        lightInnerShadow.setInput(darkInnerShadow);
    }

    private void updateAllStyles() {
        updateThemeStyles();

        root.setStyle("-fx-background-color: " + currentBaseSemiTransparent + "; -fx-background-radius: 20;");
        mainLayout.setStyle("-fx-background-color: " + currentBaseColor + "; -fx-background-radius: 15;");
        mainLayout.setEffect(lightOuterShadow);
        
        splitPane.setStyle("-fx-background-color: " + currentBaseColor + ";");
        loginListView.setStyle("-fx-background-color: " + currentBaseColor + ";");
        detailsPane.setStyle("-fx-background-color: " + currentBaseColor + ";");
        
        statusLabel.setStyle("-fx-text-fill: " + currentMutedTextColor + ";");
        
        titleLabel.setStyle("-fx-text-fill: " + currentMutedTextColor + ";");
        styleThemeToggle(themeToggle);
        styleWindowButton(minimizeButton, false);
        styleWindowButton(closeButton, true);

        if (loginListView.getSelectionModel().getSelectedItem() == null) {
            addForm = buildAddForm();
            detailsPane.getChildren().setAll(addForm);
        } else {
            currentDetailsForm = buildDetailsForm(loginListView.getSelectionModel().getSelectedItem());
            detailsPane.getChildren().setAll(currentDetailsForm);
        }

        loginListView.refresh();
    }

    @Override
    public void start(Stage primaryStage) {
        
        updateThemeStyles();

        boolean proceed = false;
        try {
            proceed = showMasterPasswordPrompt();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Fatal Error", "Failed to initialize encryption settings.\n" + e.getMessage());
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
        loginListView.setCellFactory(lv -> new LoginListCell());
        loginListView.setPrefWidth(280);
        loginListView.setMinWidth(200);

        detailsPane = new VBox(15);
        detailsPane.setPadding(new Insets(20));
        
        addForm = buildAddForm();
        detailsPane.getChildren().add(addForm);

        loginListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                currentDetailsForm = buildDetailsForm(newSelection);
                detailsPane.getChildren().setAll(currentDetailsForm);
            } else {
                detailsPane.getChildren().setAll(addForm);
            }
        });

        titleLabel = new Label("RapidCipher");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        themeIcon = new FontIcon();
        themeToggle = new ToggleButton();
        themeToggle.setGraphic(themeIcon);
        
        loadThemePreference();
        
        themeToggle.setOnAction(e -> {
            isDarkMode = themeToggle.isSelected();
            updateAllStyles();
            saveThemePreference(); // Save the user's choice
        });

        minimizeButton = new Button(" _ ");
        minimizeButton.setOnAction(e -> primaryStage.setIconified(true));
        
        closeButton = new Button(" X ");
        closeButton.setOnAction(e -> primaryStage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar = new HBox(10, titleLabel, spacer, themeToggle, minimizeButton, closeButton);
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
        
        updateAllStyles();
        
        primaryStage.show();
    }

    private void loadThemePreference() {
        if (!Files.exists(SETTINGS_FILE)) {
            this.themeToggle.setSelected(this.isDarkMode);
            return;
        }
        
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(SETTINGS_FILE)) {
            props.load(in);
            // Load the saved preference, overriding the system default
            this.isDarkMode = Boolean.parseBoolean(props.getProperty("isDarkMode", "false"));
            this.themeToggle.setSelected(this.isDarkMode);
        } catch (Exception e) {
            System.err.println("Failed to load theme preference: " + e.getMessage());
        }
    }

    private void saveThemePreference() {
        Properties props = new Properties();
        props.setProperty("isDarkMode", String.valueOf(this.isDarkMode));
        try (OutputStream out = Files.newOutputStream(SETTINGS_FILE)) {
            props.store(out, "RapidCipher User Preferences");
        } catch (Exception e) {
            System.err.println("Failed to save theme preference: " + e.getMessage());
        }
    }
    
    // --- (Rest of the file is unchanged) ---
    
    private boolean showMasterPasswordPrompt() throws Exception {
        Files.createDirectories(Paths.get(DB_DIR_PATH));

        if (Files.exists(SALT_FILE) && Files.exists(KEY_CHECK_FILE)) {
            return showLoginPrompt();
        } else {
            return showFirstRunPrompt();
        }
    }

    private boolean showLoginPrompt() throws Exception {
        Dialog<String> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.getDialogPane().setStyle("-fx-background-color: " + currentBaseSemiTransparent + "; -fx-background-radius: 15;");
        dialog.getDialogPane().setEffect(lightOuterShadow);

        dialog.setTitle("Login");
        dialog.setHeaderText("Enter your master password for RapidCipher.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        Label label = new Label("Password:");
        label.setStyle("-fx-text-fill: " + currentTextColor + ";");
        
        PasswordField pwd = createStyledPasswordField("");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(label, 0, 0);
        grid.add(pwd, 1, 0);
        dialog.getDialogPane().setContent(grid);

        Platform.runLater(pwd::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return pwd.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().isEmpty()) {
            String password = result.get();
            try {
                byte[] salt = Files.readAllBytes(SALT_FILE);
                SecretKey key = Encryption.getSecretKey(password, salt);
                
                String[] checkData = new String(Files.readAllBytes(KEY_CHECK_FILE), "UTF-8").split(":");
                byte[] iv = Base64.getDecoder().decode(checkData[0]);
                String encryptedCheck = checkData[1];
                
                String decrypted = Encryption.decrypt(encryptedCheck, key, new IvParameterSpec(iv));

                if (KEY_CHECK_STRING.equals(decrypted)) {
                    MasterPassword.setKey(key);
                    return true;
                } else {
                    showErrorAlert("Login Failed", "Incorrect password.");
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                showErrorAlert("Login Error", "Failed to decrypt key data. Is the password correct?");
                return false;
            }
        }
        return false;
    }

    private boolean showFirstRunPrompt() throws Exception {
        Dialog<String> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.getDialogPane().setStyle("-fx-background-color: " + currentBaseSemiTransparent + "; -fx-background-radius: 15;");
        dialog.getDialogPane().setEffect(lightOuterShadow);
        
        dialog.setTitle("Welcome to RapidCipher");
        dialog.setHeaderText("Please create a new master password.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Label label1 = new Label("Password:");
        label1.setStyle("-fx-text-fill: " + currentTextColor + ";");
        Label label2 = new Label("Confirm:");
        label2.setStyle("-fx-text-fill: " + currentTextColor + ";");

        PasswordField pwd1 = createStyledPasswordField("");
        PasswordField pwd2 = createStyledPasswordField("");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(label1, 0, 0);
        grid.add(pwd1, 1, 0);
        grid.add(label2, 0, 1);
        grid.add(pwd2, 1, 1);
        dialog.getDialogPane().setContent(grid);

        Platform.runLater(pwd1::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                String p1 = pwd1.getText();
                if (p1.isEmpty() || !p1.equals(pwd2.getText())) {
                    showErrorAlert("Password Mismatch", "Passwords do not match or are empty.");
                    return null;
                }
                return p1;
            }
            return "CANCEL";
        });

        String password = null;
        while (password == null) {
            Optional<String> result = dialog.showAndWait();
            if (!result.isPresent() || "CANCEL".equals(result.get())) {
                return false;
            }
            password = result.get();
        }

        byte[] salt = Encryption.generateSalt();
        Files.write(SALT_FILE, salt);

        SecretKey key = Encryption.getSecretKey(password, salt);

        byte[] iv = Encryption.generateIv();
        String encryptedCheck = Encryption.encrypt(KEY_CHECK_STRING, key, new IvParameterSpec(iv));
        
        String checkData = Base64.getEncoder().encodeToString(iv) + ":" + encryptedCheck;
        Files.write(KEY_CHECK_FILE, checkData.getBytes("UTF-8"));

        MasterPassword.setKey(key);
        return true;
    }
    
    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initStyle(StageStyle.TRANSPARENT);
        alert.getDialogPane().setStyle("-fx-background-color: " + currentBaseColor + "; -fx-background-radius: 15;");
        alert.getDialogPane().setEffect(lightOuterShadow);
        
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        alert.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: " + currentTextColor + ";");
        
        alert.showAndWait();
    }
    
    private VBox buildAddForm() {
        VBox form = new VBox(15);
        form.setPadding(new Insets(10));
        
        Label title = new Label("Add New Login");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setStyle("-fx-text-fill: " + currentTextColor + ";");

        TextField nameField = createStyledTextField("Name");
        TextField usernameField = createStyledTextField("Username");
        PasswordField passwordField = createStyledPasswordField("Password");
        TextField urlField = createStyledTextField("URL");
        TextField notesField = createStyledTextField("Notes");
        notesField.setPrefHeight(80);

        Button addButton = createStyledButton("Add Login");
        
        addButton.setOnAction(e -> addLogin(nameField, usernameField, passwordField, urlField, notesField));
        
        form.getChildren().addAll(title, nameField, usernameField, passwordField, urlField, notesField, addButton);
        return form;
    }

    private VBox buildDetailsForm(LoginEntry login) {
        VBox form = new VBox(15);
        form.setPadding(new Insets(10));

        Label title = new Label(login.getName());
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setStyle("-fx-text-fill: " + currentTextColor + ";");

        // --- Name Row (no copy button) ---
        TextField nameDisplay = createStyledTextField("Name");
        nameDisplay.setText(login.getName());
        nameDisplay.setEditable(false);

        // --- Username Row (with copy button) ---
        TextField userDisplay = createStyledTextField("Username");
        userDisplay.setText(login.getUsername());
        userDisplay.setEditable(false);
        
        Button copyUserButton = createCopyButton();
        copyUserButton.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(login.getUsername());
            clipboard.setContent(content);
            statusLabel.setText("Username copied to clipboard.");
            statusLabel.setStyle("-fx-text-fill: " + currentSuccessColor + ";");
        });
        
        // Use HBox to place text field and button on one line
        HBox userBox = new HBox(10, userDisplay, copyUserButton);
        userBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(userDisplay, Priority.ALWAYS); // Make text field fill space

        // --- Password Row (with copy button) ---
        TextField passDisplay = createStyledTextField("Password");
        passDisplay.setText("************");
        passDisplay.setEditable(false);
        
        Button copyPassButton = createCopyButton();
        copyPassButton.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(login.getPassword()); // Get the REAL password
            clipboard.setContent(content);
            statusLabel.setText("Password copied to clipboard.");
            statusLabel.setStyle("-fx-text-fill: " + currentSuccessColor + ";");
        });
        
        // Use HBox for password row
        HBox passBox = new HBox(10, passDisplay, copyPassButton);
        passBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(passDisplay, Priority.ALWAYS); // Make text field fill space

        // --- Other Fields ---
        TextField urlDisplay = createStyledTextField("URL");
        urlDisplay.setText(login.getUrl());
        urlDisplay.setEditable(false);

        TextArea notesDisplay = new TextArea(login.getNotes());
        notesDisplay.setPromptText("Notes");
        styleControl(notesDisplay);
        notesDisplay.setEditable(false);
        notesDisplay.setWrapText(true);
        notesDisplay.setPrefHeight(80);

        // --- Button Bar ---
        Button deleteButton = createStyledButton("Delete");
        deleteButton.setStyle(deleteButton.getStyle() + "-fx-text-fill: " + currentErrorColor + ";");
        deleteButton.setOnAction(e -> deleteLogin(login));

        Button newLoginButton = createStyledButton("Add New Login");
        newLoginButton.setOnAction(e -> {
            loginListView.getSelectionModel().clearSelection();
        });
        
        HBox buttonBar = new HBox(10, deleteButton, newLoginButton);
        
        // Add all controls to the form
        form.getChildren().addAll(title, nameDisplay, userBox, passBox, urlDisplay, notesDisplay, buttonBar);
        return form;
    }
    
    private void loadDataFromDatabase() {
        loginData.clear();
        try {
            ResultSet rs = database.searchLogins("");
            while (rs != null && rs.next()) {
                
                String encryptedPass = rs.getString("password");
                String plainTextPass;
                try {
                    plainTextPass = Encryption.decryptWithIV(encryptedPass, MasterPassword.getKey());
                } catch (Exception e) {
                    e.printStackTrace();
                    plainTextPass = "!!DECRYPT_ERROR!!";
                }

                loginData.add(new LoginEntry(
                        rs.getString("name"),
                        rs.getString("username"),
                        plainTextPass,
                        rs.getString("url"),
                        rs.getString("notes")
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
            statusLabel.setStyle("-fx-text-fill: " + currentErrorColor + ";");
            return;
        }

        String encryptedPass;
        try {
            encryptedPass = Encryption.encryptWithIV(password, MasterPassword.getKey());
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Failed to encrypt password.");
            statusLabel.setStyle("-fx-text-fill: " + currentErrorColor + ";");
            return;
        }

        boolean success = database.createLogin(name, username, encryptedPass, url, notes);
        
        if (success) {
            statusLabel.setText("Login added successfully.");
            statusLabel.setStyle("-fx-text-fill: " + currentSuccessColor + ";");
            
            LoginEntry newLogin = new LoginEntry(name, username, password, url, notes); 
            loginData.add(newLogin);
            loginListView.getSelectionModel().select(newLogin);
            
            nameField.clear();
            usernameField.clear();
            passwordField.clear();
            urlField.clear();
            notesField.clear();
        } else {
            statusLabel.setText("Failed to add login.");
            statusLabel.setStyle("-fx-text-fill: " + currentErrorColor + ";");
        }
    }
    
    private void deleteLogin(LoginEntry login) {
        boolean success = database.deleteLogin(login.getName(), login.getUsername());
        if (success) {
            statusLabel.setText("Login deleted successfully.");
            statusLabel.setStyle("-fx-text-fill: " + currentSuccessColor + ";");
            loginData.remove(login);
            loginListView.getSelectionModel().clearSelection();
        } else {
            statusLabel.setText("Failed to delete login.");
            statusLabel.setStyle("-fx-text-fill: " + currentErrorColor + ";");
        }
    }

    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        styleControl(field);
        return field;
    }

    private PasswordField createStyledPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        styleControl(field);
        return field;
    }
    
    private Button createStyledButton(String text) {
        Button button = new Button(text);
        String buttonStyle = "-fx-background-color: " + currentBaseColor + "; -fx-text-fill: " + currentTextColor + "; -fx-background-radius: 10;";
        button.setStyle(buttonStyle);
        button.setEffect(lightOuterShadow);
        button.setPrefHeight(35);

        button.setOnMousePressed(e -> {
            button.setStyle(buttonStyle + "-fx-background-color: " + currentControlInnerBase + ";");
            button.setEffect(lightInnerShadow);
        });
        button.setOnMouseReleased(e -> {
            button.setStyle(buttonStyle);
            button.setEffect(lightOuterShadow);
        });
        return button;
    }

    private void styleControl(TextInputControl control) {
        InnerShadow darkInnerShadow = new InnerShadow(5, 1, 1, Color.web(currentDarkShadowColor));
        darkInnerShadow.setOffsetX(2);
        darkInnerShadow.setOffsetY(2);

        InnerShadow lightInnerShadow = new InnerShadow(5, 1, 1, Color.web(currentLightShadowColor));
        lightInnerShadow.setOffsetX(-2);
        lightInnerShadow.setOffsetY(-2);
        lightInnerShadow.setInput(darkInnerShadow);

        control.setStyle(
            "-fx-background-color: " + currentControlInnerBase + ";" +
            "-fx-background-radius: 10;" +
            "-fx-text-fill: " + currentTextColor + ";"
        );
        control.setEffect(lightInnerShadow);
        if (control instanceof TextField) {
            control.setPrefHeight(35);
        }

        control.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                InnerShadow focusedShadow = new InnerShadow(8, 0, 0, Color.web(currentAccentColor, 0.6));
                focusedShadow.setInput(lightInnerShadow);
                control.setEffect(focusedShadow);
            } else {
                control.setEffect(lightInnerShadow);
            }
        });
    }

    private void styleWindowButton(Button button, boolean isCloseButton) {
        String baseStyle = "-fx-background-color: transparent; -fx-text-fill: " + currentMutedTextColor + "; -fx-font-weight: bold; -fx-font-size: 14; -fx-background-radius: 5;";
        
        String hoverBgColor = isCloseButton ? currentErrorColor : currentControlInnerBase;
        String hoverTxtColor = isCloseButton ? currentLightShadowColor : currentTextColor;
        String hoverStyle = "-fx-background-color: " + hoverBgColor + "; -fx-text-fill: " + hoverTxtColor + ";";
        
        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(baseStyle + hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
    }

    private void styleThemeToggle(ToggleButton toggle) {
        if (isDarkMode) {
            themeIcon.setIconCode(MaterialDesign.MDI_WEATHER_SUNNY);
        } else {
            themeIcon.setIconCode(MaterialDesign.MDI_WEATHER_NIGHT);
        }
        themeIcon.setIconColor(Color.web(currentTextColor));
        themeIcon.setIconSize(16);

        String style = "-fx-background-color: " + currentBaseColor + "; " +
                       "-fx-background-radius: 10; " +
                       "-fx-background-insets: 0;";
                       
        toggle.setStyle(style);
        
        if (toggle.isSelected()) {
            toggle.setEffect(lightInnerShadow);
        } else {
            toggle.setEffect(lightOuterShadow);
        }
    }
    
    class LoginListCell extends ListCell<LoginEntry> {
        private VBox content;
        private Label nameLabel;
        private Label usernameLabel;
        
        public LoginListCell() {
            super();
            nameLabel = new Label();
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            
            usernameLabel = new Label();

            content = new VBox(5, nameLabel, usernameLabel);
            content.setPadding(new Insets(10));
            content.setAlignment(Pos.CENTER_LEFT);
            
            setStyle("-fx-background-color: transparent; -fx-padding: 5 10 5 10;"); 
        }

        @Override
        protected void updateItem(LoginEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                nameLabel.setText(item.getName());
                nameLabel.setStyle("-fx-text-fill: " + currentTextColor + ";");
                
                usernameLabel.setText(item.getUsername());
                usernameLabel.setStyle("-fx-text-fill: " + currentMutedTextColor + ";");
                
                String style = "-fx-background-color: " + currentBaseColor + "; -fx-background-radius: 10;";
                content.setStyle(style);
                
                if (isSelected()) {
                    content.setEffect(lightInnerShadow); 
                } else {
                    content.setEffect(lightOuterShadow);
                }
                
                setGraphic(content);
            }
        }
    }
    
    private Button createCopyButton() {
        Button copyButton = new Button();
        FontIcon copyIcon = new FontIcon(MaterialDesign.MDI_CONTENT_COPY);
        copyIcon.setIconSize(16);
        copyIcon.setIconColor(Color.web(currentMutedTextColor));
        copyButton.setGraphic(copyIcon);

        // Style to match text fields
        String style = "-fx-background-color: " + currentBaseColor + "; " +
                       "-fx-background-radius: 10; " +
                       "-fx-background-insets: 0;";
        copyButton.setStyle(style);
        copyButton.setEffect(lightOuterShadow);
        
        // Set fixed size to match text field height
        copyButton.setPrefSize(35, 35);
        copyButton.setMinSize(35, 35);

        copyButton.setOnMousePressed(e -> {
            copyButton.setStyle(style + "-fx-background-color: " + currentControlInnerBase + ";");
            copyButton.setEffect(lightInnerShadow);
        });
        copyButton.setOnMouseReleased(e -> {
            copyButton.setStyle(style);
            copyButton.setEffect(lightOuterShadow);
        });

        return copyButton;
    }

    public static class LoginEntry {
        private final String name;
        private final String username;
        private final String password;
        private final String url;
        private final String notes;

        public LoginEntry(String name, String username, String password, String url, String notes) {
            this.name = name;
            this.username = username;
            this.password = password;
            this.url = url;
            this.notes = notes;
        }

        public String getName() { return name; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getUrl() { return url; }
        public String getNotes() { return notes; }
    }
}