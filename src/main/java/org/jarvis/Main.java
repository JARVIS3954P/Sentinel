package org.jarvis;

import org.jarvis.core.PacketListenerService;

public class Main {

    public static void main(String[] args) {
        PacketListenerService service = new PacketListenerService();
        service.start();

        // Add a shutdown hook to ensure the service is stopped gracefully
        // This is important for closing the Pcap handle properly.
        Runtime.getRuntime().addShutdownHook(new Thread(service::stop));

        // Keep the main thread alive. In a real UI app, the UI thread does this.
        // For our command-line test, we can just sleep or wait for user input.
        System.out.println("Press Ctrl+C to stop the application.");
        while (true) {
            try {
                Thread.sleep(10000); // Sleep indefinitely
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}