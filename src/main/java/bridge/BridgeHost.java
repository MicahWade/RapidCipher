package bridge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import gui.ThemeManager;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Native Messaging Bridge that runs as a separate thread inside the main GUI application.
 * It implements Runnable and listens to System.in/System.out for messages from the browser.
 */
public class BridgeHost implements Runnable {

    private static final Gson gson = new Gson();
    private static CommandRouter commandRouter;
    private static OutputStream stdOut;
    private static InputStream stdIn;
    
    private final ThemeManager themeManager;
    
    // volatile ensures changes are visible across threads
    private volatile boolean isRunning = true; 

    public BridgeHost(ThemeManager themeManager) {
        this.themeManager = themeManager;
    }

    /**
     * The main entry point for the bridge thread.
     */
    @Override
    public void run() {
        try {
            commandRouter = new CommandRouter(themeManager);
            stdOut = System.out;
            stdIn = System.in;
            
            readMessages();
            
        } catch (Exception e) {
            if (isRunning) {
                // Log unexpected errors
                System.err.println("BridgeHost thread terminated with error: " + e.getMessage());
            }
        } finally {
             System.out.println("BridgeHost thread shutting down.");
        }
    }

    /**
     * Reads messages from System.in in a loop until isRunning is false.
     */
    private void readMessages() {
        try {
            while (isRunning) {
                // Read the 4-byte message length (little-endian)
                byte[] lengthBytes = new byte[4];
                
                // This read is blocking, so we need to be able to interrupt it
                int bytesRead = stdIn.read(lengthBytes);
                if (bytesRead != 4) {
                    // End of stream or interrupted
                    break;
                }
                
                int length = getMessageLength(lengthBytes);

                // Read the message content
                byte[] messageBytes = new byte[length];
                if (stdIn.read(messageBytes) != length) {
                    break; // Or handle partial read
                }
                String messageJson = new String(messageBytes, StandardCharsets.UTF_8);

                // Process the message
                JsonObject response = processMessage(messageJson);

                // Send the response
                sendMessage(response.toString());
            }
        } catch (IOException e) {
            if (isRunning) {
                System.err.println("Bridge IO Error: " + e.getMessage());
            } else {
                // This is expected if stdIn was closed by stop()
                System.out.println("Bridge I/O closed normally.");
            }
        }
    }
    
    /**
     * Signals the bridge thread to stop and closes System.in to interrupt
     * the blocking read() call, allowing the thread to terminate.
     */
    public void stop() {
        isRunning = false;
        try {
            // Closing stdIn will cause the stdIn.read() call to
            // throw an IOException, breaking the readMessages() loop.
            if (stdIn != null) {
                stdIn.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing System.in: " + e.getMessage());
        }
    }

    /**
     * Processes an incoming JSON message and routes it to the CommandRouter.
     */
    private JsonObject processMessage(String messageJson) {
        try {
            JsonObject jsonRequest = gson.fromJson(messageJson, JsonObject.class);
            
            // Route the command
            // The CommandRouter will use Platform.runLater for any UI tasks
            return commandRouter.routeCommand(jsonRequest);
            
        } catch (JsonSyntaxException e) {
            return CommandRouter.createErrorResponse("Invalid JSON: " + e.getMessage());
        } catch (Exception e) {
            return CommandRouter.createErrorResponse("Internal Java Error: " + e.getMessage());
        }
    }

    /**
     * Sends a JSON message to System.out, prefixed with the 4-byte length.
     * This method is static so the CommandRouter can call it.
     */
    public static void sendMessage(String messageJson) {
        try {
            byte[] messageBytes = messageJson.getBytes(StandardCharsets.UTF_8);
            byte[] lengthBytes = intToLittleEndian(messageBytes.length);

            // Synchronize on stdOut to prevent garbled messages
            synchronized (stdOut) {
                stdOut.write(lengthBytes);
                stdOut.write(messageBytes);
                stdOut.flush();
            }
        } catch (IOException e) {
            // Handle error (e.g., pipe closed)
            System.err.println("Failed to send message to extension: " + e.getMessage());
        }
    }

    /**
     * Converts an integer to a 4-byte little-endian array.
     */
    private static byte[] intToLittleEndian(int value) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (value & 0xFF);
        bytes[1] = (byte) ((value >> 8) & 0xFF);
        bytes[2] = (byte) ((value >> 16) & 0xFF);
        bytes[3] = (byte) ((value >> 24) & 0xFF);
        return bytes;
    }

    /**
     * Converts a 4-byte little-endian array to an integer.
     */
    private int getMessageLength(byte[] bytes) {
        return (bytes[0] & 0xFF) |
               ((bytes[1] & 0xFF) << 8) |
               ((bytes[2] & 0xFF) << 16) |
               ((bytes[3] & 0xFF) << 24);
    }
}

