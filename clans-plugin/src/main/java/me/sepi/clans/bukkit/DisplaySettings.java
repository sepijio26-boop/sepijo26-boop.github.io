package me.sepi.clans.bukkit;

import me.sepi.clans.ClansPlugin;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Chat / tab / name-tag display settings from config.yml.
 */
public final class DisplaySettings {

    private boolean chatEnabled;
    private String chatFormat;
    private boolean tabEnabled;
    private String tabFormat;
    private boolean nametagEnabled;

    public void load(FileConfiguration cfg) {
        chatEnabled = cfg.getBoolean("chat.enabled", true);
        chatFormat = cfg.getString("chat.format", "{name} {tag}: {message}");
        tabEnabled = cfg.getBoolean("tab.enabled", true);
        tabFormat = cfg.getString("tab.format", "{name} {tag}");
        nametagEnabled = cfg.getBoolean("nametag.enabled", true);
    }

    public boolean isChatEnabled() {
        return chatEnabled;
    }

    public String getChatFormat() {
        return chatFormat;
    }

    public boolean isTabEnabled() {
        return tabEnabled;
    }

    public String getTabFormat() {
        return tabFormat;
    }

    public boolean isNametagEnabled() {
        return nametagEnabled;
    }
}
