package org.jarvis.persistence;

import org.jarvis.model.Rule;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:firewall.db";

    public DatabaseManager() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        // SQL statement for creating the rules table
        String createRulesTableSql = "CREATE TABLE IF NOT EXISTS rules ("
                + " id integer PRIMARY KEY AUTOINCREMENT,"
                + " type text NOT NULL,"
                + " value text NOT NULL UNIQUE,"
                + " enabled boolean NOT NULL"
                + ");";

        // SQL statement for creating the logs table
        String createLogsTableSql = "CREATE TABLE IF NOT EXISTS logs ("
                + " id integer PRIMARY KEY AUTOINCREMENT,"
                + " timestamp text NOT NULL,"
                + " event_type text NOT NULL,"
                + " details text NOT NULL"
                + ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            // Create the tables if they don't exist
            stmt.execute(createRulesTableSql);
            stmt.execute(createLogsTableSql);
            System.out.println("Database initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    public Set<Rule> getAllActiveRules() {
        Set<Rule> rules = new HashSet<>();
        String sql = "SELECT id, type, value, enabled FROM rules WHERE enabled = true";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                rules.add(new Rule(
                        rs.getInt("id"),
                        rs.getString("type"),
                        rs.getString("value"),
                        rs.getBoolean("enabled")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching active rules: " + e.getMessage());
        }
        return rules;
    }

    public void addRule(String type, String value, boolean enabled) {
        String sql = "INSERT OR IGNORE INTO rules(type, value, enabled) VALUES(?,?,?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type);
            pstmt.setString(2, value);
            pstmt.setBoolean(3, enabled);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding rule: " + e.getMessage());
        }
    }

    public void logEvent(String eventType, String details) {
        String sql = "INSERT INTO logs(timestamp, event_type, details) VALUES(datetime('now'),?,?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, eventType);
            pstmt.setString(2, details);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error logging event: " + e.getMessage());
        }
    }

    public void deleteRule(int id) {
        String sql = "DELETE FROM rules WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Set the corresponding parameter
            pstmt.setInt(1, id);
            // Execute the delete statement
            pstmt.executeUpdate();
            System.out.println("Rule with id " + id + " deleted successfully.");

        } catch (SQLException e) {
            System.err.println("Error deleting rule: " + e.getMessage());
        }
    }
}