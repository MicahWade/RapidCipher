package database;

import core.ConfigManager;
import core.Encryption;
import core.MasterPassword;
import gui.LoginEntry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

public class SqliteDriver implements IDatabaseDriver {

    private Connection connection;
    private static final String SQLITE_DB_PATH = System.getProperty("user.home") + "/Documents/RapidCipher/main.db";
    private static final String SQLITE_DB_URL = "jdbc:sqlite:" + SQLITE_DB_PATH;

    @Override
    public void connect(ConfigManager.DbConfig config) throws SQLException {
        try {
            System.out.println("Connecting to SQLite database...");
            Path dbPath = Paths.get(SQLITE_DB_PATH);
            Path parentDir = dbPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                System.out.println("Created directories for: " + parentDir.toString());
            }
            connection = DriverManager.getConnection(SQLITE_DB_URL);
            System.out.println("Connected to SQLite at " + SQLITE_DB_URL);
            
            checkAndCreateloginsTable();
        } catch (Exception e) {
            System.err.println("Failed to create directories or connect to SQLite: " + e.getMessage());
            throw new SQLException(e);
        }
    }

    @Override
    public void checkAndCreateloginsTable() throws SQLException {
        if (!doesTableExist("logins") || !isPasswordTableSchemaCorrect()) {
            createloginsTable();
        }
    }

    private boolean doesTableExist(String tableName) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName, new String[] { "TABLE" })) {
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
        String createTableSQL = "CREATE TABLE IF NOT EXISTS logins (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "username TEXT NOT NULL, " +
            "password TEXT NOT NULL, " +
            "url TEXT, " +
            "notes TEXT);";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("SQLite logins table created or verified.");
        }
    }

    @Override
    public long createLogin(String name, String username, String password, String url, String notes) {
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
                        return rs.getLong(1);
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            System.err.println("Create login failed: " + e.getMessage());
            return -1;
        } catch (Exception e) {
            System.err.println("Encryption failed during create: " + e.getMessage());
            return -1;
        }
    }

    @Override
    public boolean deleteLogin(long id) {
        String sql = "DELETE FROM logins WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Delete login failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public ResultSet searchLogins() {
        String sql = "SELECT id, name, username, password, url, notes FROM logins";
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            System.err.println("Search login failed: " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<LoginEntry> getAllLoginEntries(SecretKey masterKey) throws SQLException {
        List<LoginEntry> entries = new ArrayList<>();
        String sql = "SELECT id, name, username, password, url, notes FROM logins";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs != null && rs.next()) {
                long id = rs.getLong("id");
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
        String sql = "DELETE FROM logins";
        String sqlReset = "DELETE FROM sqlite_sequence WHERE name='logins';"; 
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            try {
                stmt.execute(sqlReset); 
            } catch (SQLException e) {
                // This fails on MySQL, ignore
            }
            System.out.println("Destination table wiped.");
        }
    }
    
    @Override
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                System.out.println("Closing SQLite connection...");
                connection.close();
                System.out.println("SQLite connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Failed to close SQLite connection: " + e.getMessage());
        }
    }
}