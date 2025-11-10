package org.jarvis.persistence;

import org.jarvis.model.Rule;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:firewall.db";

    public DatabaseManager() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        String createRulesTableSql = "CREATE TABLE IF NOT EXISTS rules ("
                + " id integer PRIMARY KEY AUTOINCREMENT,"
                + " type text NOT NULL,"
                + " value text NOT NULL UNIQUE,"
                + " direction text NOT NULL DEFAULT 'Outgoing',"
                + " enabled boolean NOT NULL"
                + ");";

        String createLogsTableSql = "CREATE TABLE IF NOT EXISTS logs ("
                + " id integer PRIMARY KEY AUTOINCREMENT,"
                + " timestamp text NOT NULL,"
                + " event_type text NOT NULL,"
                + " direction text," // New column for analytics
                + " details text NOT NULL"
                + ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createRulesTableSql);
            stmt.execute(createLogsTableSql);
        } catch (SQLException e) {
            System.err.println("Error creating database tables: " + e.getMessage());
        }

        // Add columns if they don't exist in an old DB file
        addColumnIfNotExists("rules", "direction", "TEXT NOT NULL DEFAULT 'Outgoing'");
        addColumnIfNotExists("logs", "direction", "TEXT");
    }

    private void addColumnIfNotExists(String tableName, String columnName, String columnDefinition) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            DatabaseMetaData md = conn.getMetaData();
            ResultSet rs = md.getColumns(null, null, tableName, columnName);
            if (!rs.next()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
                    System.out.println("Added column '" + columnName + "' to table '" + tableName + "'.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking or adding column: " + e.getMessage());
        }
    }

    public Set<Rule> getAllActiveRules() {
        Set<Rule> rules = new HashSet<>();
        String sql = "SELECT id, type, value, direction, enabled FROM rules WHERE enabled = true";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                rules.add(new Rule(
                        rs.getInt("id"),
                        rs.getString("type"),
                        rs.getString("value"),
                        rs.getString("direction"),
                        rs.getBoolean("enabled")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching active rules: " + e.getMessage());
        }
        return rules;
    }

    public void addRule(String type, String value, String direction, boolean enabled) {
        String sql = "INSERT OR IGNORE INTO rules(type, value, direction, enabled) VALUES(?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type);
            pstmt.setString(2, value);
            pstmt.setString(3, direction);
            pstmt.setBoolean(4, enabled);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding rule: " + e.getMessage());
        }
    }

    public void deleteRule(int id) {
        String sql = "DELETE FROM rules WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            System.out.println("Rule with id " + id + " deleted successfully.");
        } catch (SQLException e) {
            System.err.println("Error deleting rule: " + e.getMessage());
        }
    }

    public void logEvent(String eventType, String direction, String details) {
        String sql = "INSERT INTO logs(timestamp, event_type, direction, details) VALUES(datetime('now'),?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, eventType);
            pstmt.setString(2, direction); // Save the direction
            pstmt.setString(3, details);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error logging event: " + e.getMessage());
        }
    }

    public void deleteAllRules() {
        String sql = "DELETE FROM rules";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("All rules have been deleted from the database.");
            // Optional: Reset the auto-increment counter for a clean slate.
            stmt.execute("DELETE FROM sqlite_sequence WHERE name='rules'");
        } catch (SQLException e) {
            System.err.println("Error deleting all rules: " + e.getMessage());
        }
    }

    public Map<String, Integer> getTopBlockedIPs(int limit) {
        Map<String, Integer> topIps = new HashMap<>();
        // This query extracts the IP from the details string, groups by it, and counts occurrences.
        String sql = "SELECT substr(details, instr(details, ': ') + 2) as ip, COUNT(*) as count "
                + "FROM logs WHERE event_type = 'IP_BLOCKED' "
                + "GROUP BY ip ORDER BY count DESC LIMIT ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                topIps.put(rs.getString("ip"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting top blocked IPs: " + e.getMessage());
        }
        return topIps;
    }

    public Map<String, Integer> getBlockedTrafficByDirection() {
        Map<String, Integer> directionCounts = new HashMap<>();
        String sql = "SELECT direction, COUNT(*) as count FROM logs "
                + "WHERE event_type = 'IP_BLOCKED' GROUP BY direction";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String direction = rs.getString("direction");
                if (direction != null) {
                    directionCounts.put(direction, rs.getInt("count"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting blocked traffic by direction: " + e.getMessage());
        }
        return directionCounts;
    }

    public int getTotalBlockedCount() {
        String sql = "SELECT COUNT(*) FROM logs WHERE event_type = 'IP_BLOCKED'";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting total blocked count: " + e.getMessage());
        }
        return 0;
    }
}