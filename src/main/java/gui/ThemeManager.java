package gui;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox; 
import javafx.scene.control.ListCell; // <-- Import is already here, just was an error in the code
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea; 
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Alert; 
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle; 
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class ThemeManager {

    private boolean isDarkMode;
    
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

    private static final String DB_DIR_PATH = System.getProperty("user.home") + "/Documents/RapidCipher";
    private static final Path SETTINGS_FILE = Paths.get(DB_DIR_PATH, "settings.properties");

    public ThemeManager() {
        this.isDarkMode = isSystemDarkMode();
        updateThemeStyles();
    }

    private boolean isSystemDarkMode() {
        String os = System.getProperty("os.name").toLowerCase();
        String command;
        boolean isDark = false;

        try {
            if (os.contains("win")) {
                command = "reg query \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize\" /v AppsUseLightTheme";
                Process process = Runtime.getRuntime().exec(command);
                String result = new BufferedReader(new InputStreamReader(process.getInputStream()))
                        .lines().filter(line -> line.contains("AppsUseLightTheme")).findFirst().orElse("");
                
                process.waitFor(1, TimeUnit.SECONDS);
                if (result.contains("0x0")) isDark = true;

            } else if (os.contains("mac")) {
                command = "defaults read -g AppleInterfaceStyle";
                Process process = Runtime.getRuntime().exec(command);
                String result = new BufferedReader(new InputStreamReader(process.getInputStream()))
                        .lines().findFirst().orElse("");
                
                process.waitFor(1, TimeUnit.SECONDS);
                if (result.trim().equals("Dark")) isDark = true;

            } else if (os.contains("nix") || os.contains("nux")) {
                command = "gsettings get org.gnome.desktop.interface color-scheme";
                Process process = Runtime.getRuntime().exec(command);
                String result = new BufferedReader(new InputStreamReader(process.getInputStream()))
                        .lines().findFirst().orElse("");
                
                process.waitFor(1, TimeUnit.SECONDS);
                if (result.contains("prefer-dark")) isDark = true;
            }
        } catch (Exception e) {
            System.err.println("Could not detect system theme: " + e.getMessage());
        }
        
        return isDark;
    }

    public void updateThemeStyles() {
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

    public void loadThemePreference() {
        if (!Files.exists(SETTINGS_FILE)) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(SETTINGS_FILE)) {
            props.load(in);
            this.isDarkMode = Boolean.parseBoolean(props.getProperty("isDarkMode", String.valueOf(this.isDarkMode)));
        } catch (Exception e) {
            System.err.println("Failed to load theme preference: " + e.getMessage());
        }
        updateThemeStyles();
    }

    public void saveThemePreference() {
        Properties props = new Properties();
        props.setProperty("isDarkMode", String.valueOf(this.isDarkMode));
        try (OutputStream out = Files.newOutputStream(SETTINGS_FILE)) {
            props.store(out, "RapidCipher User Preferences");
        } catch (Exception e) {
            System.err.println("Failed to save theme preference: " + e.getMessage());
        }
    }
    
    // --- Styling Helpers ---

    public TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        styleControl(field);
        return field;
    }

    public PasswordField createStyledPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        styleControl(field);
        return field;
    }
    
    public Button createStyledButton(String text) {
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

    public void styleControl(TextInputControl control) {
        InnerShadow darkInnerShadow = new InnerShadow(5, 1, 1, Color.web(currentDarkShadowColor));
        darkInnerShadow.setOffsetX(2);
        darkInnerShadow.setOffsetY(2);

        InnerShadow lightInnerShadow = new InnerShadow(5, 1, 1, Color.web(currentLightShadowColor));
        lightInnerShadow.setOffsetX(-2);
        lightInnerShadow.setOffsetY(-2);
        lightInnerShadow.setInput(darkInnerShadow);

        String baseStyle = "-fx-background-color: " + currentControlInnerBase + ";" +
                         "-fx-background-radius: 10;" +
                         "-fx-text-fill: " + currentTextColor + ";";

        if (control instanceof TextArea) {
            baseStyle += "-fx-control-inner-background: " + currentControlInnerBase + ";";
        }

        control.setStyle(baseStyle);
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

    public void styleWindowButton(Button button, boolean isCloseButton) {
        String baseStyle = "-fx-background-color: transparent; -fx-text-fill: " + currentMutedTextColor + "; -fx-font-weight: bold; -fx-font-size: 14; -fx-background-radius: 5;";
        
        String hoverBgColor = isCloseButton ? currentErrorColor : currentControlInnerBase;
        String hoverTxtColor = isCloseButton ? currentLightShadowColor : currentTextColor;
        String hoverStyle = "-fx-background-color: " + hoverBgColor + "; -fx-text-fill: " + hoverTxtColor + ";";
        
        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(baseStyle + hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
    }

    public void styleThemeToggle(ToggleButton toggle, FontIcon icon) {
        if (isDarkMode) {
            icon.setIconCode(MaterialDesign.MDI_WEATHER_SUNNY);
        } else {
            icon.setIconCode(MaterialDesign.MDI_WEATHER_NIGHT);
        }
        icon.setIconColor(Color.web(currentTextColor)); // Use text color for visibility
        icon.setIconSize(16);

        String style = "-fx-background-color: " + currentBaseColor + "; " +
                       "-fx-background-radius: 10; " +
                       "-fx-background-insets: 0;";
                       
        toggle.setStyle(style);
        
        toggle.setPrefSize(35, 35);
        toggle.setMinSize(35, 35);
        
        // Re-set listeners to capture new theme colors
        toggle.setOnMousePressed(e -> {
            toggle.setStyle(style + "-fx-background-color: " + currentControlInnerBase + ";");
            toggle.setEffect(lightInnerShadow);
        });
        toggle.setOnMouseReleased(e -> {
            toggle.setStyle(style);
            toggle.setEffect(toggle.isSelected() ? lightInnerShadow : lightOuterShadow);
        });

        // Set current effect based on selection
        toggle.setEffect(toggle.isSelected() ? lightInnerShadow : lightOuterShadow);
    }
    
    public <T> void styleComboBox(ComboBox<T> combo) {
        // This styling is basic, but ensures it doesn't look completely out of place.
        String style = "-fx-background-color: " + currentControlInnerBase + "; " +
                       "-fx-background-radius: 10; " +
                       "-fx-text-fill: " + currentTextColor + "; " +
                       "-fx-border-width: 0;";
        
        combo.setStyle(style);
        combo.setEffect(lightInnerShadow);
        
        combo.setCellFactory(lv -> new ListCell<T>() { // <-- FIX 1: Use generic type T
            @Override
            protected void updateItem(T item, boolean empty) { // <-- FIX 2: Use generic type T
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: " + currentBaseColor);
                } else {
                    setText(item.toString());
                    setStyle("-fx-background-color: " + currentBaseColor + "; -fx-text-fill: " + currentTextColor + ";");
                }
            }
        });
    }
    
    // --- NEW PUBLIC STYLING METHOD ---
    public void styleIconButton(Button button, MaterialDesign iconCode) {
        FontIcon icon;
        // Ensure button has a FontIcon graphic
        if (button.getGraphic() instanceof FontIcon) {
            icon = (FontIcon) button.getGraphic();
        } else {
            icon = new FontIcon();
            button.setGraphic(icon);
        }
        
        icon.setIconCode(iconCode);
        icon.setIconSize(16);
        icon.setIconColor(Color.web(currentMutedTextColor)); // Use current theme color

        String style = "-fx-background-color: " + currentBaseColor + "; " + // Use current theme color
                       "-fx-background-radius: 10; " +
                       "-fx-background-insets: 0;";
        button.setStyle(style);
        button.setEffect(lightOuterShadow); // Use current theme effect
        
        button.setPrefSize(35, 35);
        button.setMinSize(35, 35);

        // Re-set listeners to capture new theme colors
        button.setOnMousePressed(e -> {
            button.setStyle(style + "-fx-background-color: " + currentControlInnerBase + ";"); // Use current theme color
            button.setEffect(lightInnerShadow); // Use current theme effect
        });
        button.setOnMouseReleased(e -> {
            button.setStyle(style);
            button.setEffect(lightOuterShadow); // Use current theme effect
        });
    }

    // --- UPDATED Create Methods to use the Styler ---

    public Button createCopyButton() {
        Button button = new Button();
        styleIconButton(button, MaterialDesign.MDI_CONTENT_COPY);
        return button;
    }
    
    public Button createNewLoginButton() {
        Button button = new Button();
        styleIconButton(button, MaterialDesign.MDI_PLUS);
        return button;
    }
    
    public Button createSettingsButton() {
        Button button = new Button();
        styleIconButton(button, MaterialDesign.MDI_SETTINGS);
        return button;
    }
    
    public Button createLogoutButton() {
        Button button = new Button();
        styleIconButton(button, MaterialDesign.MDI_LOGOUT);
        return button;
    }
    
    public ToggleButton createShowHideButton() {
        ToggleButton showHideButton = new ToggleButton();
        FontIcon eyeIcon = new FontIcon(MaterialDesign.MDI_EYE);
        eyeIcon.setIconSize(16);
        eyeIcon.setIconColor(Color.web(currentMutedTextColor)); // Set initial color
        showHideButton.setGraphic(eyeIcon);

        String style = "-fx-background-color: " + currentBaseColor + "; " +
                       "-fx-background-radius: 10; " +
                       "-fx-background-insets: 0;";
        showHideButton.setStyle(style);
        showHideButton.setEffect(lightOuterShadow);
        
        showHideButton.setPrefSize(35, 35);
        showHideButton.setMinSize(35, 35);
        
        eyeIcon.setIconColor(Color.web(currentMutedTextColor));

        showHideButton.setOnMousePressed(e -> {
            showHideButton.setStyle(style + "-fx-background-color: " + currentControlInnerBase + ";");
            showHideButton.setEffect(lightInnerShadow);
        });
        showHideButton.setOnMouseReleased(e -> {
            showHideButton.setStyle(style);
            showHideButton.setEffect(lightOuterShadow);
        });

        // Add listener to change icon
        showHideButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                eyeIcon.setIconCode(MaterialDesign.MDI_EYE_OFF);
            } else {
                eyeIcon.setIconCode(MaterialDesign.MDI_EYE);
            }
        });

        return showHideButton;
    }
    
    public void showErrorAlert(String title, String content) {
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
    
    // --- Getters ---
    public boolean isDarkMode() { return isDarkMode; }
    public void setDarkMode(boolean isDarkMode) { this.isDarkMode = isDarkMode; }
    public String getCurrentBaseColor() { return currentBaseColor; }
    public String getCurrentBaseSemiTransparent() { return currentBaseSemiTransparent; }
    public String getCurrentTextColor() { return currentTextColor; }
    public String getCurrentMutedTextColor() { return currentMutedTextColor; }
    public String getCurrentErrorColor() { return currentErrorColor; }
    public String getCurrentSuccessColor() { return currentSuccessColor; }
    public DropShadow getLightOuterShadow() { return lightOuterShadow; }
    public InnerShadow getLightInnerShadow() { return lightInnerShadow; }
}