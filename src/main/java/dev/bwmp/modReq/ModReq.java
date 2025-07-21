package dev.bwmp.modReq;

import dev.bwmp.modReq.config.ConfigManager;
import dev.bwmp.modReq.database.DatabaseManager;
import dev.bwmp.modReq.service.DiscordService;
import dev.bwmp.modReq.service.ModRequestService;
import org.bukkit.plugin.java.JavaPlugin;

public final class ModReq extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private ModRequestService modRequestService;
    private DiscordService discordService;

    @Override
    public void onEnable() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");

        this.configManager = new ConfigManager(this);

        this.databaseManager = new DatabaseManager(this);
        try {
            this.databaseManager.initialize();
        } catch (Exception e) {
            getLogger().severe("Failed to initialize database. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.modRequestService = new ModRequestService(this);
        this.discordService = new DiscordService(this);

        ModReqCommand commandHandler = new ModReqCommand(this);
        getCommand("modreq").setExecutor(commandHandler);
        getCommand("modreq").setTabCompleter(commandHandler);

        getLogger().info("ModReq plugin has been enabled!");
        getLogger().info("Database type: " + configManager.getString("database.type", "h2"));
        getLogger().info("Discord integration: "
                + (configManager.getBoolean("discord.enabled", false) ? "enabled" : "disabled"));
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        getLogger().info("ModReq plugin has been disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ModRequestService getModRequestService() {
        return modRequestService;
    }

    public DiscordService getDiscordService() {
        return discordService;
    }
}
