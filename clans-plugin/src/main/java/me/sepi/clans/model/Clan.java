package me.sepi.clans.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Mutable in-memory representation of a clan. All fields that can change at
 * runtime are kept volatile so async chat rendering can read them safely.
 */
public class Clan {

    /** Team names/scoreboard entries must stay case-insensitively unique. */
    private final String key;
    private final UUID owner;
    private final long created;

    private final Map<UUID, String> members = new HashMap<UUID, String>();
    private final Set<UUID> admins = new HashSet<UUID>();
    private final Set<UUID> ogs = new HashSet<UUID>();
    /** uuid -&gt; timestamp (ms) when the player was kicked while the clan was public. */
    private final Map<UUID, Long> kicks = new HashMap<UUID, Long>();

    private volatile String colorId;
    private volatile boolean pvp;
    private volatile double tax;
    private volatile boolean publicClan;
    private volatile double vault;

    public Clan(String key, UUID owner, long created, String colorId, boolean pvp, boolean publicClan) {
        this.key = key;
        this.owner = owner;
        this.created = created;
        this.colorId = colorId;
        this.pvp = pvp;
        this.publicClan = publicClan;
        this.tax = 0.0D;
    }

    public String getKey() {
        return key;
    }

    public UUID getOwner() {
        return owner;
    }

    public long getCreated() {
        return created;
    }

    public String getColorId() {
        return colorId;
    }

    public void setColorId(String colorId) {
        this.colorId = colorId;
    }

    public boolean isPvp() {
        return pvp;
    }

    public void setPvp(boolean pvp) {
        this.pvp = pvp;
    }

    public double getTax() {
        return tax;
    }

    public void setTax(double tax) {
        this.tax = tax;
    }

    public boolean isPublic() {
        return publicClan;
    }

    public void setPublic(boolean publicClan) {
        this.publicClan = publicClan;
    }

    public double getVault() {
        return vault;
    }

    public void addToVault(double amount) {
        this.vault += amount;
    }

    public void removeFromVault(double amount) {
        this.vault = Math.max(0.0D, this.vault - amount);
    }

    public void setVault(double vault) {
        this.vault = vault;
    }

    public Map<UUID, String> getMembers() {
        return members;
    }

    public Set<UUID> getAdmins() {
        return admins;
    }

    public Set<UUID> getOgs() {
        return ogs;
    }

    public Map<UUID, Long> getKicks() {
        return kicks;
    }

    public boolean hasMember(UUID id) {
        return owner.equals(id) || members.containsKey(id);
    }

    public boolean isOwner(UUID id) {
        return owner.equals(id);
    }

    public boolean isAdmin(UUID id) {
        return admins.contains(id);
    }

    public boolean isOg(UUID id) {
        return ogs.contains(id);
    }

    public Rank rankOf(UUID id) {
        if (owner.equals(id)) {
            return Rank.OWNER;
        }
        if (ogs.contains(id)) {
            return Rank.OG;
        }
        if (admins.contains(id)) {
            return Rank.ADMIN;
        }
        if (members.containsKey(id)) {
            return Rank.MEMBER;
        }
        return null;
    }

    public String memberName(UUID id) {
        if (owner.equals(id)) {
            return members.get(id);
        }
        return members.get(id);
    }

    public void addMember(UUID id, String name, Rank rank) {
        members.put(id, name);
        if (rank == Rank.ADMIN) {
            admins.add(id);
        } else if (rank == Rank.OG) {
            ogs.add(id);
        }
    }

    public void setRank(UUID id, Rank rank) {
        admins.remove(id);
        ogs.remove(id);
        if (rank == Rank.ADMIN) {
            admins.add(id);
        } else if (rank == Rank.OG) {
            ogs.add(id);
        }
    }

    public void removeMember(UUID id) {
        members.remove(id);
        admins.remove(id);
        ogs.remove(id);
    }

    /** @return an unmodifiable snapshot of the member UUIDs. */
    public Set<UUID> memberIds() {
        Set<UUID> all = new HashSet<UUID>(members.keySet());
        all.add(owner);
        return Collections.unmodifiableSet(all);
    }
}
