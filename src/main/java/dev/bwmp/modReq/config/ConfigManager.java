package dev.bwmp.modReq.config;

import dev.bwmp.modReq.ModReq;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final ModReq plugin;
    private FileConfiguration config;
    private final MiniMessage miniMessage;
    private final LegacyComponentSerializer legacySerializer;

    public ConfigManager(ModReq plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.legacyAmpersand();
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public void reloadConfig() {
        loadConfig();
    }

    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }

    public String getString(String path) {
        return config.getString(path);
    }

    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }

    public Component getMessageComponent(String key) {
        String message = getString("messages." + key, "Message not found: " + key);
        if (message.contains("<") && message.contains(">")) {
            return miniMessage.deserialize(message);
        } else {
            return legacySerializer.deserialize(message);
        }
    }

    public String getMessage(String key) {
        Component component = getMessageComponent(key);
        return legacySerializer.serialize(component);
    }

    public Component getFormattedMessageComponent(String key, Object... replacements) {
        String message = getString("messages." + key, "Message not found: " + key);

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String placeholder = "{" + replacements[i] + "}";
                String value = String.valueOf(replacements[i + 1]);
                message = message.replace(placeholder, value);
            }
        }

        if (message.contains("<") && message.contains(">")) {
            return miniMessage.deserialize(message);
        } else {
            return legacySerializer.deserialize(message);
        }
    }

    public String getFormattedMessage(String key, Object... replacements) {
        Component component = getFormattedMessageComponent(key, replacements);
        return legacySerializer.serialize(component);
    }

    public int getListPageSize() {
        return getInt("settings.list_page_size", 10);
    }
}
