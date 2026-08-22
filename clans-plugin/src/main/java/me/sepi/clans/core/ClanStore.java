package me.sepi.clans.core;

import me.sepi.clans.model.Clan;
import me.sepi.clans.model.PlayerData;

import java.util.Collection;
import java.util.UUID;

/**
 * Persistence boundary. The Bukkit implementation stores to YAML files.
 */
public interface ClanStore {

    Clan getClan(String key);

    Collection<Clan> allClans();

    void saveClan(Clan clan);

    void removeClan(String key);

    PlayerData getPlayer(UUID uuid);

    Collection<PlayerData> allPlayers();

    void savePlayer(PlayerData data);

    void removePlayer(UUID uuid);
}
