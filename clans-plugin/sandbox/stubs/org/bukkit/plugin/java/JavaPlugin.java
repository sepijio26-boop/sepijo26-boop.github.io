package org.bukkit.plugin.java;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;
public abstract class JavaPlugin implements Plugin, CommandExecutor, TabCompleter {
    public File getDataFolder() { return null; }
    public Server getServer() { return null; }
    public Logger getLogger() { return Logger.getGlobal(); }
    public void saveResource(String resourcePath, boolean replace) { }
    public void reloadConfig() { }
    public FileConfiguration getConfig() { return null; }
    public void onEnable() { }
    public void onDisable() { }
    public PluginCommand getCommand(String name) { return null; }
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { return false; }
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) { return null; }
}
