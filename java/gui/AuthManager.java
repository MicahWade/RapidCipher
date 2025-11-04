package gui;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.geometry.Pos;
import javafx.stage.StageStyle;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javafx.scene.paint.Color;

import core.Encryption;
import core.MasterPassword;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;

public class AuthManager {

    private ThemeManager themeManager;
    
    private static final String DB_DIR_PATH = System.getProperty("user.home") + "/Documents/RapidCipher";
    private static final Path SALT_FILE = Paths.get(DB_DIR_PATH, "salt.bin");
    private static final Path KEY_CHECK_FILE = Paths.get(DB_DIR_PATH, "key_check.bin");
    private static final String KEY_CHECK_STRING = "RapidCipher-OK";

    public AuthManager(ThemeManager themeManager) {
        this.themeManager = themeManager;
    }

    public boolean showMasterPasswordPrompt() throws Exception {
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
        dialog.getDialogPane().getScene().setFill(Color.TRANSPARENT);
        dialog.getDialogPane().setStyle("-fx-background-color: " + themeManager.getCurrentBaseSemiTransparent() + "; -fx-background-radius: 15;");
        dialog.getDialogPane().setEffect(themeManager.getLightOuterShadow());

        dialog.setTitle("Login");
        dialog.setHeaderText("Enter your master password for RapidCipher.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        Label label = new Label("Password:");
        label.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        
        PasswordField pwd = themeManager.createStyledPasswordField("");
        TextField visiblePwd = themeManager.createStyledTextField("");
        visiblePwd.setPromptText("Password");
        visiblePwd.textProperty().bindBidirectional(pwd.textProperty());
        visiblePwd.setVisible(false);
        
        StackPane passStack = new StackPane(pwd, visiblePwd);
        StackPane.setAlignment(visiblePwd, Pos.CENTER_LEFT);
        StackPane.setAlignment(pwd, Pos.CENTER_LEFT);

        ToggleButton showHideButton = themeManager.createShowHideButton();
        showHideButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            visiblePwd.setVisible(newVal);
            pwd.setVisible(!newVal);
        });
        
        HBox passBox = new HBox(10, passStack, showHideButton);
        HBox.setHgrow(passStack, Priority.ALWAYS);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(label, 0, 0);
        grid.add(passBox, 1, 0);
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
        dialog.getDialogPane().getScene().setFill(Color.TRANSPARENT);
        dialog.getDialogPane().setStyle("-fx-background-color: " + themeManager.getCurrentBaseSemiTransparent() + "; -fx-background-radius: 15;");
        dialog.getDialogPane().setEffect(themeManager.getLightOuterShadow());
        
        dialog.setTitle("Welcome to RapidCipher");
        dialog.setHeaderText("Please create a new master password.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Label label1 = new Label("Password:");
        label1.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        Label label2 = new Label("Confirm:");
        label2.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");

        PasswordField pwd1 = themeManager.createStyledPasswordField("");
        TextField visiblePwd1 = themeManager.createStyledTextField("");
        visiblePwd1.setPromptText("Password");
        visiblePwd1.textProperty().bindBidirectional(pwd1.textProperty());
        visiblePwd1.setVisible(false);
        StackPane passStack1 = new StackPane(pwd1, visiblePwd1);
        StackPane.setAlignment(visiblePwd1, Pos.CENTER_LEFT);
        StackPane.setAlignment(pwd1, Pos.CENTER_LEFT);
        ToggleButton showHideButton1 = themeManager.createShowHideButton();
        showHideButton1.selectedProperty().addListener((obs, oldVal, newVal) -> {
            visiblePwd1.setVisible(newVal);
            pwd1.setVisible(!newVal);
        });
        HBox passBox1 = new HBox(10, passStack1, showHideButton1);
        HBox.setHgrow(passStack1, Priority.ALWAYS);

        PasswordField pwd2 = themeManager.createStyledPasswordField("");
        TextField visiblePwd2 = themeManager.createStyledTextField("");
        visiblePwd2.setPromptText("Confirm Password");
        visiblePwd2.textProperty().bindBidirectional(pwd2.textProperty());
        visiblePwd2.setVisible(false);
        StackPane passStack2 = new StackPane(pwd2, visiblePwd2);
        StackPane.setAlignment(visiblePwd2, Pos.CENTER_LEFT);
        StackPane.setAlignment(pwd2, Pos.CENTER_LEFT);
        ToggleButton showHideButton2 = themeManager.createShowHideButton();
        showHideButton2.selectedProperty().addListener((obs, oldVal, newVal) -> {
            visiblePwd2.setVisible(newVal);
            pwd2.setVisible(!newVal);
        });
        HBox passBox2 = new HBox(10, passStack2, showHideButton2);
        HBox.setHgrow(passStack2, Priority.ALWAYS);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(label1, 0, 0);
        grid.add(passBox1, 1, 0);
        grid.add(label2, 0, 1);
        grid.add(passBox2, 1, 1);
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
        alert.getDialogPane().setStyle("-fx-background-color: " + themeManager.getCurrentBaseColor() + "; -fx-background-radius: 15;");
        alert.getDialogPane().setEffect(themeManager.getLightOuterShadow());
        
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        alert.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        
        alert.showAndWait();
    }
}