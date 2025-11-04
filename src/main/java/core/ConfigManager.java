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
    
    // This record holds the in-memory config
    public record DbConfig(
        String dbType, // "SQLITE" or "MYSQL"
        String host,
        String port,
        String dbName,
        String user,
        String pass
    ) {}

    public static DbConfig loadConfig() {
        Properties props = new Properties();
        
        // Set defaults first
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
                pass = props.getProperty("db.pass", ""); // WARNING: Stored in plain text
            } catch (Exception e) {
                System.err.println("Failed to load config file, using defaults: " + e.getMessage());
            }
        } else {
            // If file doesn't exist, create it with defaults
            saveConfig(new DbConfig(dbType, host, port, dbName, user, pass));
        }

        return new DbConfig(dbType, host, port, dbName, user, pass);
    }

    public static void saveConfig(DbConfig config) {
        Properties props = new Properties();
        props.setProperty("db.type", config.dbType());
        props.setProperty("db.host", config.host());
        props.setProperty("db.port", config.port());
        props.setProperty("db.name", config.dbName());
        props.setProperty("db.user", config.user());
        props.setProperty("db.pass", config.pass()); // WARNING: Stored in plain text
        
        try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
            props.store(out, "RapidCipher Database Configuration\n" +
                             "WARNING: db.pass is stored in plain text. Secure this file.");
        } catch (Exception e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }
}