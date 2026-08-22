package me.sepi.clans.core;

/**
 * Mutable defaults read from config.yml.
 */
public final class ClanSettings {

    private volatile boolean defaultPublic = true;
    private volatile boolean defaultPvp = true;
    private volatile int inviteExpiryMinutes = 10;
    private volatile int kickCooldownDays = 7;
    private volatile int taxIntervalMinutes = 60;
    private volatile double taxOgPercent = 20.0D;
    private volatile boolean taxExemptOwner = true;
    /** "salary" = vault pays members each hour (default), "collect" = members pay into vault. */
    private volatile String taxMode = "salary";

    public boolean isDefaultPublic() {
        return defaultPublic;
    }

    public void setDefaultPublic(boolean defaultPublic) {
        this.defaultPublic = defaultPublic;
    }

    public boolean isDefaultPvp() {
        return defaultPvp;
    }

    public void setDefaultPvp(boolean defaultPvp) {
        this.defaultPvp = defaultPvp;
    }

    public int getInviteExpiryMinutes() {
        return inviteExpiryMinutes;
    }

    public void setInviteExpiryMinutes(int inviteExpiryMinutes) {
        this.inviteExpiryMinutes = inviteExpiryMinutes;
    }

    public int getKickCooldownDays() {
        return kickCooldownDays;
    }

    public void setKickCooldownDays(int kickCooldownDays) {
        this.kickCooldownDays = kickCooldownDays;
    }

    public int getTaxIntervalMinutes() {
        return taxIntervalMinutes;
    }

    public void setTaxIntervalMinutes(int taxIntervalMinutes) {
        this.taxIntervalMinutes = taxIntervalMinutes;
    }

    public double getTaxOgPercent() {
        return taxOgPercent;
    }

    public void setTaxOgPercent(double taxOgPercent) {
        this.taxOgPercent = taxOgPercent;
    }

    public String getTaxMode() {
        return taxMode;
    }

    public void setTaxMode(String taxMode) {
        this.taxMode = "collect".equalsIgnoreCase(taxMode) ? "collect" : "salary";
    }

    public boolean isTaxExemptOwner() {
        return taxExemptOwner;
    }

    public void setTaxExemptOwner(boolean taxExemptOwner) {
        this.taxExemptOwner = taxExemptOwner;
    }
}
