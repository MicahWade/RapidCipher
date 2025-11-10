package database;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import core.ConfigManager;
import core.Encryption;
import core.MasterPassword;
import gui.LoginEntry;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;

public class CassandraDriver implements IDatabaseDriver {

    private CqlSession session;
    private String keyspace;

    @Override
    public void connect(ConfigManager.DbConfig config) throws Exception {
        System.out.println("Connecting to Cassandra database...");

        String host, port, dbName, user, pass;
        try {
            host = Encryption.decryptWithIV(config.host(), MasterPassword.getKey());
            port = Encryption.decryptWithIV(config.port(), MasterPassword.getKey());
            dbName = Encryption.decryptWithIV(config.dbName(), MasterPassword.getKey());
            user = Encryption.decryptWithIV(config.user(), MasterPassword.getKey());
            pass = Encryption.decryptWithIV(config.pass(), MasterPassword.getKey());
        } catch (Exception e) {
            throw new Exception("Failed to decrypt Cassandra credentials", e);
        }

        this.keyspace = dbName;
        // NOTE: Cassandra requires a local datacenter name, which is not in the config.
        // Defaulting to 'datacenter1'. This may need to be configurable.
        String localDatacenter = "datacenter1"; 

        try {
            session = CqlSession.builder()
                    .addContactPoint(new InetSocketAddress(host, Integer.parseInt(port)))
                    .withAuthCredentials(user, pass)
                    .withLocalDatacenter(localDatacenter)
                    .build();
            
            System.out.println("Connected to Cassandra cluster. Checking keyspace...");
            checkAndCreateloginsTable();
            
            // Switch session to use the keyspace
            session.execute("USE " + keyspace);
            System.out.println("Switched to keyspace: " + keyspace);

        } catch (Exception e) {
            System.err.println("Failed to connect to Cassandra: " + e.getMessage());
            throw new Exception("Cassandra connection failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void checkAndCreateloginsTable() throws Exception {
        // 1. Create Keyspace (Database)
        String createKeyspaceCql = "CREATE KEYSPACE IF NOT EXISTS " + keyspace +
                " WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };";
        session.execute(createKeyspaceCql);
        System.out.println("Cassandra keyspace created or verified.");

        // 2. Create Table (Schema)
        String createTableCql = "CREATE TABLE IF NOT EXISTS " + keyspace + ".logins (" +
                "id uuid PRIMARY KEY, " +
                "name text, " +
                "username text, " +
                "password text, " +
                "url text, " +
                "notes text);";
        session.execute(createTableCql);
        System.out.println("Cassandra logins table created or verified.");
    }

    @Override
    public String createLogin(String name, String username, String password, String url, String notes) {
        try {
            UUID newId = UUID.randomUUID();
            String cql = "INSERT INTO logins (id, name, username, password, url, notes) VALUES (?, ?, ?, ?, ?, ?)";
            
            SimpleStatement stmt = SimpleStatement.builder(cql)
                    .addPositionalValue(newId)
                    .addPositionalValue(Encryption.encryptWithIV(name, MasterPassword.getKey()))
                    .addPositionalValue(Encryption.encryptWithIV(username, MasterPassword.getKey()))
                    .addPositionalValue(Encryption.encryptWithIV(password, MasterPassword.getKey()))
                    .addPositionalValue(Encryption.encryptWithIV(url, MasterPassword.getKey()))
                    .addPositionalValue(Encryption.encryptWithIV(notes, MasterPassword.getKey()))
                    .build();
            
            session.execute(stmt);
            return newId.toString();

        } catch (Exception e) {
            System.err.println("Failed to create Cassandra login: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean deleteLogin(String id) {
        try {
            String cql = "DELETE FROM logins WHERE id = ?";
            SimpleStatement stmt = SimpleStatement.builder(cql)
                    .addPositionalValue(UUID.fromString(id))
                    .build();
            
            session.execute(stmt);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to delete Cassandra login: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<LoginEntry> getAllLoginEntries(SecretKey masterKey) throws Exception {
        List<LoginEntry> entries = new ArrayList<>();
        String cql = "SELECT id, name, username, password, url, notes FROM logins";
        
        ResultSet rs = session.execute(cql);
        
        for (Row row : rs) {
            String id = row.getUuid("id").toString();
            String plainTextName = "!!DECRYPT_ERROR!!";
            String plainTextUser = "!!DECRYPT_ERROR!!";
            String plainTextPass = "!!DECRYPT_ERROR!!";
            String plainTextUrl = "!!DECRYPT_ERROR!!";
            String plainTextNotes = "!!DECRYPT_ERROR!!";
            
            try {
                plainTextName = Encryption.decryptWithIV(row.getString("name"), masterKey);
                plainTextUser = Encryption.decryptWithIV(row.getString("username"), masterKey);
                plainTextPass = Encryption.decryptWithIV(row.getString("password"), masterKey);
                plainTextUrl = Encryption.decryptWithIV(row.getString("url"), masterKey);
                plainTextNotes = Encryption.decryptWithIV(row.getString("notes"), masterKey);
                
            } catch (Exception e) {
                System.err.println("Failed to decrypt entry with id " + id + " during migration: " + e.getMessage());
            }
            
            entries.add(new LoginEntry(id, plainTextName, plainTextUser, plainTextPass, plainTextUrl, plainTextNotes));
        }
        return entries;
    }

    @Override
    public void deleteAllLogins() throws Exception {
        session.execute("TRUNCATE logins");
        System.out.println("Destination table wiped.");
    }

    @Override
    public void closeConnection() {
        if (session != null && !session.isClosed()) {
            System.out.println("Closing Cassandra connection...");
            session.close();
            System.out.println("Cassandra connection closed.");
        }
    }
}