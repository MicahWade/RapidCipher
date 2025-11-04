package database;

import core.ConfigManager;
import core.Encryption;
import core.MasterPassword;
import gui.LoginEntry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.crypto.SecretKey;

public class PostgresDriver implements IDatabaseDriver {
    
    private Connection connection;

    @Override
    public void connect(ConfigManager.DbConfig config) throws SQLException {
        System.out.println("Connecting to PostgreSQL database...");
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found. Add it to pom.xml.");
            throw new SQLException("PostgreSQL Driver not found", e);
        }

        String host, port, dbName, user, pass;
        try {
            host = Encryption.decryptWithIV(config.host(), MasterPassword.getKey());
            port = Encryption.decryptWithIV(config.port(), MasterPassword.getKey());
            dbName = Encryption.decryptWithIV(config.dbName(), MasterPassword.getKey());
            user = Encryption.decryptWithIV(config.user(), MasterPassword.getKey());
            pass = Encryption.decryptWithIV(config.pass(), MasterPassword.getKey());
        } catch (Exception e) {
            throw new SQLException("Failed to decrypt PostgreSQL credentials", e);
        }

        String dbUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
        System.out.println("Connecting to PostgreSQL at " + dbUrl);
        connection = DriverManager.getConnection(dbUrl, user, pass);
        System.out.println("Connected to PostgreSQL.");
        
        // In a real implementation, you would call checkAndCreateloginsTable() here
        // For this stub, we'll just show a message.
        System.out.println("PostgreSQL driver is a stub. Table creation logic not implemented.");
    }
    
    private void checkStub() throws SQLException {
        if (connection == null) {
            throw new SQLException("Not connected to database.");
        }
        throw new UnsupportedOperationException("This database driver is a stub and does not implement this functionality.");
    }

    @Override
    public void checkAndCreateloginsTable() throws SQLException {
        checkStub();
    }

    @Override
    public long createLogin(String name, String username, String password, String url, String notes) {
        try {
            checkStub();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public boolean deleteLogin(long id) {
        try {
            checkStub();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public ResultSet searchLogins() {
        try {
            checkStub();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<LoginEntry> getAllLoginEntries(SecretKey masterKey) throws SQLException {
        checkStub();
        return null;
    }

    @Override
    public void deleteAllLogins() throws SQLException {
        checkStub();
    }

    @Override
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                System.out.println("Closing PostgreSQL connection...");
                connection.close();
                System.out.println("PostgreSQL connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Failed to close PostgreSQL connection: " + e.getMessage());
        }
    }
}