package org.bukkit.scoreboard;
import org.bukkit.ChatColor;
public interface Team {
    void setColor(ChatColor color);
    void addEntry(String entry);
    void unregister();
}
