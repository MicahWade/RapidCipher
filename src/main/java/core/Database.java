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
                System.out.println("Connecting to MySQL database...");
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException e) {
                    System.err.println("MySQL JDBC Driver not found. Add it to pom.xml.");
                    throw new SQLException("MySQL Driver not found", e);
                }
                
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
                        "?useSSL=true&requireSSL=true";
                
                System.out.println("Connecting to MySQL at " + dbUrl);
                connection = DriverManager.getConnection(dbUrl, user, pass);
                System.out.println("Connected to MySQL.");

            } else {
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

            checkAndCreateloginsTable(config.dbType());
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            System.err.println("Failed to create directories or connect: " + e.getMessage());
            throw new SQLException(e);
        }
    }

    private void checkAndCreateloginsTable(String dbType) throws SQLException {
        if (!doesTableExist("logins") || !isPasswordTableSchemaCorrect()) {
            createloginsTable(dbType);
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

    private void createloginsTable(String dbType) throws SQLException {
        
        String createTableSQL;
        
        if (dbType.equals("MYSQL")) {
            createTableSQL = "CREATE TABLE IF NOT EXISTS logins (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "name TEXT NOT NULL, " +
                "username TEXT NOT NULL, " +
                "password TEXT NOT NULL, " +
                "url TEXT, " +
                "notes TEXT);";
        } else {
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

    public static synchronized Database getInstance(ConfigManager.DbConfig config) throws SQLException {
        if (instance == null) {
            instance = new Database(config);
        }
        return instance;
    }

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