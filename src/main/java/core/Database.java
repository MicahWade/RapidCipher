package core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.crypto.SecretKey;
import database.*; // Import the new package
import gui.LoginEntry;

public class Database {
    
    private IDatabaseDriver driver; // Use the interface

    public Database(ConfigManager.DbConfig config) throws SQLException {
        
        // This constructor now acts as a factory
        switch (config.dbType().toUpperCase()) {
            case "MYSQL":
                this.driver = new MySqlDriver();
                break;
            case "POSTGRESQL":
                this.driver = new PostgresDriver();
                break;
            case "FIREBIRD":
                this.driver = new FirebirdDriver();
                break;
            case "CASSANDRA":
                this.driver = new CassandraDriver();
                break;
            case "COUCHDB":
                this.driver = new CouchDbDriver();
                break;
            case "SQLITE":
            default:
                this.driver = new SqliteDriver();
                break;
        }

        // Connect using the selected driver
        try {
            driver.connect(config);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException("Failed to initialize database driver: " + e.getMessage(), e);
        }
    }
    
    // --- All methods below are now delegated to the driver ---

    public long createLogin(String name, String username, String password, String url, String notes) {
        return driver.createLogin(name, username, password, url, notes);
    }

    public boolean deleteLogin(long id) {
        return driver.deleteLogin(id);
    }

    public ResultSet searchLogins() {
        return driver.searchLogins();
    }

    public List<LoginEntry> getAllLoginEntries(SecretKey masterKey) throws SQLException {
        return driver.getAllLoginEntries(masterKey);
    }

    public void deleteAllLogins() throws SQLException {
        driver.deleteAllLogins();
    }

    public void closeConnection() {
        if (driver != null) {
            driver.closeConnection();
        }
    }
}