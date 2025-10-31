package gui;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.StageStyle;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

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
        dialog.getDialogPane().setStyle("-fx-background-color: " + themeManager.getCurrentBaseSemiTransparent() + "; -fx-background-radius: 15;");
        dialog.getDialogPane().setEffect(themeManager.getLightOuterShadow());

        dialog.setTitle("Login");
        dialog.setHeaderText("Enter your master password for RapidCipher.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        Label label = new Label("Password:");
        label.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        
        PasswordField pwd = themeManager.createStyledPasswordField("");
        
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
        PasswordField pwd2 = themeManager.createStyledPasswordField("");
        
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
        alert.getDialogPane().setStyle("-fx-background-color: " + themeManager.getCurrentBaseColor() + "; -fx-background-radius: 15;");
        alert.getDialogPane().setEffect(themeManager.getLightOuterShadow());
        
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        alert.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        
        alert.showAndWait();
    }
}