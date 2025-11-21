package org.jarvis.enforcer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

public class HostsFileManager {

    private static final String HOSTS_FILE_PATH = "/etc/hosts";
    private static final String BACKUP_FILE_PATH = "/etc/hosts.sentinel_backup";
    private static final String SENTINEL_MARKER = "# SENTINEL_BLOCK";

    public HostsFileManager() {
        createBackup();
    }

    private void createBackup() {
        try {
            Path original = Paths.get(HOSTS_FILE_PATH);
            Path backup = Paths.get(BACKUP_FILE_PATH);
            if (!Files.exists(backup)) {
                Files.copy(original, backup, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Successfully created backup of hosts file.");
            }
        } catch (IOException e) {
            System.err.println("CRITICAL: Failed to create hosts file backup. Aborting hosts file operations.");
            e.printStackTrace();
        }
    }

    public void addDomainBlock(String domain) {
        String ruleLine = "127.0.0.1 " + domain + " " + SENTINEL_MARKER;
        try {
            List<String> lines = Files.readAllLines(Paths.get(HOSTS_FILE_PATH));
            // Check for the exact line to prevent duplicates
            if (lines.stream().noneMatch(line -> line.trim().equals(ruleLine.trim()))) {
                lines.add(ruleLine);
                Files.write(Paths.get(HOSTS_FILE_PATH), lines);
                System.out.println("Added to hosts file: " + domain);
            }
        } catch (IOException e) {
            System.err.println("Failed to add domain to hosts file. Ensure you have sudo privileges.");
            e.printStackTrace();
        }
    }

    public void removeDomainBlock(String domain) {
        try {
            Path hostsPath = Paths.get(HOSTS_FILE_PATH);
            List<String> lines = Files.readAllLines(hostsPath);

            // This is more robust: it removes any Sentinel-marked line containing the specific domain.
            List<String> updatedLines = lines.stream()
                    .filter(line -> !(line.contains(SENTINEL_MARKER) && line.contains(" " + domain)))
                    .collect(Collectors.toList());

            Files.write(hostsPath, updatedLines);
            System.out.println("Removed from hosts file: " + domain);
        } catch (IOException e) {
            System.err.println("Failed to remove domain from hosts file.");
            e.printStackTrace();
        }
    }

    /**
     * NEW METHOD: Efficiently removes all lines added by Sentinel in one operation.
     */
    public void removeAllSentinelBlocks() {
        try {
            Path hostsPath = Paths.get(HOSTS_FILE_PATH);
            List<String> lines = Files.readAllLines(hostsPath);

            // Keep only the lines that DO NOT have our marker.
            List<String> updatedLines = lines.stream()
                    .filter(line -> !line.contains(SENTINEL_MARKER))
                    .collect(Collectors.toList());

            Files.write(hostsPath, updatedLines);
            System.out.println("Removed all Sentinel-managed blocks from hosts file.");
        } catch (IOException e) {
            System.err.println("Failed to remove all blocks from hosts file.");
            e.printStackTrace();
        }
    }

    public void restoreAndCleanup() {
        try {
            Path original = Paths.get(HOSTS_FILE_PATH);
            Path backup = Paths.get(BACKUP_FILE_PATH);
            if (Files.exists(backup)) {
                Files.copy(backup, original, StandardCopyOption.REPLACE_EXISTING);
                Files.delete(backup);
                System.out.println("Successfully restored hosts file from backup and cleaned up.");
            }
        } catch (IOException e) {
            System.err.println("CRITICAL: Failed to restore hosts file from backup.");
            e.printStackTrace();
        }
    }
}