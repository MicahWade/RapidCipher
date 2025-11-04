package bridge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Handles a single client socket connection for the BridgeHost server.
 * Runs in its own thread.
 */
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final CommandRouter commandRouter;
    private final Gson gson = new Gson();
    private PrintWriter writer;

    public ClientHandler(Socket socket, CommandRouter router) {
        this.clientSocket = socket;
        this.commandRouter = router;
    }

    @Override
    public void run() {
        try (InputStreamReader isr = new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr);
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true, StandardCharsets.UTF_8)) {

            this.writer = writer;
            String requestJson;

            // Read messages (JSON strings, one per line)
            while ((requestJson = reader.readLine()) != null) {
                JsonObject response;
                try {
                    JsonObject jsonRequest = gson.fromJson(requestJson, JsonObject.class);
                    // Route the command and get a response
                    response = commandRouter.routeCommand(jsonRequest);
                } catch (JsonSyntaxException e) {
                    response = CommandRouter.createErrorResponse("Invalid JSON received: " + e.getMessage());
                } catch (Exception e) {
                    response = CommandRouter.createErrorResponse("Internal Java Error: " + e.getMessage());
                }

                // Send the response back to the client (NativeRelay)
                // We send it as a simple string terminated by a newline.
                writer.println(response.toString());
            }

        } catch (IOException e) {
            System.out.println("ClientHandler: Client disconnected: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.err.println("ClientHandler: Error closing client socket: " + e.getMessage());
            }
            System.out.println("ClientHandler: Connection closed.");
        }
    }
}
