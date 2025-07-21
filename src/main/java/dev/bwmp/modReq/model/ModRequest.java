package dev.bwmp.modReq.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class ModRequest {

    private int id;
    private UUID playerId;
    private String playerName;
    private String description;
    private ModRequestStatus status;
    private UUID claimedBy;
    private String claimedByName;
    private UUID closedBy;
    private String closedByName;
    private UUID completedBy;
    private String completedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;

    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    private List<ModRequestNote> notes;

    public ModRequest() {
        this.status = ModRequestStatus.OPEN;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.notes = new ArrayList<>();
    }

    public ModRequest(UUID playerId, String playerName, String description, Location location) {
        this();
        this.playerId = playerId;
        this.playerName = playerName;
        this.description = description;
        if (location != null) {
            this.worldName = location.getWorld().getName();
            this.x = location.getX();
            this.y = location.getY();
            this.z = location.getZ();
            this.yaw = location.getYaw();
            this.pitch = location.getPitch();
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ModRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ModRequestStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        if (status == ModRequestStatus.CLOSED || status == ModRequestStatus.COMPLETED) {
            this.closedAt = LocalDateTime.now();
        }
    }

    public UUID getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(UUID claimedBy) {
        this.claimedBy = claimedBy;
    }

    public String getClaimedByName() {
        return claimedByName;
    }

    public void setClaimedByName(String claimedByName) {
        this.claimedByName = claimedByName;
    }

    public UUID getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(UUID closedBy) {
        this.closedBy = closedBy;
    }

    public String getClosedByName() {
        return closedByName;
    }

    public void setClosedByName(String closedByName) {
        this.closedByName = closedByName;
    }

    public UUID getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(UUID completedBy) {
        this.completedBy = completedBy;
    }

    public String getCompletedByName() {
        return completedByName;
    }

    public void setCompletedByName(String completedByName) {
        this.completedByName = completedByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public List<ModRequestNote> getNotes() {
        return notes;
    }

    public void setNotes(List<ModRequestNote> notes) {
        this.notes = notes;
    }

    /**
     * Gets the location of this mod request
     * 
     * @return Location or null if world is not loaded
     */
    public Location getLocation() {
        if (worldName == null)
            return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null)
            return null;

        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Sets the location of this mod request
     * 
     * @param location The location to set
     */
    public void setLocation(Location location) {
        if (location == null) {
            this.worldName = null;
            return;
        }

        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

    /**
     * Checks if this request is claimed by someone
     * 
     * @return true if claimed
     */
    public boolean isClaimed() {
        return claimedBy != null;
    }

    /**
     * Checks if this request is open (not closed or completed)
     * 
     * @return true if open
     */
    public boolean isOpen() {
        return status == ModRequestStatus.OPEN || status == ModRequestStatus.ELEVATED;
    }

    /**
     * Claims this request for a staff member
     * 
     * @param staffId   The UUID of the staff member
     * @param staffName The name of the staff member
     */
    public void claim(UUID staffId, String staffName) {
        this.claimedBy = staffId;
        this.claimedByName = staffName;
        // Status remains the same - claiming is independent of status
        this.updatedAt = LocalDateTime.now();
    }

    public void unclaim() {
        this.claimedBy = null;
        this.claimedByName = null;
        // Status remains the same - unclaiming doesn't change the status
        this.updatedAt = LocalDateTime.now();
    }

    public void elevate() {
        this.status = ModRequestStatus.ELEVATED;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = ModRequestStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
        this.closedAt = LocalDateTime.now();
    }

    /**
     * Marks this request as completed and tracks who completed it
     * 
     * @param completerId   The UUID of who completed it
     * @param completerName The name of who completed it
     */
    public void complete(UUID completerId, String completerName) {
        this.status = ModRequestStatus.COMPLETED;
        this.completedBy = completerId;
        this.completedByName = completerName;
        this.closedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void close() {
        this.status = ModRequestStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
        this.closedAt = LocalDateTime.now();
    }

    /**
     * Closes this request and tracks who closed it
     * 
     * @param closerId   The UUID of who closed it
     * @param closerName The name of who closed it
     */
    public void close(UUID closerId, String closerName) {
        this.status = ModRequestStatus.CLOSED;
        this.closedBy = closerId;
        this.closedByName = closerName;
        this.closedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("ModRequest{id=%d, player=%s, status=%s, description='%s'}",
                id, playerName, status, description);
    }
}
