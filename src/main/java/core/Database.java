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
    
    private static final String SQLITE_DB_PATH = System.getProperty("user.home") + "/Documents/RapidCipher/main.db";
    private static final String SQLITE_DB_URL = "jdbc:sqlite:" + SQLITE_DB_PATH;

    private Database(ConfigManager.DbConfig config) throws SQLException {
        try {
            String dbUrl;
            
            if (config.dbType().equals("MYSQL")) {
                // Connect to remote MySQL database
                System.out.println("Connecting to MySQL database...");
                try {
                    // Ensure the driver is loaded
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException e) {
                    System.err.println("MySQL JDBC Driver not found. Add it to pom.xml.");
                    throw new SQLException("MySQL Driver not found", e);
                }
                
                // --- DECRYPT CREDENTIALS ---
                String host = "";
                String port = "";
                String dbName = "";
                String user = "";
                String pass = "";
                
                try {
                    host = Encryption.decryptWithIV(config.host(), MasterPassword.getKey());
                    port = Encryption.decryptWithIV(config.port(), MasterPassword.getKey());
                    dbName = Encryption.decryptWithIV(config.dbName(), MasterPassword.getKey());
                    user = Encryption.decryptWithIV(config.user(), MasterPassword.getKey());
                    pass = Encryption.decryptWithIV(config.pass(), MasterPassword.getKey());
                } catch (Exception e) {
                    System.err.println("Failed to decrypt MySQL credentials: " + e.getMessage());
                    throw new SQLException("Failed to decrypt credentials. Is config file corrupted or password correct?", e);
                }
                
                dbUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName +
                        "?useSSL=true&requireSSL=true"; // Force SSL
                
                System.out.println("Connecting to MySQL at " + dbUrl);
                connection = DriverManager.getConnection(dbUrl, user, pass);
                System.out.println("Connected to MySQL.");

            } else {
                // Default to local SQLite database
                System.out.println("Connecting to SQLite database...");
                Path dbPath = Paths.get(SQLITE_DB_PATH);
                Path parentDir = dbPath.getParent();
                if (parentDir != null && !Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                    System.out.println("Created directories for: " + parentDir.toString());
                }
                dbUrl = SQLITE_DB_URL;
                connection = DriverManager.getConnection(dbUrl);
                System.out.println("Connected to SQLite at " + dbUrl);
            }

            checkAndCreateloginsTable(config.dbType()); // Pass dbType
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            // Catch IOException or others
            System.err.println("Failed to create directories or connect: " + e.getMessage());
            throw new SQLException(e);
        }
    }

    // --- UPDATED METHOD ---
    private void checkAndCreateloginsTable(String dbType) throws SQLException {
        if (!doesTableExist("logins") || !isPasswordTableSchemaCorrect()) {
            createloginsTable(dbType); // Pass dbType
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

    // --- UPDATED METHOD ---
    private void createloginsTable(String dbType) throws SQLException {
        
        String createTableSQL;
        
        if (dbType.equals("MYSQL")) {
             // MySQL specific-syntax (e.g., auto_increment)
             // Using TEXT as blob-like storage for the encrypted base64 strings
            createTableSQL = "CREATE TABLE IF NOT EXISTS logins (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "name TEXT NOT NULL, " +
                "username TEXT NOT NULL, " +
                "password TEXT NOT NULL, " +
                "url TEXT, " +
                "notes TEXT);";
        } else {
            // SQLite syntax
            createTableSQL = "CREATE TABLE IF NOT EXISTS logins (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "username TEXT NOT NULL, " +
                "password TEXT NOT NULL, " +
                "url TEXT, " +
                "notes TEXT);";
        }
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("logins table created or verified.");
        }
    }

    // --- UPDATED METHOD ---
    public static synchronized Database getInstance(ConfigManager.DbConfig config) throws SQLException {
        if (instance == null) {
            instance = new Database(config);
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