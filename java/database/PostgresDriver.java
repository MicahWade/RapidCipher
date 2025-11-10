package database;

import core.ConfigManager;
import core.Encryption;
import core.MasterPassword;
import gui.LoginEntry;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
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
        
        checkAndCreateloginsTable();
    }
    
    @Override
    public void checkAndCreateloginsTable() throws SQLException {
        if (!doesTableExist("logins") || !isPasswordTableSchemaCorrect()) {
            createloginsTable();
        }
    }

    private boolean doesTableExist(String tableName) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName.toLowerCase(), new String[] { "TABLE" })) {
            return rs.next();
        }
    }

    private boolean isPasswordTableSchemaCorrect() throws SQLException {
        String[] requiredColumns = { "id", "name", "username", "password", "url", "notes" };
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet columns = meta.getColumns(null, null, "logins", null)) {
            int foundColumns = 0;
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                for (String required : requiredColumns) {
                    if (required.equalsIgnoreCase(columnName)) {
                        foundColumns++;
                        break;
                    }
                }
            }
            return foundColumns == requiredColumns.length;
        }
    }

    private void createloginsTable() throws SQLException {
        // Use BIGSERIAL for auto-incrementing 64-bit integer
        String createTableSQL = "CREATE TABLE IF NOT EXISTS logins (" +
            "id BIGSERIAL PRIMARY KEY, " +
            "name TEXT NOT NULL, " +
            "username TEXT NOT NULL, " +
            "password TEXT NOT NULL, " +
            "url TEXT, " +
            "notes TEXT);";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("PostgreSQL logins table created or verified.");
        }
    }

    @Override
    public String createLogin(String name, String username, String password, String url, String notes) {
        String sql = "INSERT INTO logins (name, username, password, url, notes) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, Encryption.encryptWithIV(name, MasterPassword.getKey()));
            pstmt.setString(2, Encryption.encryptWithIV(username, MasterPassword.getKey()));
            pstmt.setString(3, Encryption.encryptWithIV(password, MasterPassword.getKey()));
            pstmt.setString(4, Encryption.encryptWithIV(url, MasterPassword.getKey()));
            pstmt.setString(5, Encryption.encryptWithIV(notes, MasterPassword.getKey()));
            
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return String.valueOf(rs.getLong(1)); // REFACTORED
                    }
                }
            }
            return null;
        } catch (SQLException e) {
            System.err.println("Create login failed: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Encryption failed during create: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean deleteLogin(String id) { // REFACTORED
        String sql = "DELETE FROM logins WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, Long.parseLong(id)); // REFACTORED
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException | NumberFormatException e) {
            System.err.println("Delete login failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<LoginEntry> getAllLoginEntries(SecretKey masterKey) throws SQLException {
        List<LoginEntry> entries = new ArrayList<>();
        String sql = "SELECT id, name, username, password, url, notes FROM logins";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs != null && rs.next()) {
                String id = String.valueOf(rs.getLong("id")); // REFACTORED
                String plainTextName = "!!DECRYPT_ERROR!!";
                String plainTextUser = "!!DECRYPT_ERROR!!";
                String plainTextPass = "!!DECRYPT_ERROR!!";
                String plainTextUrl = "!!DECRYPT_ERROR!!";
                String plainTextNotes = "!!DECRYPT_ERROR!!";
                
                try {
                    plainTextName = Encryption.decryptWithIV(rs.getString("name"), masterKey);
                    plainTextUser = Encryption.decryptWithIV(rs.getString("username"), masterKey);
                    plainTextPass = Encryption.decryptWithIV(rs.getString("password"), masterKey);
                    plainTextUrl = Encryption.decryptWithIV(rs.getString("url"), masterKey);
                    plainTextNotes = Encryption.decryptWithIV(rs.getString("notes"), masterKey);
                    
                } catch (Exception e) {
                    System.err.println("Failed to decrypt entry with id " + id + " during migration: " + e.getMessage());
                }
                
                entries.add(new LoginEntry(id, plainTextName, plainTextUser, plainTextPass, plainTextUrl, plainTextNotes));
            }
        }
        return entries;
    }

    @Override
    public void deleteAllLogins() throws SQLException {
        // Truncate and reset the auto-incrementing serial counter
        String sqlReset = "TRUNCATE TABLE logins RESTART IDENTITY;";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlReset);
            System.out.println("Destination table wiped.");
        }
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