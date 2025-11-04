package bridge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * This is a separate console application that acts as the Native Messaging client.
 * The browser executes this program.
 * It connects to the main RapidCipher application's BridgeHost (server) via a socket
 * and relays messages between the browser (stdio) and the main app (socket).
 */
public class NativeRelay {

    public static void main(String[] args) {
        try {
            String logPath = System.getProperty("user.home") + "/Documents/RapidCipher/relay-log.txt";
            java.io.File logFile = new java.io.File(logPath);
            // Clear the log on each run
            if (logFile.exists()) {
                logFile.delete();
            }
            System.setErr(new java.io.PrintStream(new java.io.FileOutputStream(logPath, true), true, StandardCharsets.UTF_8));
        } catch (Exception e) { /* ignore */ }
        

        System.err.println("NativeRelay: Starting...");

        try (Socket socket = new Socket("127.0.0.1", BridgeHost.PORT)) {
            System.err.println("NativeRelay: Connected to server on port " + BridgeHost.PORT);
            
            // Stream to read from server
            BufferedReader serverReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            
            // Stream to write to server
            PrintWriter serverWriter = new PrintWriter(
                    socket.getOutputStream(), true, StandardCharsets.UTF_8);

            // Streams for browser stdio
            InputStream browserIn = System.in;
            OutputStream browserOut = System.out;

            // Thread 1: Read from Server, write to Browser (stdout)
            Thread serverToBrowser = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = serverReader.readLine()) != null) {
                        System.err.println("NativeRelay (Server->Browser): " + serverMessage);
                        sendMessageToBrowser(browserOut, serverMessage);
                    }
                } catch (IOException e) {
                    System.err.println("Relay (Server->Browser): Connection lost. " + e.getMessage());
                }
            });
            serverToBrowser.setDaemon(true);
            serverToBrowser.start();

            while (true) {
                String browserMessage = readMessageFromBrowser(browserIn);
                if (browserMessage == null) {
                    break; // End of stream from browser
                }
                System.err.println("NativeRelay (Browser->Server): " + browserMessage);
                serverWriter.println(browserMessage);
            }

        } catch (Exception e) {
            System.err.println("NativeRelay: FAILED to connect to RapidCipher server on port "
                    + BridgeHost.PORT + ". Is the main app running and the bridge enabled?");
            System.err.println(e.getMessage());
        }
    }

    /**
     * Reads a message from the browser's stdio.
     * Protocol: 4-byte little-endian length, followed by JSON message.
     */
    private static String readMessageFromBrowser(InputStream in) throws IOException {
        byte[] lengthBytes = new byte[4];
        int bytesRead = in.readNBytes(lengthBytes, 0, 4);
        if (bytesRead != 4) {
            // End of stream
            return null;
        }

        int length = (lengthBytes[0] & 0xFF) |
                     ((lengthBytes[1] & 0xFF) << 8) |
                     ((lengthBytes[2] & 0xFF) << 16) |
                     ((lengthBytes[3] & 0xFF) << 24);

        if (length == 0) {
            return null;
        }

        byte[] messageBytes = new byte[length];
        int totalBytesRead = 0;
        while(totalBytesRead < length) {
            int read = in.read(messageBytes, totalBytesRead, length - totalBytesRead);
            if (read == -1) {
                throw new IOException("Unexpected end of stream while reading message body.");
            }
            totalBytesRead += read;
        }

        return new String(messageBytes, StandardCharsets.UTF_8);
    }

    private static void sendMessageToBrowser(OutputStream out, String messageJson) throws IOException {
        byte[] messageBytes = messageJson.getBytes(StandardCharsets.UTF_8);
        int length = messageBytes.length;

        byte[] lengthBytes = new byte[4];
        lengthBytes[0] = (byte) (length & 0xFF);
        lengthBytes[1] = (byte) ((length >> 8) & 0xFF);
        lengthBytes[2] = (byte) ((length >> 16) & 0xFF);
        lengthBytes[3] = (byte) ((length >> 24) & 0xFF);

        out.write(lengthBytes);
        out.write(messageBytes);
        out.flush();
    }
}


