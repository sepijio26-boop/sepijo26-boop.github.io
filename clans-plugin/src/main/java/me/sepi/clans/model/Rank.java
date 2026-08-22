package me.sepi.clans.model;

/**
 * Clan ranks. The owner is stored separately and is always the highest rank.
 */
public enum Rank {
    OWNER("Owner"),
    OG("OG"),
    ADMIN("Admin"),
    MEMBER("Member");

    private final String display;

    Rank(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
