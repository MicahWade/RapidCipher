package core;

import java.util.List;
import javax.crypto.SecretKey;
import database.*;
import gui.LoginEntry;

public class Database {
    
    private IDatabaseDriver driver;

    public Database(ConfigManager.DbConfig config) throws Exception { 
        
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

        try {
            driver.connect(config);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Failed to initialize database driver: " + e.getMessage(), e); 
        }
    }
    
    public String createLogin(String name, String username, String password, String url, String notes) {
        return driver.createLogin(name, username, password, url, notes);
    }

    public boolean deleteLogin(String id) {
        return driver.deleteLogin(id);
    }

    public List<LoginEntry> getAllLoginEntries(SecretKey masterKey) throws Exception {
        return driver.getAllLoginEntries(masterKey);
    }

    public void deleteAllLogins() throws Exception {
        driver.deleteAllLogins();
    }

    public void closeConnection() {
        if (driver != null) {
            driver.closeConnection();
        }
    }
}