package core;
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

public class Database {
    private static Database instance;
    private Connection connection;
    private static final String DB_PATH = System.getProperty("user.home") + "/Documents/RapidCipher/main.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    private Database() throws SQLException {
        try {
            // Ensure directory exists before connecting
            Path dbPath = Paths.get(DB_PATH);
            Path parentDir = dbPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                System.out.println("Created directories for: " + parentDir.toString());
            }

            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to SQLite at " + DB_URL);
            checkAndCreateloginsTable();
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            // Catch IOException or others from Files.createDirectories
            System.err.println("Failed to create directories: " + e.getMessage());
            throw new SQLException(e);
        }
    }

    private void checkAndCreateloginsTable() throws SQLException {
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
        // Check for 'id' as well
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
        // UPDATED to include a primary key ID
        String createTableSQL = "CREATE TABLE IF NOT EXISTS logins (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "username TEXT NOT NULL, " +
                "password TEXT NOT NULL, " +
                "url TEXT, " +
                "notes TEXT);";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("logins table created or verified.");
        }
    }

    public static synchronized Database getInstance() throws SQLException {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    // Create a new login entry
    // UPDATED: Now accepts plain-text password, encrypts ALL fields, and returns the new row's ID
    public long createLogin(String name, String username, String password, String url, String notes) {
        String sql = "INSERT INTO logins (name, username, password, url, notes) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            // Encrypt all 5 fields
            pstmt.setString(1, Encryption.encryptWithIV(name, MasterPassword.getKey()));
            pstmt.setString(2, Encryption.encryptWithIV(username, MasterPassword.getKey()));
            pstmt.setString(3, Encryption.encryptWithIV(password, MasterPassword.getKey()));
            pstmt.setString(4, Encryption.encryptWithIV(url, MasterPassword.getKey()));
            pstmt.setString(5, Encryption.encryptWithIV(notes, MasterPassword.getKey()));
            
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1); // Return the new ID
                    }
                }
            }
            return -1; // Failure
        } catch (SQLException e) {
            System.err.println("Create login failed: " + e.getMessage());
            return -1; // Failure
        } catch (Exception e) {
            System.err.println("Encryption failed during create: " + e.getMessage());
            return -1; // Failure
        }
    }

    // Delete a login by its unique ID
    // UPDATED: Signature now takes a long id
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

    // UPDATED: Removed searchTerm, this method now just gets ALL logins
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

}