package dev.bwmp.modReq.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import dev.bwmp.modReq.ModReq;
import dev.bwmp.modReq.database.DatabaseManager;
import dev.bwmp.modReq.model.ModRequest;
import dev.bwmp.modReq.model.ModRequestNote;
import dev.bwmp.modReq.model.ModRequestStatus;

public class ModRequestService {

    private final ModReq plugin;
    private final DatabaseManager databaseManager;

    public ModRequestService(ModReq plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    public CompletableFuture<ModRequest> createRequest(Player player, String description) {
        return createRequest(player.getUniqueId(), player.getName(), description, player.getLocation());
    }

    public CompletableFuture<ModRequest> createRequest(UUID playerId, String playerName, String description,
            Location location) {
        return CompletableFuture.supplyAsync(() -> {
            int maxRequests = plugin.getConfigManager().getInt("settings.max_requests_per_player", 5);
            if (maxRequests > 0) {
                try {
                    int currentCount = databaseManager.countOpenRequestsByPlayer(playerId).get();
                    if (currentCount >= maxRequests) {
                        throw new IllegalStateException(
                                "Player has reached maximum number of open requests: " + maxRequests);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to check player request count: " + e.getMessage());
                    throw new RuntimeException("Failed to check player request count", e);
                }
            }

            ModRequest request = new ModRequest(playerId, playerName, description, location);

            try {
                return databaseManager.createRequest(request).get();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create mod request: " + e.getMessage());
                throw new RuntimeException("Failed to create mod request", e);
            }
        });
    }

    public CompletableFuture<Boolean> claimRequest(int requestId, Player staff) {
        return claimRequest(requestId, staff.getUniqueId(), staff.getName());
    }

    public CompletableFuture<Boolean> claimRequest(int requestId, UUID staffId, String staffName) {
        return databaseManager.getRequest(requestId).thenCompose(request -> {
            if (request == null) {
                return CompletableFuture.completedFuture(false);
            }

            if (request.isClaimed()) {
                return CompletableFuture.completedFuture(false);
            }

            request.claim(staffId, staffName);
            return databaseManager.updateRequest(request).thenApply(v -> true);
        });
    }

    public CompletableFuture<Boolean> forceClaimRequest(int requestId, Player staff) {
        return forceClaimRequest(requestId, staff.getUniqueId(), staff.getName());
    }

    public CompletableFuture<Boolean> forceClaimRequest(int requestId, UUID staffId, String staffName) {
        return databaseManager.getRequest(requestId).thenCompose(request -> {
            if (request == null) {
                return CompletableFuture.completedFuture(false);
            }

            request.claim(staffId, staffName);
            return databaseManager.updateRequest(request).thenApply(v -> true);
        });
    }

    public CompletableFuture<Boolean> unclaimRequest(int requestId) {
        return databaseManager.getRequest(requestId).thenCompose(request -> {
            if (request == null || !request.isClaimed()) {
                return CompletableFuture.completedFuture(false);
            }

            request.unclaim();
            return databaseManager.updateRequest(request).thenApply(v -> true);
        });
    }

    public CompletableFuture<Boolean> completeRequest(int requestId, Player player) {
        return databaseManager.getRequest(requestId).thenCompose(request -> {
            if (request == null || !request.isOpen()) {
                return CompletableFuture.completedFuture(false);
            }

            request.complete(player.getUniqueId(), player.getName());
            return databaseManager.updateRequest(request).thenApply(v -> true);
        });
    }

    public CompletableFuture<ModRequest> completeRequestAndReturn(int requestId, Player player) {
        return databaseManager.getRequest(requestId).thenCompose(request -> {
            if (request == null || !request.isOpen()) {
                return CompletableFuture.completedFuture(null);
            }

            request.complete(player.getUniqueId(), player.getName());
            return databaseManager.updateRequest(request).thenApply(v -> request);
        });
    }

    public CompletableFuture<ModRequest> closeRequest(int requestId, Player player) {
        return databaseManager.getRequest(requestId).thenCompose(request -> {
            if (request == null || !request.isOpen()) {
                return CompletableFuture.completedFuture(null);
            }

            request.close(player.getUniqueId(), player.getName());
            return databaseManager.updateRequest(request).thenApply(v -> request);
        });
    }

    public CompletableFuture<Boolean> elevateRequest(int requestId) {
        return databaseManager.getRequest(requestId).thenCompose(request -> {
            if (request == null || !request.isOpen()) {
                return CompletableFuture.completedFuture(false);
            }

            request.elevate();
            return databaseManager.updateRequest(request).thenApply(v -> true);
        });
    }

    public CompletableFuture<ModRequest> elevateRequestAndReturn(int requestId) {
        return databaseManager.getRequest(requestId).thenCompose(request -> {
            if (request == null || !request.isOpen()) {
                return CompletableFuture.completedFuture(null);
            }

            request.elevate();
            return databaseManager.updateRequest(request).thenApply(v -> request);
        });
    }

    public CompletableFuture<ModRequestNote> addNote(int requestId, Player author, String content) {
        return addNote(requestId, author.getUniqueId(), author.getName(), content);
    }

    public CompletableFuture<ModRequestNote> addNote(int requestId, UUID authorId, String authorName, String content) {
        ModRequestNote note = new ModRequestNote(requestId, authorId, authorName, content);
        return databaseManager.addNote(note);
    }

    public CompletableFuture<ModRequest> getRequest(int id) {
        return databaseManager.getRequest(id);
    }

    /**
     * Gets a mod request by ID with notes loaded
     * TODO: Implement getNotesForRequest in DatabaseManager
     */
    public CompletableFuture<ModRequest> getRequestWithNotes(int id) {
        // For now, just return the request without notes
        // This can be enhanced later when we add note retrieval to DatabaseManager
        return getRequest(id);
    }

    public CompletableFuture<List<ModRequest>> getPlayerRequests(UUID playerId) {
        return databaseManager.getRequestsByPlayer(playerId);
    }

    public CompletableFuture<List<ModRequest>> getOpenPlayerRequests(UUID playerId) {
        return databaseManager.getOpenRequestsByPlayer(playerId);
    }

    public CompletableFuture<List<ModRequest>> getRequestsByStatus(ModRequestStatus status) {
        return databaseManager.getRequestsByStatus(status);
    }

    public CompletableFuture<List<ModRequest>> getOpenRequests() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<ModRequest> requests = databaseManager.getRequestsByStatus(ModRequestStatus.OPEN).get();
                requests.addAll(databaseManager.getRequestsByStatus(ModRequestStatus.ELEVATED).get());

                requests.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));

                return requests;
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to get open requests: " + e.getMessage());
                throw new RuntimeException("Failed to get open requests", e);
            }
        });
    }

    public CompletableFuture<List<ModRequest>> getRequests(ModRequestStatus status, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<ModRequest> requests = new ArrayList<>();

                if (status != null) {
                    requests = databaseManager.getRequestsByStatus(status).get();
                } else {
                    for (ModRequestStatus s : ModRequestStatus.values()) {
                        requests.addAll(databaseManager.getRequestsByStatus(s).get());
                    }
                }

                if (playerName != null) {
                    final String finalPlayerName = playerName.toLowerCase();
                    requests = requests.stream()
                            .filter(r -> r.getPlayerName().toLowerCase().contains(finalPlayerName))
                            .collect(java.util.stream.Collectors.toList());
                }

                requests.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

                return requests;
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to get requests: " + e.getMessage());
                throw new RuntimeException("Failed to get requests", e);
            }
        });
    }

    public CompletableFuture<List<ModRequest>> getActiveRequests(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<ModRequest> requests = new ArrayList<>();

                requests.addAll(databaseManager.getRequestsByStatus(ModRequestStatus.OPEN).get());
                requests.addAll(databaseManager.getRequestsByStatus(ModRequestStatus.ELEVATED).get());

                if (playerName != null) {
                    final String finalPlayerName = playerName.toLowerCase();
                    requests = requests.stream()
                            .filter(r -> r.getPlayerName().toLowerCase().contains(finalPlayerName))
                            .collect(java.util.stream.Collectors.toList());
                }

                requests.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

                return requests;
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to get active requests: " + e.getMessage());
                throw new RuntimeException("Failed to get active requests", e);
            }
        });
    }

    public CompletableFuture<Integer> countOpenRequests(UUID playerId) {
        return databaseManager.countOpenRequestsByPlayer(playerId);
    }

    public CompletableFuture<Boolean> canPlayerCreateRequest(UUID playerId) {
        int maxRequests = plugin.getConfigManager().getInt("settings.max_requests_per_player", 5);
        if (maxRequests <= 0) {
            return CompletableFuture.completedFuture(true);
        }

        return countOpenRequests(playerId).thenApply(count -> count < maxRequests);
    }
}
