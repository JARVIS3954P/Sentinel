package org.jarvis;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jarvis.core.PacketListenerService;
import org.jarvis.ui.MainViewController;

import java.io.IOException;

public class App extends Application {

    private PacketListenerService packetListenerService;

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load the FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/jarvis/ui/main-view.fxml"));
        Parent root = loader.load();

        // Get the controller instance from the loader
        MainViewController controller = loader.getController();

        // Create the service and pass the controller to it
        packetListenerService = new PacketListenerService(controller);

        // Start the backend service on a separate thread
        Thread serviceThread = new Thread(() -> packetListenerService.start());
        serviceThread.setDaemon(true); // This allows the JVM to exit if only daemon threads are running
        serviceThread.start();

        primaryStage.setTitle("Sentinel Firewall");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    @Override
    public void stop() {
        // This method is called when the UI is closed.
        // We must ensure our background service is stopped gracefully.
        System.out.println("Application is shutting down...");
        if (packetListenerService != null) {
            packetListenerService.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}