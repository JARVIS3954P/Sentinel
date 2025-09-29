package org.jarvis;

import org.jarvis.core.PacketListenerService;
import org.jarvis.persistence.DatabaseManager; // Import this

public class Main {

    public static void main(String[] args) {
        // --- TEMPORARY: Add a rule to the database for testing ---
        DatabaseManager dbManager = new DatabaseManager();
        dbManager.addRule("IP_BLOCK", "142.250.193.42", true);
        System.out.println("Test rule added to database.");
        // --- END TEMPORARY ---

        PacketListenerService service = new PacketListenerService();
        service.start();

        Runtime.getRuntime().addShutdownHook(new Thread(service::stop));

        System.out.println("Press Ctrl+C to stop the application.");
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}