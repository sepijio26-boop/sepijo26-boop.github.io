package me.sepi.clans.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent per-player data handled by the plugin.
 */
public class PlayerData {

    private final UUID uuid;
    private String lastName;
    private String clanKey;
    /** clanKey -&gt; invite expiry timestamp (ms). */
    private final Map<String, Long> invites = new HashMap<String, Long>();

    public PlayerData(UUID uuid, String lastName) {
        this.uuid = uuid;
        this.lastName = lastName;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getClanKey() {
        return clanKey;
    }

    public void setClanKey(String clanKey) {
        this.clanKey = clanKey;
    }

    public Map<String, Long> getInvites() {
        return invites;
    }

    public void addInvite(String clanKey, long expiryMs) {
        invites.put(clanKey, Long.valueOf(expiryMs));
    }

    public void removeInvite(String clanKey) {
        invites.remove(clanKey);
    }

    public void clearInvites() {
        invites.clear();
    }
}
