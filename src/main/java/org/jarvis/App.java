package org.jarvis;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jarvis.core.PacketListenerService;
import org.jarvis.enforcer.BlockHttpServer;
import org.jarvis.enforcer.HostsFileManager;
import org.jarvis.ui.MainViewController;

import java.io.IOException;

public class App extends Application {

    private PacketListenerService packetListenerService;
    private BlockHttpServer blockHttpServer;
    private HostsFileManager hostsFileManager;

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Initialize helpers
        blockHttpServer = new BlockHttpServer();
        hostsFileManager = new HostsFileManager();

        blockHttpServer.start();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/jarvis/ui/main-view.fxml"));
        Parent root = loader.load();

        MainViewController controller = loader.getController();

        // Pass the new hosts file manager to the controller
        controller.setHostsFileManager(hostsFileManager);

        packetListenerService = new PacketListenerService(controller);
        controller.setPacketListenerService(packetListenerService);

        Thread serviceThread = new Thread(() -> packetListenerService.start());
        serviceThread.setDaemon(true);
        serviceThread.start();

        primaryStage.setTitle("Sentinel Firewall");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    @Override
    public void stop() {
        System.out.println("Application is shutting down...");
        if (packetListenerService != null) {
            packetListenerService.stop();
        }
        if (blockHttpServer != null) {
            blockHttpServer.stop();
        }
        if (hostsFileManager != null) {
            // Restore the hosts file to its original state on exit
            hostsFileManager.restoreAndCleanup();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}