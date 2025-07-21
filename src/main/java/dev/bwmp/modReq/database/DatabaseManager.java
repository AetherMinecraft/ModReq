package dev.bwmp.modReq.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import dev.bwmp.modReq.ModReq;
import dev.bwmp.modReq.model.ModRequest;
import dev.bwmp.modReq.model.ModRequestNote;
import dev.bwmp.modReq.model.ModRequestStatus;

public class DatabaseManager {

    private final ModReq plugin;
    private HikariDataSource dataSource;
    private final String databaseType;

    public DatabaseManager(ModReq plugin) {
        this.plugin = plugin;
        this.databaseType = plugin.getConfigManager().getString("database.type", "h2").toLowerCase();
    }

    public void initialize() {
        try {
            setupDataSource();
            createTables();

            DatabaseMigrations migrations = new DatabaseMigrations(plugin, this);
            migrations.checkAndMigrate();

            plugin.getLogger().info("Database initialized successfully using " + databaseType.toUpperCase());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private void setupDataSource() {
        HikariConfig config = new HikariConfig();

        switch (databaseType) {
            case "h2" -> {
                String dbFile = plugin.getConfigManager().getString("database.file", "modreq.db");
                String jdbcUrl = "jdbc:h2:file:" + plugin.getDataFolder().getAbsolutePath() + "/" + dbFile
                        + ";MODE=MySQL;AUTO_RECONNECT=TRUE";
                config.setJdbcUrl(jdbcUrl);
                config.setDriverClassName("org.h2.Driver");
                config.setUsername("sa");
                config.setPassword("");
            }
            case "mysql" -> {
                String host = plugin.getConfigManager().getString("database.host", "localhost");
                int port = plugin.getConfigManager().getInt("database.port", 3306);
                String database = plugin.getConfigManager().getString("database.database", "modreq");
                String username = plugin.getConfigManager().getString("database.username", "root");
                String password = plugin.getConfigManager().getString("database.password", "");

                String jdbcUrl = String.format(
                        "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&autoReconnect=true",
                        host, port, database);
                config.setJdbcUrl(jdbcUrl);
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                config.setUsername(username);
                config.setPassword(password);
            }
            default -> throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        }

        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("ModReq-CP");

        config.setConnectionTestQuery("SELECT 1");

        this.dataSource = new HikariDataSource(config);
    }

    private void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String createRequestsTable = """
                    CREATE TABLE IF NOT EXISTS mod_requests (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        player_id VARCHAR(36) NOT NULL,
                        player_name VARCHAR(16) NOT NULL,
                        description TEXT NOT NULL,
                        status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                        claimed_by VARCHAR(36),
                        claimed_by_name VARCHAR(16),
                        closed_by VARCHAR(36),
                        closed_by_name VARCHAR(16),
                        completed_by VARCHAR(36),
                        completed_by_name VARCHAR(16),
                        world_name VARCHAR(50),
                        x DOUBLE,
                        y DOUBLE,
                        z DOUBLE,
                        yaw FLOAT,
                        pitch FLOAT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        closed_at TIMESTAMP NULL,
                        INDEX idx_player_id (player_id),
                        INDEX idx_status (status),
                        INDEX idx_claimed_by (claimed_by),
                        INDEX idx_closed_by (closed_by),
                        INDEX idx_completed_by (completed_by),
                        INDEX idx_created_at (created_at),
                        INDEX idx_updated_at (updated_at)
                    )
                    """;

            String createNotesTable = """
                    CREATE TABLE IF NOT EXISTS mod_request_notes (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        request_id INT NOT NULL,
                        author_id VARCHAR(36) NOT NULL,
                        author_name VARCHAR(16) NOT NULL,
                        content TEXT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (request_id) REFERENCES mod_requests(id) ON DELETE CASCADE,
                        INDEX idx_request_id (request_id),
                        INDEX idx_author_id (author_id)
                    )
                    """;

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(createRequestsTable);
                stmt.executeUpdate(createNotesTable);
            }
        }
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed");
        }
    }

    /**
     * Gets a database connection from the pool
     * 
     * @return Database connection
     * @throws SQLException if connection cannot be obtained
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public CompletableFuture<ModRequest> createRequest(ModRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                    INSERT INTO mod_requests (player_id, player_name, description, status, world_name, x, y, z, yaw, pitch, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (Connection conn = dataSource.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, request.getPlayerId().toString());
                stmt.setString(2, request.getPlayerName());
                stmt.setString(3, request.getDescription());
                stmt.setString(4, request.getStatus().name());
                stmt.setString(5, request.getWorldName());
                stmt.setDouble(6, request.getX());
                stmt.setDouble(7, request.getY());
                stmt.setDouble(8, request.getZ());
                stmt.setFloat(9, request.getYaw());
                stmt.setFloat(10, request.getPitch());
                stmt.setTimestamp(11, Timestamp.valueOf(request.getCreatedAt()));
                stmt.setTimestamp(12, Timestamp.valueOf(request.getUpdatedAt()));

                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        request.setId(keys.getInt(1));
                    }
                }

                return request;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create mod request", e);
                throw new RuntimeException("Failed to create mod request", e);
            }
        });
    }

    public CompletableFuture<Void> updateRequest(ModRequest request) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                    UPDATE mod_requests SET
                        description = ?, status = ?, claimed_by = ?, claimed_by_name = ?,
                        closed_by = ?, closed_by_name = ?, completed_by = ?, completed_by_name = ?,
                        world_name = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ?,
                        updated_at = ?, closed_at = ?
                    WHERE id = ?
                    """;

            try (Connection conn = dataSource.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, request.getDescription());
                stmt.setString(2, request.getStatus().name());
                stmt.setString(3, request.getClaimedBy() != null ? request.getClaimedBy().toString() : null);
                stmt.setString(4, request.getClaimedByName());
                stmt.setString(5, request.getClosedBy() != null ? request.getClosedBy().toString() : null);
                stmt.setString(6, request.getClosedByName());
                stmt.setString(7, request.getCompletedBy() != null ? request.getCompletedBy().toString() : null);
                stmt.setString(8, request.getCompletedByName());
                stmt.setString(9, request.getWorldName());
                stmt.setDouble(10, request.getX());
                stmt.setDouble(11, request.getY());
                stmt.setDouble(12, request.getZ());
                stmt.setFloat(13, request.getYaw());
                stmt.setFloat(14, request.getPitch());
                stmt.setTimestamp(15, Timestamp.valueOf(request.getUpdatedAt()));
                stmt.setTimestamp(16, request.getClosedAt() != null ? Timestamp.valueOf(request.getClosedAt()) : null);
                stmt.setInt(17, request.getId());

                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update mod request", e);
                throw new RuntimeException("Failed to update mod request", e);
            }
        });
    }

    public CompletableFuture<ModRequest> getRequest(int id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM mod_requests WHERE id = ?";

            try (Connection conn = dataSource.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, id);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        ModRequest request = mapResultSetToRequest(rs);
                        // Load notes
                        request.setNotes(getRequestNotesSync(id));
                        return request;
                    }
                }

                return null;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get mod request", e);
                throw new RuntimeException("Failed to get mod request", e);
            }
        });
    }

    public CompletableFuture<List<ModRequest>> getRequestsByPlayer(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM mod_requests WHERE player_id = ? ORDER BY created_at DESC";

            try (Connection conn = dataSource.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerId.toString());

                List<ModRequest> requests = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ModRequest request = mapResultSetToRequest(rs);
                        request.setNotes(getRequestNotesSync(request.getId()));
                        requests.add(request);
                    }
                }

                return requests;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player requests", e);
                throw new RuntimeException("Failed to get player requests", e);
            }
        });
    }

    public CompletableFuture<List<ModRequest>> getRequestsByStatus(ModRequestStatus status) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM mod_requests WHERE status = ? ORDER BY created_at ASC";

            try (Connection conn = dataSource.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, status.name());

                List<ModRequest> requests = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ModRequest request = mapResultSetToRequest(rs);
                        request.setNotes(getRequestNotesSync(request.getId()));
                        requests.add(request);
                    }
                }

                return requests;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get requests by status", e);
                throw new RuntimeException("Failed to get requests by status", e);
            }
        });
    }

    public CompletableFuture<List<ModRequest>> getOpenRequestsByPlayer(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM mod_requests WHERE player_id = ? AND status IN ('OPEN', 'CLAIMED', 'ELEVATED') ORDER BY created_at DESC";

            try (Connection conn = dataSource.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerId.toString());

                List<ModRequest> requests = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ModRequest request = mapResultSetToRequest(rs);
                        request.setNotes(getRequestNotesSync(request.getId()));
                        requests.add(request);
                    }
                }

                return requests;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get open player requests", e);
                throw new RuntimeException("Failed to get open player requests", e);
            }
        });
    }

    public CompletableFuture<Integer> countOpenRequestsByPlayer(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM mod_requests WHERE player_id = ? AND status IN ('OPEN', 'CLAIMED', 'ELEVATED')";

            try (Connection conn = dataSource.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerId.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }

                return 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to count player requests", e);
                throw new RuntimeException("Failed to count player requests", e);
            }
        });
    }

    public CompletableFuture<ModRequestNote> addNote(ModRequestNote note) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO mod_request_notes (request_id, author_id, author_name, content, created_at) VALUES (?, ?, ?, ?, ?)";

            try (Connection conn = dataSource.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setInt(1, note.getRequestId());
                stmt.setString(2, note.getAuthorId().toString());
                stmt.setString(3, note.getAuthorName());
                stmt.setString(4, note.getContent());
                stmt.setTimestamp(5, Timestamp.valueOf(note.getCreatedAt()));

                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        note.setId(keys.getInt(1));
                    }
                }

                return note;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to add note", e);
                throw new RuntimeException("Failed to add note", e);
            }
        });
    }

    private List<ModRequestNote> getRequestNotesSync(int requestId) throws SQLException {
        String sql = "SELECT * FROM mod_request_notes WHERE request_id = ? ORDER BY created_at ASC";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, requestId);

            List<ModRequestNote> notes = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }

            return notes;
        }
    }

    private ModRequest mapResultSetToRequest(ResultSet rs) throws SQLException {
        ModRequest request = new ModRequest();
        request.setId(rs.getInt("id"));
        request.setPlayerId(UUID.fromString(rs.getString("player_id")));
        request.setPlayerName(rs.getString("player_name"));
        request.setDescription(rs.getString("description"));
        request.setStatus(ModRequestStatus.valueOf(rs.getString("status")));

        String claimedByStr = rs.getString("claimed_by");
        if (claimedByStr != null) {
            request.setClaimedBy(UUID.fromString(claimedByStr));
        }
        request.setClaimedByName(rs.getString("claimed_by_name"));

        String closedByStr = rs.getString("closed_by");
        if (closedByStr != null) {
            request.setClosedBy(UUID.fromString(closedByStr));
        }
        request.setClosedByName(rs.getString("closed_by_name"));

        String completedByStr = rs.getString("completed_by");
        if (completedByStr != null) {
            request.setCompletedBy(UUID.fromString(completedByStr));
        }
        request.setCompletedByName(rs.getString("completed_by_name"));

        request.setWorldName(rs.getString("world_name"));
        request.setX(rs.getDouble("x"));
        request.setY(rs.getDouble("y"));
        request.setZ(rs.getDouble("z"));
        request.setYaw(rs.getFloat("yaw"));
        request.setPitch(rs.getFloat("pitch"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            request.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            request.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        Timestamp closedAt = rs.getTimestamp("closed_at");
        if (closedAt != null) {
            request.setClosedAt(closedAt.toLocalDateTime());
        }

        return request;
    }

    private ModRequestNote mapResultSetToNote(ResultSet rs) throws SQLException {
        ModRequestNote note = new ModRequestNote();
        note.setId(rs.getInt("id"));
        note.setRequestId(rs.getInt("request_id"));
        note.setAuthorId(UUID.fromString(rs.getString("author_id")));
        note.setAuthorName(rs.getString("author_name"));
        note.setContent(rs.getString("content"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            note.setCreatedAt(createdAt.toLocalDateTime());
        }

        return note;
    }
}
