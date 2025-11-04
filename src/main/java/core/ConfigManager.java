package core;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigManager {

    private static final String DB_DIR_PATH = System.getProperty("user.home") + "/Documents/RapidCipher";
    private static final Path CONFIG_FILE = Paths.get(DB_DIR_PATH, "config.properties");
    
    public record DbConfig(
        String dbType,
        String host,
        String port,
        String dbName,
        String user,
        String pass
    ) {}

    public static DbConfig loadConfig() {
        Properties props = new Properties();
        
        String dbType = "SQLITE";
        String host = "";
        String port = "3306";
        String dbName = "rapidcipher";
        String user = "";
        String pass = "";

        if (Files.exists(CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                props.load(in);
                dbType = props.getProperty("db.type", "SQLITE");
                host = props.getProperty("db.host", "");
                port = props.getProperty("db.port", "3306");
                dbName = props.getProperty("db.name", "rapidcipher");
                user = props.getProperty("db.user", "");
                pass = props.getProperty("db.pass", "");
            } catch (Exception e) {
                System.err.println("Failed to load config file, using defaults: " + e.getMessage());
            }
        } else {
            saveConfig(new DbConfig(dbType, host, port, dbName, user, pass));
        }

        return new DbConfig(dbType, host, port, dbName, user, pass);
    }

    public static void saveConfig(DbConfig config) {
        Properties props = new Properties();
        props.setProperty("db.type", config.dbType());

        String warning = "RapidCipher Database Configuration\n";
        
        if (config.dbType().equals("MYSQL")) {
            try {
                props.setProperty("db.host", Encryption.encryptWithIV(config.host(), MasterPassword.getKey()));
                props.setProperty("db.port", Encryption.encryptWithIV(config.port(), MasterPassword.getKey()));
                props.setProperty("db.name", Encryption.encryptWithIV(config.dbName(), MasterPassword.getKey()));
                props.setProperty("db.user", Encryption.encryptWithIV(config.user(), MasterPassword.getKey()));
                props.setProperty("db.pass", Encryption.encryptWithIV(config.pass(), MasterPassword.getKey()));
                warning += "MySQL credentials are encrypted.\n";

            } catch (IllegalStateException e) {
                System.err.println("Cannot save config: Master key is not available. " + e.getMessage());
                props.setProperty("db.host", config.host());
                props.setProperty("db.port", config.port());
                props.setProperty("db.name", config.dbName());
                props.setProperty("db.user", config.user());
                props.setProperty("db.pass", config.pass());
                warning += "WARNING: Could not encrypt credentials, Master Key not set. Storing in plain text.\n";

            } catch (Exception e) {
                System.err.println("Failed to encrypt config: " + e.getMessage());
                return;
            }
        } else {
            props.setProperty("db.host", config.host());
            props.setProperty("db.port", config.port());
            props.setProperty("db.name", config.dbName());
            props.setProperty("db.user", config.user());
            props.setProperty("db.pass", config.pass());
        }
        
        try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
            props.store(out, warning);
        } catch (Exception e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }
}