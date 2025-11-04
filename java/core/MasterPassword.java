package core;
import javax.crypto.SecretKey;

public class MasterPassword {
    
    private static SecretKey masterKey;

    public static void setKey(SecretKey key) {
        masterKey = key;
    }

    public static SecretKey getKey() {
        if (masterKey == null) {
            throw new IllegalStateException("Master key has not been set. User is not authenticated.");
        }
        return masterKey;
    }
}