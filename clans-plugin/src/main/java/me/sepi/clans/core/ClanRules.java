package me.sepi.clans.core;

import me.sepi.clans.model.Clan;

import java.util.UUID;

/**
 * Pure clan PvP rule decisions (used by the Bukkit damage listener and by
 * the unit tests).
 */
public final class ClanRules {

    private ClanRules() {
    }

    /**
     * @return true when a clan member's attack on {@code victim} must be blocked.
     */
    public static boolean isPvpBlocked(Clan clan, UUID victim) {
        if (clan == null) {
            return false;
        }
        if (!clan.isPvp()) {
            return true;
        }
        // Owner and OG members are always protected from clan member attacks.
        return clan.isOwner(victim) || clan.isOg(victim);
    }
}
