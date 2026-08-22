package org.bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.ScoreboardManager;
import java.util.Collection;
import java.util.UUID;
/** Minimal stub of org.bukkit.Bukkit. */
public final class Bukkit {
    public static Server getServer() { return null; }
    public static Player getPlayer(UUID id) { return null; }
    public static Player getPlayerExact(String name) { return null; }
    public static OfflinePlayer getOfflinePlayer(String name) { return null; }
    public static OfflinePlayer getOfflinePlayer(UUID id) { return null; }
    public static Collection<? extends Player> getOnlinePlayers() { return null; }
    public static ScoreboardManager getScoreboardManager() { return null; }
}
