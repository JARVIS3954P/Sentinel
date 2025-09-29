package org.jarvis.enforcer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class FirewallManager {

    /**
     * Blocks an IP address by adding a DROP rule to the INPUT chain in iptables.
     * @param ipAddress The IP address to block.
     * @return true if the command was executed successfully, false otherwise.
     */
    public boolean blockIp(String ipAddress) {
        System.out.println("Attempting to block IP: " + ipAddress);
        // We use -I (insert) instead of -A (append) to put our rule at the top.
        // This ensures our rule is checked before any general ALLOW rules.
        return executeIptablesCommand("-I", "INPUT", "-s", ipAddress, "-j", "DROP");
    }

    /**
     * Unblocks an IP address by deleting the corresponding DROP rule.
     * @param ipAddress The IP address to unblock.
     * @return true if the command was executed successfully, false otherwise.
     */
    public boolean unblockIp(String ipAddress) {
        System.out.println("Attempting to unblock IP: " + ipAddress);
        return executeIptablesCommand("-D", "INPUT", "-s", ipAddress, "-j", "DROP");
    }

    private boolean executeIptablesCommand(String... commandArgs) {
        try {
            // We must prepend "sudo" and "iptables" to the command arguments.
            String[] fullCommand = new String[commandArgs.length + 2];
            fullCommand[0] = "sudo";
            fullCommand[1] = "iptables";
            System.arraycopy(commandArgs, 0, fullCommand, 2, commandArgs.length);

            ProcessBuilder pb = new ProcessBuilder(fullCommand);
            Process process = pb.start();

            // We should wait for the command to finish and check its exit code.
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("iptables command executed successfully.");
                return true;
            } else {
                // If there was an error, print the error stream for debugging.
                System.err.println("iptables command failed with exit code: " + exitCode);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.err.println(line);
                    }
                }
                return false;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to execute iptables command.");
            e.printStackTrace();
            return false;
        }
    }
}