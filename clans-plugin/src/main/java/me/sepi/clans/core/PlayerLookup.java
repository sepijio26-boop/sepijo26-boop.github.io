package me.sepi.clans.core;

import java.util.UUID;

/**
 * Resolves player names to UUIDs (Bukkit-backed in production).
 */
public interface PlayerLookup {

    UUID findByName(String name);
}
