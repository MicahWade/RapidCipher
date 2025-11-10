package database;

import com.google.gson.annotations.SerializedName;
import core.ConfigManager;
import core.Encryption;
import core.MasterPassword;
import gui.LoginEntry;
import org.lightcouch.CouchDbClient;
import org.lightcouch.NoDocumentException;
import org.lightcouch.Response;
// ***FIXED***: Removed unused import org.lightcouch.View;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;

public class CouchDbDriver implements IDatabaseDriver {

    private CouchDbClient client;
    private String dbName;

    /**
     * Inner class representing the JSON document structure in CouchDB.
     */
    private static class LoginDocument {
        @SerializedName("_id")
        private String id;
        @SerializedName("_rev")
        private String rev;

        // We add a "type" field for good practice, to distinguish doc types
        private String type = "login"; 
        
        private String name;
        private String username;
        private String password;
        private String url;
        private String notes;

        // Getters and setters are needed for LightCouch
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getRev() { return rev; }
        public void setRev(String rev) { this.rev = rev; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    @Override
    public void connect(ConfigManager.DbConfig config) throws Exception {
        System.out.println("Connecting to CouchDB database...");
        String host, port, user, pass;
        try {
            host = Encryption.decryptWithIV(config.host(), MasterPassword.getKey());
            port = Encryption.decryptWithIV(config.port(), MasterPassword.getKey());
            this.dbName = Encryption.decryptWithIV(config.dbName(), MasterPassword.getKey());
            user = Encryption.decryptWithIV(config.user(), MasterPassword.getKey());
            pass = Encryption.decryptWithIV(config.pass(), MasterPassword.getKey());
        } catch (Exception e) {
            throw new Exception("Failed to decrypt CouchDB credentials", e);
        }

        try {
            // true = create DB if it doesn't exist
            client = new CouchDbClient(dbName, true, "http", host, Integer.parseInt(port), user, pass);
            System.out.println("Connected to CouchDB at " + host + ":" + port + "/" + dbName);
            checkAndCreateloginsTable();
        } catch (Exception e) {
            System.err.println("Failed to connect to CouchDB: " + e.getMessage());
            throw new Exception("CouchDB connection failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void checkAndCreateloginsTable() throws Exception {
        // No-op for CouchDB, it's schemaless.
        // The database itself was created in connect().
        System.out.println("CouchDB connection verified. Schemaless, no table creation needed.");
    }

    @Override
    public String createLogin(String name, String username, String password, String url, String notes) {
        try {
            LoginDocument doc = new LoginDocument();
            doc.setName(Encryption.encryptWithIV(name, MasterPassword.getKey()));
            doc.setUsername(Encryption.encryptWithIV(username, MasterPassword.getKey()));
            doc.setPassword(Encryption.encryptWithIV(password, MasterPassword.getKey()));
            doc.setUrl(Encryption.encryptWithIV(url, MasterPassword.getKey()));
            doc.setNotes(Encryption.encryptWithIV(notes, MasterPassword.getKey()));

            Response response = client.save(doc);
            return response.getId(); // Return the CouchDB-generated _id

        } catch (Exception e) {
            System.err.println("Failed to create CouchDB login: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean deleteLogin(String id) {
        try {
            // To delete, we need the document's _id and _rev.
            // We can get them with a find() first.
            LoginDocument doc = client.find(LoginDocument.class, id);
            client.remove(doc.getId(), doc.getRev());
            return true;
        } catch (NoDocumentException e) {
            System.err.println("Document not found, cannot delete: " + id);
            return false;
        } catch (Exception e) {
            System.err.println("Failed to delete CouchDB login: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<LoginEntry> getAllLoginEntries(SecretKey masterKey) throws Exception {
        List<LoginEntry> entries = new ArrayList<>();
        
        // Use the built-in _all_docs view to get all documents
        List<LoginDocument> docs;
        try {
             docs = client.view("_all_docs").includeDocs(true).query(LoginDocument.class);
        } catch(NoDocumentException e) {
            // This happens if the database is completely empty
            return entries;
        }

        // Filter out any design documents or other junk
        for (LoginDocument doc : docs.stream().filter(d -> "login".equals(d.getType())).collect(Collectors.toList())) {
            String id = doc.getId();
            String plainTextName = "!!DECRYPT_ERROR!!";
            String plainTextUser = "!!DECRYPT_ERROR!!";
            String plainTextPass = "!!DECRYPT_ERROR!!";
            String plainTextUrl = "!!DECRYPT_ERROR!!";
            String plainTextNotes = "!!DECRYPT_ERROR!!";

            try {
                plainTextName = Encryption.decryptWithIV(doc.getName(), masterKey);
                plainTextUser = Encryption.decryptWithIV(doc.getUsername(), masterKey);
                plainTextPass = Encryption.decryptWithIV(doc.getPassword(), masterKey);
                plainTextUrl = Encryption.decryptWithIV(doc.getUrl(), masterKey);
                plainTextNotes = Encryption.decryptWithIV(doc.getNotes(), masterKey);
            } catch (Exception e) {
                System.err.println("Failed to decrypt entry with id " + id + " during migration: " + e.getMessage());
            }

            entries.add(new LoginEntry(id, plainTextName, plainTextUser, plainTextPass, plainTextUrl, plainTextNotes));
        }
        return entries;
    }

    @Override
    public void deleteAllLogins() throws Exception {
        // Easiest way to wipe a CouchDB is to delete and re-create the database
        System.out.println("Wiping destination database by recreating it...");
        client.context().deleteDB(this.dbName, this.dbName);
        client.context().createDB(this.dbName);
        System.out.println("Destination table wiped.");
    }

    @Override
    public void closeConnection() {
        if (client != null) {
            System.out.println("Closing CouchDB connection...");
            client.shutdown();
            System.out.println("CouchDB connection closed.");
        }
    }
}