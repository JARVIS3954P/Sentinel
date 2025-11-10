package org.jarvis.enforcer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FirewallManager {

    // A simple way to check if an address is IPv6
    private boolean isIPv6(String ipAddress) {
        return ipAddress.contains(":");
    }

    public void blockIp(String ipAddress, String direction) {
        String command = isIPv6(ipAddress) ? "ip6tables" : "iptables";
        System.out.println("Attempting to block IP (" + command + "): " + ipAddress + " for direction: " + direction);

        if ("Incoming".equalsIgnoreCase(direction)) {
            executeFirewallCommand(command, "-I", "INPUT", "-s", ipAddress, "-j", "DROP");
        } else if ("Outgoing".equalsIgnoreCase(direction)) {
            executeFirewallCommand(command, "-I", "OUTPUT", "-d", ipAddress, "-j", "DROP");
        } else if ("Both".equalsIgnoreCase(direction)) {
            executeFirewallCommand(command, "-I", "INPUT", "-s", ipAddress, "-j", "DROP");
            executeFirewallCommand(command, "-I", "OUTPUT", "-d", ipAddress, "-j", "DROP");
        }
    }

    public void unblockIp(String ipAddress, String direction) {
        String command = isIPv6(ipAddress) ? "ip6tables" : "iptables";
        System.out.println("Attempting to unblock IP (" + command + "): " + ipAddress + " for direction: " + direction);

        if ("Incoming".equalsIgnoreCase(direction)) {
            executeFirewallCommand(command, "-D", "INPUT", "-s", ipAddress, "-j", "DROP");
        } else if ("Outgoing".equalsIgnoreCase(direction)) {
            executeFirewallCommand(command, "-D", "OUTPUT", "-d", ipAddress, "-j", "DROP");
        } else if ("Both".equalsIgnoreCase(direction)) {
            executeFirewallCommand(command, "-D", "INPUT", "-s", ipAddress, "-j", "DROP");
            executeFirewallCommand(command, "-D", "OUTPUT", "-d", ipAddress, "-j", "DROP");
        }
    }

    private boolean executeFirewallCommand(String firewallCommand, String... commandArgs) {
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add("sudo");
        fullCommand.add(firewallCommand);
        for (String arg : commandArgs) {
            fullCommand.add(arg);
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(fullCommand);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Firewall command executed successfully: " + String.join(" ", fullCommand));
                return true;
            } else {
                System.err.println("Firewall command failed with exit code " + exitCode + ": " + String.join(" ", fullCommand));
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.err.println(line);
                    }
                }
                return false;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to execute firewall command: " + String.join(" ", fullCommand));
            e.printStackTrace();
            return false;
        }
    }
}