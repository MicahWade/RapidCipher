package database;

import core.ConfigManager;
import gui.LoginEntry;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.crypto.SecretKey;

/**
 * Interface for all database drivers, local (SQLite) and remote.
 */
public interface IDatabaseDriver {

    /**
     * Connects to the database using the provided configuration.
     * @param config The database configuration.
     * @throws SQLException if connection fails.
     */
    void connect(ConfigManager.DbConfig config) throws SQLException;

    /**
     * Checks if the 'logins' table exists and has the correct schema,
     * creating it if necessary.
     * @throws SQLException
     */
    void checkAndCreateloginsTable() throws SQLException;

    /**
     * Creates a new login entry in the database.
     * @return The generated ID of the new login, or -1 on failure.
     */
    long createLogin(String name, String username, String password, String url, String notes);

    /**
     * Deletes a login entry by its ID.
     * @return true if successful, false otherwise.
     */
    boolean deleteLogin(long id);

    /**
     * Searches for and returns all login entries.
     * @return A ResultSet containing all logins.
     */
    ResultSet searchLogins();

    /**
     * Retrieves and decrypts all login entries. Used for data migration.
     * @param masterKey The master key to decrypt the data.
     * @return A List of all LoginEntry objects.
     * @throws SQLException
     */
    List<LoginEntry> getAllLoginEntries(SecretKey masterKey) throws SQLException;

    /**
     * Deletes all entries from the 'logins' table. Used for data migration.
     * @throws SQLException
     */
    void deleteAllLogins() throws SQLException;

    /**
     * Closes the database connection.
     */
    void closeConnection();
}