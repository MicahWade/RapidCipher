import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle; // Import for transparent stage

import java.sql.ResultSet;
import java.sql.SQLException;

public class Gui extends Application {
    private static Gui instance;
    private Database database;
    private ObservableList<LoginEntry> loginData;

    // --- UI Components ---
    private ListView<LoginEntry> loginListView;
    private VBox detailsPane; // The right-hand pane
    private VBox addForm; // The form for adding new logins
    private VBox currentDetailsForm; // The form for showing details
    private Label statusLabel;
    private StackPane root;
    private VBox mainLayout;
    private SplitPane splitPane;
    private HBox topBar;
    private ToggleButton themeToggle;
    private Label titleLabel;
    private Button minimizeButton;
    private Button closeButton;

    // --- Theme State ---
    private boolean isDarkMode = false;
    
    // --- Theme Variables (will be updated) ---
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

    // --- Theme Effects (will be updated) ---
    private DropShadow lightOuterShadow;
    private InnerShadow lightInnerShadow;
    
    // --- Window Drag ---
    private double xOffset = 0;
    private double yOffset = 0;

    public Gui() {
        instance = this;
        try {
            database = Database.getInstance();
            loginData = FXCollections.observableArrayList();
            loadDataFromDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static synchronized Gui getInstance() {
        return instance;
    }

    /**
     * Sets the current theme variables and re-creates effects based on the isDarkMode flag.
     */
    private void updateThemeStyles() {
        if (isDarkMode) {
            currentBaseColor = "#383e46"; // Dark grey
            currentBaseSemiTransparent = "rgba(56, 62, 70, 0.85)"; // Semi-transparent dark
            currentDarkShadowColor = "#2a2e34"; // Darker shadow
            currentLightShadowColor = "#464e58"; // Lighter shadow
            currentControlInnerBase = "#32383e"; // Slightly lighter than base
            currentTextColor = "#e0e5ec"; // Light text
            currentMutedTextColor = "#a3b1c6"; // Muted light text
        } else {
            currentBaseColor = "#e0e5ec"; // Light, off-white
            currentBaseSemiTransparent = "rgba(224, 229, 236, 0.85)"; // Semi-transparent light
            currentDarkShadowColor = "#a3b1c6"; // Darker shadow
            currentLightShadowColor = "#ffffff"; // Lighter shadow
            currentControlInnerBase = "#E3E9F0"; // Slightly different for controls
            currentTextColor = "#333333"; // Dark text
            currentMutedTextColor = "#555555"; // Muted dark text
        }

        // --- Re-create effects with new colors ---
        DropShadow darkOuterShadow = new DropShadow(10, 5, 5, Color.web(currentDarkShadowColor));
        darkOuterShadow.setOffsetX(5);
        darkOuterShadow.setOffsetY(5);

        lightOuterShadow = new DropShadow(10, 5, 5, Color.web(currentLightShadowColor));
        lightOuterShadow.setOffsetX(-5);
        lightOuterShadow.setOffsetY(-5);
        lightOuterShadow.setInput(darkOuterShadow); // Chain shadows
        
        InnerShadow darkInnerShadow = new InnerShadow(10, 2, 2, Color.web(currentDarkShadowColor));
        darkInnerShadow.setOffsetX(2);
        darkInnerShadow.setOffsetY(2);

        lightInnerShadow = new InnerShadow(10, 2, 2, Color.web(currentLightShadowColor));
        lightInnerShadow.setOffsetX(-2);
        lightInnerShadow.setOffsetY(-2);
        lightInnerShadow.setInput(darkInnerShadow); // Chain shadows
    }

    /**
     * Re-applies all styles to all components. Called on theme toggle.
     */
    private void updateAllStyles() {
        // 1. Update all color and effect variables
        updateThemeStyles();

        // 2. Re-style all static containers
        root.setStyle("-fx-background-color: " + currentBaseSemiTransparent + "; -fx-background-radius: 20;");
        mainLayout.setStyle("-fx-background-color: " + currentBaseColor + "; -fx-background-radius: 15;");
        mainLayout.setEffect(lightOuterShadow);
        
        splitPane.setStyle("-fx-background-color: " + currentBaseColor + ";");
        loginListView.setStyle("-fx-background-color: " + currentBaseColor + ";");
        detailsPane.setStyle("-fx-background-color: " + currentBaseColor + ";");
        
        statusLabel.setStyle("-fx-text-fill: " + currentMutedTextColor + ";");
        
        // 3. Style TopBar components
        titleLabel.setStyle("-fx-text-fill: " + currentMutedTextColor + ";");
        styleThemeToggle(themeToggle);
        styleWindowButton(minimizeButton, false);
        styleWindowButton(closeButton, true);

        // 4. Re-style dynamic components (by rebuilding them)
        if (loginListView.getSelectionModel().getSelectedItem() == null) {
            addForm = buildAddForm(); // Re-build form with new styles
            detailsPane.getChildren().setAll(addForm);
        } else {
            currentDetailsForm = buildDetailsForm(loginListView.getSelectionModel().getSelectedItem()); // Re-build form
            detailsPane.getChildren().setAll(currentDetailsForm);
        }

        // 5. Force the ListView to redraw its cells
        loginListView.refresh();
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Password Manager");
        // Make the stage transparent
        primaryStage.initStyle(StageStyle.TRANSPARENT);

        // Initialize the theme colors *before* building any components that use them.
        updateThemeStyles();

        // --- Left Pane: Login List ---
        loginListView = new ListView<>();
        loginListView.setItems(loginData);
        loginListView.setCellFactory(lv -> new LoginListCell());
        loginListView.setPrefWidth(280);
        loginListView.setMinWidth(200);

        // --- Right Pane: Details View ---
        detailsPane = new VBox(15);
        detailsPane.setPadding(new Insets(20));
        
        addForm = buildAddForm();
        detailsPane.getChildren().add(addForm);

        // --- Selection Listener ---
        loginListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                currentDetailsForm = buildDetailsForm(newSelection);
                detailsPane.getChildren().setAll(currentDetailsForm);
            } else {
                detailsPane.getChildren().setAll(addForm);
            }
        });

        // --- Top Bar (Title + Theme Toggle + Window Controls) ---
        titleLabel = new Label("RapidCipher");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        themeToggle = new ToggleButton("🌙");
        themeToggle.setOnAction(e -> {
            isDarkMode = themeToggle.isSelected();
            updateAllStyles(); // This will handle the text and style change
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

        // --- Add window dragging ---
        topBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        
        topBar.setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() - xOffset);
            primaryStage.setY(event.getScreenY() - yOffset);
        });

        // --- Main Layout: SplitPane ---
        splitPane = new SplitPane(loginListView, detailsPane);
        splitPane.setDividerPositions(0.35);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // --- Status Label ---
        statusLabel = new Label("Welcome to RapidCipher!");
        statusLabel.setPadding(new Insets(0, 0, 0, 10));

        // --- Main Layout Container (VBox) ---
        mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(10));
        mainLayout.getChildren().addAll(topBar, splitPane, statusLabel);
        mainLayout.setMaxWidth(1000);
        mainLayout.setMaxHeight(700);

        // --- Root Pane: StackPane (for centering and transparency) ---
        root = new StackPane(mainLayout);
        root.setPadding(new Insets(20));
        
        // --- Scene Setup ---
        Scene scene = new Scene(root, 1000, 700);
        scene.setFill(Color.TRANSPARENT); // Make scene background transparent
                
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        
        // --- Initial Style Update ---
        updateAllStyles(); // Apply initial theme
        
        primaryStage.show();
    }

    /**
     * Builds the VBox containing the "Add Login" form using current theme.
     */
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

    /**
     * Builds the VBox containing the details of a selected login using current theme.
     */
    private VBox buildDetailsForm(LoginEntry login) {
        VBox form = new VBox(15);
        form.setPadding(new Insets(10));

        Label title = new Label(login.getName());
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setStyle("-fx-text-fill: " + currentTextColor + ";");

        TextField nameDisplay = createStyledTextField("Name");
        nameDisplay.setText(login.getName());
        nameDisplay.setEditable(false);

        TextField userDisplay = createStyledTextField("Username");
        userDisplay.setText(login.getUsername());
        userDisplay.setEditable(false);
        
        TextField passDisplay = createStyledTextField("Password");
        passDisplay.setText("************");
        passDisplay.setEditable(false);

        TextField urlDisplay = createStyledTextField("URL");
        urlDisplay.setText(login.getUrl());
        urlDisplay.setEditable(false);

        TextArea notesDisplay = new TextArea(login.getNotes());
        notesDisplay.setPromptText("Notes");
        styleControl(notesDisplay); // Use common styling
        notesDisplay.setEditable(false);
        notesDisplay.setWrapText(true);
        notesDisplay.setPrefHeight(80);

        Button deleteButton = createStyledButton("Delete");
        deleteButton.setStyle(deleteButton.getStyle() + "-fx-text-fill: " + currentErrorColor + ";");
        deleteButton.setOnAction(e -> deleteLogin(login));

        Button newLoginButton = createStyledButton("Add New Login");
        newLoginButton.setOnAction(e -> {
            loginListView.getSelectionModel().clearSelection(); // Triggers listener
        });
        
        HBox buttonBar = new HBox(10, deleteButton, newLoginButton);
        form.getChildren().addAll(title, nameDisplay, userDisplay, passDisplay, urlDisplay, notesDisplay, buttonBar);
        return form;
    }

    // --- Data Logic ---

    private void loadDataFromDatabase() {
        loginData.clear();
        try {
            ResultSet rs = database.searchLogins("");
            while (rs != null && rs.next()) {
                loginData.add(new LoginEntry(
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("password"),
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

        boolean success = database.createLogin(name, username, password, url, notes);
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
            loginListView.getSelectionModel().clearSelection(); // Show add form
        } else {
            statusLabel.setText("Failed to delete login.");
            statusLabel.setStyle("-fx-text-fill: " + currentErrorColor + ";");
        }
    }

    // --- Helper Methods for Styling (Now Theme-Aware) ---

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
        button.setEffect(lightOuterShadow); // Raised look
        button.setPrefHeight(35);

        button.setOnMousePressed(e -> {
            button.setStyle(buttonStyle + "-fx-background-color: " + currentControlInnerBase + ";");
            button.setEffect(lightInnerShadow); // Pressed look
        });
        button.setOnMouseReleased(e -> {
            button.setStyle(buttonStyle);
            button.setEffect(lightOuterShadow); // Back to raised
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

    /**
     * Applies theme-aware style to the window buttons (min, close).
     */
    private void styleWindowButton(Button button, boolean isCloseButton) {
        String baseStyle = "-fx-background-color: transparent; -fx-text-fill: " + currentMutedTextColor + "; -fx-font-weight: bold; -fx-font-size: 14; -fx-background-radius: 5;";
        
        String hoverBgColor = isCloseButton ? currentErrorColor : currentControlInnerBase;
        String hoverTxtColor = isCloseButton ? currentLightShadowColor : currentTextColor;
        String hoverStyle = "-fx-background-color: " + hoverBgColor + "; -fx-text-fill: " + hoverTxtColor + ";";
        
        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(baseStyle + hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
    }

    /**
     * Applies theme-aware Neumorphic style to the toggle button.
     */
    private void styleThemeToggle(ToggleButton toggle) {
        toggle.setText(isDarkMode ? "☀️" : "🌙");
        toggle.setFont(Font.font(16));
        
        // --- *** This is the complete fix *** ---
        // We set all properties here in the Java code.
        // This string includes the background color, radius, text color,
        // and the -fx-background-insets which prevents the crash.
        String style = "-fx-background-color: " + currentBaseColor + "; " +
                       "-fx-background-radius: 10; " +
                       "-fx-text-fill: " + currentTextColor + "; " +
                       "-fx-background-insets: 0;"; // This line fixes the error
        // --- *** END FIX *** ---
                       
        toggle.setStyle(style);
        
        if (toggle.isSelected()) {
            toggle.setEffect(lightInnerShadow);
        } else {
            toggle.setEffect(lightOuterShadow);
        }
    }
    // --- Inner Class for ListView Cell (Now Theme-Aware) ---
    
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


    // --- Helper class to represent login data ---
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