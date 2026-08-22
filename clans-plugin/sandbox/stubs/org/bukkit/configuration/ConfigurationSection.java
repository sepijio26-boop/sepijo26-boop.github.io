package org.bukkit.configuration;
import java.util.List;
import java.util.Set;
public interface ConfigurationSection {
    Set<String> getKeys(boolean deep);
    String getString(String path);
    String getString(String path, String def);
    int getInt(String path, int def);
    double getDouble(String path, double def);
    boolean getBoolean(String path, boolean def);
    long getLong(String path, long def);
    List<String> getStringList(String path);
    ConfigurationSection getConfigurationSection(String path);
    void set(String path, Object value);
}
