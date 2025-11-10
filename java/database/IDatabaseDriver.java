package database;

import core.ConfigManager;
import gui.LoginEntry;
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
    void connect(ConfigManager.DbConfig config) throws Exception;

    /**
     * Checks if the 'logins' table/collection exists and has the correct schema,
     * creating it if necessary.
     * @throws SQLException
     */
    void checkAndCreateloginsTable() throws Exception;

    /**
     * Creates a new login entry in the database.
     * @return The generated ID of the new login, or null on failure.
     */
    String createLogin(String name, String username, String password, String url, String notes);

    /**
     * Deletes a login entry by its ID.
     * @return true if successful, false otherwise.
     */
    boolean deleteLogin(String id);

    /**
     * Retrieves and decrypts all login entries. Used for data migration and loading.
     * @param masterKey The master key to decrypt the data.
     * @return A List of all LoginEntry objects.
     * @throws Exception
     */
    List<LoginEntry> getAllLoginEntries(SecretKey masterKey) throws Exception;

    /**
     * Deletes all entries from the 'logins' table/collection. Used for data migration.
     * @throws Exception
     */
    void deleteAllLogins() throws Exception;

    /**
     * Closes the database connection.
     */
    void closeConnection();
}