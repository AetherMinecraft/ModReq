package dev.bwmp.modReq.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import dev.bwmp.modReq.ModReq;

public class DatabaseMigrations {

    private final ModReq plugin;
    private final DatabaseManager databaseManager;

    private static final int CURRENT_SCHEMA_VERSION = 3;

    public DatabaseMigrations(ModReq plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public void checkAndMigrate() throws SQLException {
        createSchemaVersionTable();

        int currentVersion = getCurrentSchemaVersion();
        plugin.getLogger().info("Current database schema version: " + currentVersion);

        if (currentVersion < CURRENT_SCHEMA_VERSION) {
            plugin.getLogger()
                    .info("Migrating database from version " + currentVersion + " to " + CURRENT_SCHEMA_VERSION);
            performMigrations(currentVersion);
            updateSchemaVersion(CURRENT_SCHEMA_VERSION);
            plugin.getLogger().info("Database migration completed successfully");
        }
    }

    private void createSchemaVersionTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS schema_version (
                    version INT NOT NULL,
                    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try (Connection conn = databaseManager.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);

            try (PreparedStatement checkStmt = conn.prepareStatement("SELECT COUNT(*) FROM schema_version")) {
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    try (PreparedStatement insertStmt = conn
                            .prepareStatement("INSERT INTO schema_version (version) VALUES (0)")) {
                        insertStmt.executeUpdate();
                    }
                }
            }
        }
    }

    private int getCurrentSchemaVersion() throws SQLException {
        String sql = "SELECT MAX(version) FROM schema_version";

        try (Connection conn = databaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    private void updateSchemaVersion(int version) throws SQLException {
        String sql = "INSERT INTO schema_version (version) VALUES (?)";

        try (Connection conn = databaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, version);
            stmt.executeUpdate();
        }
    }

    private void performMigrations(int fromVersion) throws SQLException {
        for (int version = fromVersion + 1; version <= CURRENT_SCHEMA_VERSION; version++) {
            plugin.getLogger().info("Applying migration for version " + version);

            switch (version) {
                case 1 -> migrateToVersion1();
                case 2 -> migrateToVersion2();
                case 3 -> migrateToVersion3();
                default -> plugin.getLogger().warning("Unknown migration version: " + version);
            }
        }
    }

    private void migrateToVersion1() throws SQLException {
        plugin.getLogger().info("Migration to version 1: Initial schema creation completed");
    }

    /**
     * Migration to version 2: Convert CLAIMED status to OPEN and add performance indices
     */
    private void migrateToVersion2() throws SQLException {
        try (Connection conn = databaseManager.getConnection();
                Statement stmt = conn.createStatement()) {

            // Convert any CLAIMED status requests to OPEN (claiming is now independent of status)
            int updatedRows = stmt.executeUpdate("UPDATE mod_requests SET status = 'OPEN' WHERE status = 'CLAIMED'");
            if (updatedRows > 0) {
                plugin.getLogger().info("Converted " + updatedRows + " CLAIMED requests to OPEN status");
            }

            // Add performance indices
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_requests_updated_at ON mod_requests(updated_at)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_notes_created_at ON mod_request_notes(created_at)");

            plugin.getLogger().info("Migration to version 2: Converted CLAIMED status and added performance indices");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to migrate to version 2", e);
            throw e;
        }
    }

    /**
     * Migration to version 3: Add columns to track who closed/completed requests
     */
    private void migrateToVersion3() throws SQLException {
        try (Connection conn = databaseManager.getConnection();
                Statement stmt = conn.createStatement()) {

            // Add columns to track who closed/completed requests
            stmt.executeUpdate("ALTER TABLE mod_requests ADD COLUMN IF NOT EXISTS closed_by VARCHAR(36)");
            stmt.executeUpdate("ALTER TABLE mod_requests ADD COLUMN IF NOT EXISTS closed_by_name VARCHAR(16)");
            stmt.executeUpdate("ALTER TABLE mod_requests ADD COLUMN IF NOT EXISTS completed_by VARCHAR(36)");
            stmt.executeUpdate("ALTER TABLE mod_requests ADD COLUMN IF NOT EXISTS completed_by_name VARCHAR(16)");

            // Add indices for the new columns
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_requests_closed_by ON mod_requests(closed_by)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_requests_completed_by ON mod_requests(completed_by)");

            plugin.getLogger().info("Migration to version 3: Added tracking for who closed/completed requests");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to migrate to version 3", e);
            throw e;
        }
    }
}
