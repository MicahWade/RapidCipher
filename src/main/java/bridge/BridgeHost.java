    package bridge;	

    import java.io.IOException;
    import java.net.ServerSocket;
    import java.net.Socket;
    import java.net.SocketException;

    /**
    * Native Messaging Bridge Server that runs as a separate thread inside the main GUI application.
    * It opens a ServerSocket and listens for connections from the NativeRelay client.
    */
    public class BridgeHost implements Runnable {

        private static CommandRouter commandRouter;
        private volatile boolean isRunning = true;
        private ServerSocket serverSocket;
        public static final int PORT = 12345; // The port our server listens on

        public BridgeHost(CommandRouter router) {
            commandRouter = router;
        }

        /**
        * The main entry point for the bridge server thread.
        */
        @Override
        public void run() {
            try {
                // Bind to localhost only for better security
                serverSocket = new ServerSocket(PORT, 50, java.net.InetAddress.getByName("127.0.0.1"));
                System.out.println("BridgeHost Server started, listening on port " + PORT);

                while (isRunning) {
                    try {
                        // This blocks until a client connects (e.g., our NativeRelay)
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("BridgeHost Server: Client connected from " + clientSocket.getInetAddress());

                        // Handle the client connection in a new thread
                        ClientHandler clientHandler = new ClientHandler(clientSocket, commandRouter);
                        Thread clientThread = new Thread(clientHandler, "RapidCipher-ClientHandler-Thread");
                        clientThread.setDaemon(true);
                        clientThread.start();

                    } catch (SocketException e) {
                        if (!isRunning) {
                            System.out.println("BridgeHost Server: ServerSocket closed, shutting down.");
                        } else {
                            System.err.println("BridgeHost Server: SocketException: " + e.getMessage());
                        }
                    } catch (IOException e) {
                        if (isRunning) {
                            System.err.println("BridgeHost Server: I/O error on accept: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("BridgeHost Server: Could not start server on port " + PORT + ". " + e.getMessage());
                    // Optionally, notify the GUI
                }
            } finally {
                stop(); // Ensure resources are cleaned up
                System.out.println("BridgeHost Server thread shutting down.");
            }
        }

        /**
        * Signals the bridge thread to stop and closes the ServerSocket to interrupt
        * the blocking accept() call, allowing the thread to terminate.
        */
        public void stop() {
            isRunning = false;
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.err.println("BridgeHost Server: Error closing ServerSocket: " + e.getMessage());
            }
        }
    }

