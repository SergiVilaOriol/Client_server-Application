import java.io.IOException;

public class Run_simulation {
    public static void main(String[] args) {
        // Create a new instance of the UDPClient
        Thread clientThread = new Thread(() -> {
            try {
                UDPClient.main(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        clientThread.start();

        // Wait for a brief moment to allow the client to initialize
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Start the server
        Thread serverThread = new Thread(() -> {
            try {
                UDPServer.main(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
    }
}