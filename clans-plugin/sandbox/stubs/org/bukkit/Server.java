package org.bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.ScoreboardManager;
import java.util.Collection;
public interface Server {
    PluginManager getPluginManager();
    BukkitScheduler getScheduler();
    ServicesManager getServicesManager();
    ScoreboardManager getScoreboardManager();
    Player getPlayerExact(String name);
    Collection<? extends Player> getOnlinePlayers();
}
