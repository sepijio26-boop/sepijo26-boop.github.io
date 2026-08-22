package org.bukkit.configuration.file;
import org.bukkit.configuration.ConfigurationSection;
import java.io.File;
public class YamlConfiguration implements FileConfiguration {
    public YamlConfiguration() { }
    public static YamlConfiguration loadConfiguration(File file) { return new YamlConfiguration(); }
    public void save(File file) throws java.io.IOException { }
    public ConfigurationSection createSection(String path) { return new Section(); }
    public static class Section implements ConfigurationSection {
        public java.util.Set<String> getKeys(boolean deep) { return new java.util.HashSet<String>(); }
        public String getString(String path) { return null; }
        public String getString(String path, String def) { return def; }
        public int getInt(String path, int def) { return def; }
        public double getDouble(String path, double def) { return def; }
        public boolean getBoolean(String path, boolean def) { return def; }
        public long getLong(String path, long def) { return def; }
        public java.util.List<String> getStringList(String path) { return new java.util.ArrayList<String>(); }
        public ConfigurationSection getConfigurationSection(String path) { return null; }
        public void set(String path, Object value) { }
    }
    // ConfigurationSection methods needed by YamlStore (root usage)
    public java.util.Set<String> getKeys(boolean deep) { return new java.util.HashSet<String>(); }
    public String getString(String path) { return null; }
    public String getString(String path, String def) { return def; }
    public int getInt(String path, int def) { return def; }
    public double getDouble(String path, double def) { return def; }
    public boolean getBoolean(String path, boolean def) { return def; }
    public long getLong(String path, long def) { return def; }
    public java.util.List<String> getStringList(String path) { return new java.util.ArrayList<String>(); }
    public ConfigurationSection getConfigurationSection(String path) { return null; }
    public void set(String path, Object value) { }
}
