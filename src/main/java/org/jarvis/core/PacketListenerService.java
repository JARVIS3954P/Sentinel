package org.jarvis.core;

import org.jarvis.enforcer.FirewallManager;
import org.pcap4j.core.*;
import org.jarvis.ui.MainViewController;

public class PacketListenerService {

    private PcapHandle handle;
    private Thread listenerThread;
    private MainViewController uiController;
    private PacketAnalyzer analyzer;

    public PacketListenerService(MainViewController uiController) {
        this.uiController = uiController;
    }

    public PacketAnalyzer getAnalyzer() {
        return this.analyzer;
    }

    public void start() {
        System.out.println("Starting Packet Listener Service...");
        try {
            // 1. Find the network interface
            PcapNetworkInterface nif = findNetworkInterface();
            if (nif == null) {
                System.err.println("No suitable network interface found. Exiting.");
                return;
            }
            System.out.println("Listening on: " + nif.getName());

            // 2. Open the handle
            this.handle = openPcapHandle(nif);

            // 3. Create the analyzer and start the listening loop on a new thread
            this.analyzer = new PacketAnalyzer(this.uiController);
            listenerThread = new Thread(() -> {
                try {
                    // A value of -1 means loop indefinitely
                    handle.loop(-1, analyzer);
                } catch (PcapNativeException | InterruptedException | NotOpenException e) {
                    System.err.println("Packet listening loop was interrupted.");
                    e.printStackTrace();
                }
            });

            listenerThread.start();
            System.out.println("Service started successfully. Listening for packets...");

        } catch (PcapNativeException e) {
            System.err.println("Failed to start packet listener service.");
            e.printStackTrace();
        }
    }

    public void stop() {
        if (handle != null && handle.isOpen()) {
            try {
                handle.breakLoop();
                System.out.println("Stopping packet listener...");
            } catch (NotOpenException e) {
                // Ignore, already closed.
            }
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
            try {
                listenerThread.join(1000); // Wait for the thread to die
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (handle != null && handle.isOpen()){
            handle.close();
        }
        System.out.println("Service stopped.");
    }

    private PcapNetworkInterface findNetworkInterface() throws PcapNativeException {
        return Pcaps.findAllDevs().stream()
                .filter(dev -> !dev.isLoopBack())
                .findFirst()
                .orElse(null);
    }

    private PcapHandle openPcapHandle(PcapNetworkInterface nif) throws PcapNativeException {
        int snapLen = 65536;
        PcapNetworkInterface.PromiscuousMode mode = PcapNetworkInterface.PromiscuousMode.PROMISCUOUS;
        int timeout = 1000; // 1 second
        return nif.openLive(snapLen, mode, timeout);
    }

    // Getter to expose the FirewallManager to the UI controller
    public FirewallManager getFirewallManager() {
        if (analyzer != null) {
            return analyzer.getFirewallManager();
        }
        return null; // Or handle this case appropriately
    }
}