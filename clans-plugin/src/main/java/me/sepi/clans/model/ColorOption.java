package me.sepi.clans.model;

/**
 * A selectable clan tag colour.
 */
public final class ColorOption {

    private final String id;          // e.g. "red"
    private final String legacyCode;  // e.g. "&c"
    private final String chatColor;   // Bukkit ChatColor enum name, e.g. "RED"
    private final double cost;        // Vault cost, 0 = free

    public ColorOption(String id, String legacyCode, String chatColor, double cost) {
        this.id = id;
        this.legacyCode = legacyCode;
        this.chatColor = chatColor;
        this.cost = cost;
    }

    public String getId() {
        return id;
    }

    public String getLegacyCode() {
        return legacyCode;
    }

    public String getChatColor() {
        return chatColor;
    }

    public double getCost() {
        return cost;
    }

    public boolean isFree() {
        return cost <= 0.0D;
    }
}
