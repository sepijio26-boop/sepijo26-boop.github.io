package org.bukkit.plugin;
import java.io.File;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
public interface Plugin {
    File getDataFolder();
    Server getServer();
    Logger getLogger();
    void saveResource(String resourcePath, boolean replace);
    void reloadConfig();
    FileConfiguration getConfig();
    void onEnable();
    void onDisable();
}
