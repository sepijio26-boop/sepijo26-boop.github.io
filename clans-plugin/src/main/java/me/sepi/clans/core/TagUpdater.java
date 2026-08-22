package me.sepi.clans.core;

import java.util.UUID;

/**
 * Callback used after clan changes so the Bukkit layer can refresh the tab
 * list, name tags and scoreboard teams.
 */
public interface TagUpdater {

    void updatePlayer(UUID uuid);
}
