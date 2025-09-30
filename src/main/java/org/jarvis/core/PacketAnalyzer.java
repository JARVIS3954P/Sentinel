package org.jarvis.core;

import org.jarvis.enforcer.FirewallManager;
import org.jarvis.model.Rule;
import org.jarvis.persistence.DatabaseManager;
import org.pcap4j.core.PacketListener;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.jarvis.ui.MainViewController;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class PacketAnalyzer implements PacketListener {

    private final FirewallManager firewallManager;
    private final DatabaseManager databaseManager;
    private final Set<String> ruleValues; // The set of IP addresses to block
    private final Set<String> alreadyBlocked;
    private final MainViewController uiController;

    public PacketAnalyzer(MainViewController uiController) {
        this.uiController = uiController;
        this.firewallManager = new FirewallManager();
        this.databaseManager = new DatabaseManager();
        this.alreadyBlocked = Collections.synchronizedSet(new HashSet<>());

        // Load rules from the database on startup
        Set<Rule> activeRules = databaseManager.getAllActiveRules();
        this.ruleValues = activeRules.stream()
                .map(Rule::getValue)
                .collect(Collectors.toSet());

        System.out.println("Analyzer initialized with rules from database: " + ruleValues);
    }

    @Override
    public void gotPacket(Packet packet) {
        IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
        if (ipV4Packet == null) {
            return;
        }

        String srcIp = ipV4Packet.getHeader().getSrcAddr().getHostAddress();

        if (ruleValues.contains(srcIp)) {
            if (alreadyBlocked.add(srcIp)) { // .add() returns true if the element was new
                String logMsg = "!!! MATCH FOUND !!! Blocking source IP: " + srcIp;
                System.out.println(logMsg);

                // Send the message to the UI
                if (uiController != null) {
                    uiController.addLogMessage(logMsg);
                }

                firewallManager.blockIp(srcIp);

                // Log the block event to the database
                String details = "Blocked incoming packet from source IP: " + srcIp;
                databaseManager.logEvent("IP_BLOCKED", details);
            }
        }
    }

    public void addRule(String ruleValue) {
        ruleValues.add(ruleValue);
        System.out.println("New rule added to analyzer: " + ruleValue);
    }

    public void removeRule(String ruleValue) {
        ruleValues.remove(ruleValue);
        alreadyBlocked.remove(ruleValue); // Allow it to be blocked again if re-added
        System.out.println("Rule removed from analyzer: " + ruleValue);
    }

    // Getter to expose the FirewallManager to other parts of the application
    public FirewallManager getFirewallManager() {
        return this.firewallManager;
    }
}