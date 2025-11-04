package bridge;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import core.ConfigManager;
import core.Database;
import core.Encryption;
import core.MasterPassword;
import gui.AuthManager;
import gui.LoginEntry;
import gui.ThemeManager;
import javafx.application.Platform;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles routing commands from the bridge to the core application logic.
 */
public class CommandRouter {

    private final Gson gson = new Gson();
    private final AuthManager authManager;
    private Database database;
    private ConfigManager.DbConfig dbConfig;
    
    // Copied from AuthManager
    private static final String DB_DIR_PATH = System.getProperty("user.home") + "/Documents/RapidCipher";
    private static final Path SALT_FILE = Paths.get(DB_DIR_PATH, "salt.bin");
    private static final Path KEY_CHECK_FILE = Paths.get(DB_DIR_PATH, "key_check.bin");
    private static final String KEY_CHECK_STRING = "RapidCipher-OK";

    public CommandRouter(ThemeManager themeManager) {
        this.authManager = new AuthManager(themeManager);
    }

    /**
     * Routes a command and returns a JSON response.
     * This method is called from the I/O thread.
     */
    public JsonObject routeCommand(JsonObject request) {
        String command = request.has("command") ? request.get("command").getAsString() : "unknown";

        try {
            switch (command) {
                case "getStatus":
                    return getStatus();
                case "requestUnlock": // "Bridge Mode" unlock
                    return requestUnlock();
                case "getLogins": // "Bridge Mode" get logins (requires prior unlock)
                    return getLogins(MasterPassword.getKey()); // Fails if locked
                
                // --- NEW "DIRECT MODE" COMMAND ---
                case "unlockAndGetLoginsDirectly":
                    if (!request.has("password")) {
                        return createErrorResponse("Password not provided for direct unlock.");
                    }
                    String password = request.get("password").getAsString();
                    return unlockAndGetLoginsDirectly(password);

                default:
                    return createErrorResponse("Unknown command: " + command);
            }
        } catch (Exception e) {
            return createErrorResponse("Error processing command '" + command + "': " + e.getMessage());
        }
    }
    
    /**
     * NEW "Direct Mode" Handler: Authenticates, decrypts, and returns logins in one go.
     * This method does NOT set the global MasterPassword.key.
     */
    private JsonObject unlockAndGetLoginsDirectly(String password) {
        try {
            // 1. Authenticate the password
            byte[] salt = Files.readAllBytes(SALT_FILE);
            SecretKey tempKey = Encryption.getSecretKey(password, salt);
            
            String[] checkData = new String(Files.readAllBytes(KEY_CHECK_FILE), "UTF-8").split(":");
            byte[] iv = Base64.getDecoder().decode(checkData[0]);
            String encryptedCheck = checkData[1];
            
            String decrypted = Encryption.decrypt(encryptedCheck, tempKey, new IvParameterSpec(iv));

            if (!KEY_CHECK_STRING.equals(decrypted)) {
                return createErrorResponse("Incorrect password.");
            }
            
            // 2. Auth success, now get logins using this temporary key
            return getLogins(tempKey);
            
        } catch (Exception e) {
            return createErrorResponse("Direct unlock failed: " + e.getMessage());
        }
    }

    /**
     * Checks if the *Bridge Mode* vault is unlocked.
     */
    private JsonObject getStatus() {
        JsonObject response = createSuccessResponse();
        try {
            MasterPassword.getKey(); // This will throw if key is null
            response.addProperty("status", "unlocked");
        } catch (IllegalStateException e) {
            response.addProperty("status", "locked");
        }
        return response;
    }

    /**
     * Triggers the native JavaFX password prompt for *Bridge Mode*.
     */
    private JsonObject requestUnlock() {
        AtomicReference<JsonObject> responseRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                boolean success = authManager.showMasterPasswordPrompt();
                if (success) {
                    responseRef.set(createSuccessResponse());
                    responseRef.get().addProperty("status", "unlocked");
                } else {
                    responseRef.set(createErrorResponse("Login cancelled or failed."));
                }
            } catch (Exception e) {
                responseRef.set(createErrorResponse("Auth exception: " + e.getMessage()));
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            return createErrorResponse("Auth was interrupted.");
        }
        
        return responseRef.get();
    }

    /**
     * Connects to the database, decrypts, and returns all logins using a provided key.
     */
    private JsonObject getLogins(SecretKey key) {
        // Ensure database is initialized
        if (database == null) {
            try {
                this.dbConfig = ConfigManager.loadConfig();
                this.database = new Database(dbConfig);
            } catch (Exception e) {
                return createErrorResponse("Failed to connect to database: " + e.getMessage());
            }
        }

        JsonArray loginsArray = new JsonArray();
        try {
            ResultSet rs = database.searchLogins();
            while (rs != null && rs.next()) {
                JsonObject login = new JsonObject();
                login.addProperty("id", rs.getLong("id"));
                
                // Decrypt data
                login.addProperty("name", Encryption.decryptWithIV(rs.getString("name"), key));
                login.addProperty("username", Encryption.decryptWithIV(rs.getString("username"), key));
                login.addProperty("password", Encryption.decryptWithIV(rs.getString("password"), key));
                login.addProperty("url", Encryption.decryptWithIV(rs.getString("url"), key));
                login.addProperty("notes", Encryption.decryptWithIV(rs.getString("notes"), key));
                
                loginsArray.add(login);
            }
            if (rs != null) rs.close();
            
        } catch (SQLException e) {
            // Try to close and reconnect on SQL exception
            try {
                if (database != null) database.closeConnection();
                this.dbConfig = ConfigManager.loadConfig();
                this.database = new Database(dbConfig);
            } catch (Exception ex) {
                 return createErrorResponse("Failed to reconnect to database: " + ex.getMessage());
            }
            // If reconnected, try one more time
            try (ResultSet rsRetry = database.searchLogins()) {
                 while (rsRetry != null && rsRetry.next()) {
                    JsonObject login = new JsonObject();
                    login.addProperty("id", rsRetry.getLong("id"));
                    login.addProperty("name", Encryption.decryptWithIV(rsRetry.getString("name"), key));
                    login.addProperty("username", Encryption.decryptWithIV(rsRetry.getString("username"), key));
                    login.addProperty("password", Encryption.decryptWithIV(rsRetry.getString("password"), key));
                    login.addProperty("url", Encryption.decryptWithIV(rsRetry.getString("url"), key));
                    login.addProperty("notes", Encryption.decryptWithIV(rsRetry.getString("notes"), key));
                    loginsArray.add(login);
                }
            } catch (Exception e2) {
                 return createErrorResponse("Failed to retrieve or decrypt logins after retry: " + e2.getMessage());
            }
            
        } catch (Exception e) {
            return createErrorResponse("Failed to retrieve or decrypt logins: " + e.getMessage());
        }

        JsonObject response = createSuccessResponse();
        response.add("logins", loginsArray);
        return response;
    }


    // --- Helper Methods ---

    public static JsonObject createErrorResponse(String error) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "error");
        response.addProperty("message", error);
        return response;
    }

    public static JsonObject createSuccessResponse() {
        JsonObject response = new JsonObject();
        response.addProperty("status", "success");
        return response;
    }
}

