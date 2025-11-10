package org.jarvis.core;

import org.jarvis.enforcer.FirewallManager;
import org.jarvis.model.Rule;
import org.jarvis.persistence.DatabaseManager;
import org.jarvis.ui.MainViewController;
import org.pcap4j.core.PacketListener;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.Packet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PacketAnalyzer implements PacketListener {

    private final FirewallManager firewallManager;
    private final DatabaseManager databaseManager;
    private final MainViewController uiController;
    private final Map<String, Rule> rulesMap;
    private final Set<String> alreadyBlocked;

    public PacketAnalyzer(MainViewController uiController) {
        this.uiController = uiController;
        this.firewallManager = new FirewallManager();
        this.databaseManager = new DatabaseManager();
        this.alreadyBlocked = Collections.synchronizedSet(new HashSet<>());

        Set<Rule> activeRules = databaseManager.getAllActiveRules();
        this.rulesMap = activeRules.stream()
                .collect(Collectors.toConcurrentMap(Rule::getValue, rule -> rule));

        System.out.println("Analyzer initialized with " + rulesMap.size() + " rules from database.");
    }

    @Override
    public void gotPacket(Packet packet) {
        String srcIp = null;
        String dstIp = null;

        // Check for IPv4
        IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
        if (ipV4Packet != null) {
            srcIp = ipV4Packet.getHeader().getSrcAddr().getHostAddress();
            dstIp = ipV4Packet.getHeader().getDstAddr().getHostAddress();
        } else {
            // If not IPv4, check for IPv6
            IpV6Packet ipV6Packet = packet.get(IpV6Packet.class);
            if (ipV6Packet != null) {
                srcIp = ipV6Packet.getHeader().getSrcAddr().getHostAddress();
                dstIp = ipV6Packet.getHeader().getDstAddr().getHostAddress();
            }
        }

        if (srcIp != null && dstIp != null) {
            // Check for incoming traffic
            checkAndBlock(srcIp, "Incoming");
            // Check for outgoing traffic
            checkAndBlock(dstIp, "Outgoing");
        }
    }

    private void checkAndBlock(String ip, String direction) {
        if (rulesMap.containsKey(ip)) {
            Rule matchedRule = rulesMap.get(ip);
            String ruleDirection = matchedRule.getDirection();

            if (direction.equalsIgnoreCase(ruleDirection) || "Both".equalsIgnoreCase(ruleDirection)) {
                String blockKey = ip + ":" + direction;
                if (alreadyBlocked.add(blockKey)) {
                    String logMsg = String.format("!!! MATCH FOUND !!! Blocking %s traffic for IP: %s", direction, ip);
                    System.out.println(logMsg);
                    if (uiController != null) {
                        uiController.addLogMessage(logMsg);
                    }
                    firewallManager.blockIp(ip, direction);
                    // Update logEvent call to include the direction
                    databaseManager.logEvent("IP_BLOCKED", direction, "Blocked incoming packet from source IP: " + ip);
                }
            }
        }
    }

    public void addRule(Rule rule) {
        rulesMap.put(rule.getValue(), rule);
        System.out.println("New rule added to analyzer: " + rule.getValue());
    }

    public void removeRule(String ruleValue) {
        rulesMap.remove(ruleValue);
        alreadyBlocked.remove(ruleValue + ":Incoming");
        alreadyBlocked.remove(ruleValue + ":Outgoing");
        System.out.println("Rule removed from analyzer: " + ruleValue);
    }

    public FirewallManager getFirewallManager() {
        return this.firewallManager;
    }
}