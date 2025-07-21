package dev.bwmp.modReq.command;

import dev.bwmp.modReq.util.TextUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class SubCommand {
    
    private final String name;
    private final String permission;
    private final String description;
    private final String usage;
    private final boolean requiresPlayer;
    
    public SubCommand(String name, String permission, String description, String usage, boolean requiresPlayer) {
        this.name = name;
        this.permission = permission;
        this.description = description;
        this.usage = usage;
        this.requiresPlayer = requiresPlayer;
    }
    
    /**
     * Execute the subcommand
     * @param sender The command sender
     * @param args The command arguments (excluding the subcommand name)
     * @return true if command was handled successfully
     */
    public abstract boolean execute(CommandSender sender, String[] args);
    
    /**
     * Get tab completions for this subcommand
     * @param sender The command sender
     * @param args The current arguments
     * @return List of possible completions
     */
    public abstract List<String> getTabCompletions(CommandSender sender, String[] args);
    
    // Getters
    public String getName() { return name; }
    public String getPermission() { return permission; }
    public String getDescription() { return description; }
    public String getUsage() { return usage; }
    public boolean requiresPlayer() { return requiresPlayer; }
    
    /**
     * Check if sender has permission for this command
     * @param sender The command sender
     * @return true if has permission
     */
    public boolean hasPermission(CommandSender sender) {
        return permission == null || sender.hasPermission(permission);
    }
    
    /**
     * Send error message if sender is not a player
     * @param sender The command sender
     * @return true if sender is a player, false otherwise
     */
    public boolean checkPlayer(CommandSender sender) {
        if (requiresPlayer && !(sender instanceof Player)) {
            sender.sendMessage(TextUtil.error("This command can only be used by players!"));
            return false;
        }
        return true;
    }
    
    /**
     * Send permission error message
     * @param sender The command sender
     */
    public void sendNoPermission(CommandSender sender) {
        sender.sendMessage(TextUtil.error("You don't have permission to use this command!"));
    }
    
    /**
     * Send usage message
     * @param sender The command sender
     */
    protected void sendUsage(CommandSender sender) {
        sender.sendMessage(TextUtil.error("Usage: " + usage));
    }
}
