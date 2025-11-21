package org.jarvis.enforcer;

import java.io.IOException;

public class SystemManager {

    /**
     * Attempts to flush the system's DNS cache by trying several common commands.
     * This makes changes to the /etc/hosts file take effect immediately.
     */
    public void flushDnsCache() {
        System.out.println("Attempting to flush system DNS cache...");
        // This command is for systems using systemd-resolved (modern Ubuntu/Debian/Arch)
        executeCommand("sudo", "systemd-resolve", "--flush-caches");

        // This command is for systems using nscd (older systems)
        executeCommand("sudo", "nscd", "-i", "hosts");

        // This command is for systems using dnsmasq
        executeCommand("sudo", "systemctl", "restart", "dnsmasq.service");
    }

    private void executeCommand(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Successfully executed: " + String.join(" ", command));
            } else {
                // This is not a critical error, as one of the other commands might have worked.
                System.out.println("Command failed or not applicable: " + String.join(" ", command));
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error executing command: " + String.join(" ", command));
        }
    }
}