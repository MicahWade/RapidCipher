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

public class MySqlDriver implements IDatabaseDriver {

    private Connection connection;

    @Override
    public void connect(ConfigManager.DbConfig config) throws SQLException {
        try {
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
            
            String dbUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName +
                    "?useSSL=true&requireSSL=true";
            
            System.out.println("Connecting to MySQL at " + dbUrl);
            connection = DriverManager.getConnection(dbUrl, user, pass);
            System.out.println("Connected to MySQL.");

            checkAndCreateloginsTable();
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            System.err.println("Failed to connect to MySQL: " + e.getMessage());
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
            "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
            "name TEXT NOT NULL, " +
            "username TEXT NOT NULL, " +
            "password TEXT NOT NULL, " +
            "url TEXT, " +
            "notes TEXT);";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("MySQL logins table created or verified.");
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
        // MySQL auto-increment resets if table is truncated
        String sqlReset = "TRUNCATE TABLE logins;";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlReset);
            System.out.println("Destination table wiped.");
        }
    }
    
    @Override
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                System.out.println("Closing MySQL connection...");
                connection.close();
                System.out.println("MySQL connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Failed to close MySQL connection: " + e.getMessage());
        }
    }
}