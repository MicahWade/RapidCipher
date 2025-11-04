package database;

import core.ConfigManager;
import gui.LoginEntry;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.crypto.SecretKey;

public class CassandraDriver implements IDatabaseDriver {

    public CassandraDriver() {}
    
    private void checkStub() throws SQLException {
        throw new UnsupportedOperationException("Apache Cassandra is a NoSQL database and is not supported by the current application structure.");
    }

    @Override
    public void connect(ConfigManager.DbConfig config) throws SQLException {
        System.err.println("Apache Cassandra driver is not implemented.");
        checkStub();
    }

    @Override
    public void checkAndCreateloginsTable() throws SQLException { checkStub(); }

    @Override
    public long createLogin(String name, String username, String password, String url, String notes) {
        try { checkStub(); } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    @Override
    public boolean deleteLogin(long id) {
        try { checkStub(); } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    @Override
    public ResultSet searchLogins() {
        try { checkStub(); } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public List<LoginEntry> getAllLoginEntries(SecretKey masterKey) throws SQLException {
        checkStub();
        return null;
    }

    @Override
    public void deleteAllLogins() throws SQLException { checkStub(); }

    @Override
    public void closeConnection() {
        // No connection to close
    }
}