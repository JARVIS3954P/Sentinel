package org.jarvis.enforcer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
        // Run this in a privileged context
        String ruleLine = "127.0.0.1 " + domain + " " + SENTINEL_MARKER;
        try {
            // First, check if the rule already exists to prevent duplicates
            List<String> lines = Files.readAllLines(Paths.get(HOSTS_FILE_PATH));
            if (lines.stream().noneMatch(line -> line.trim().equals(ruleLine.trim()))) {
                try (FileWriter fw = new FileWriter(HOSTS_FILE_PATH, true);
                     BufferedWriter bw = new BufferedWriter(fw)) {
                    bw.newLine();
                    bw.write(ruleLine);
                }
                System.out.println("Added to hosts file: " + domain);
            }
        } catch (IOException e) {
            System.err.println("Failed to add domain to hosts file. Ensure you have sudo privileges.");
            e.printStackTrace();
        }
    }

    public void removeDomainBlock(String domain) {
        try {
            File inputFile = new File(HOSTS_FILE_PATH);
            List<String> lines = Files.readAllLines(inputFile.toPath());
            List<String> updatedLines = lines.stream()
                    .filter(line -> !line.contains("127.0.0.1 " + domain))
                    .collect(Collectors.toList());
            Files.write(inputFile.toPath(), updatedLines);
            System.out.println("Removed from hosts file: " + domain);
        } catch (IOException e) {
            System.err.println("Failed to remove domain from hosts file.");
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