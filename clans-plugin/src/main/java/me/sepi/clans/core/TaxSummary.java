package me.sepi.clans.core;

/**
 * Result of one tax collection cycle for one clan.
 */
public final class TaxSummary {

    private final String clanKey;
    private final double tax;
    private final int paid;
    private final int failed;
    private final double total;
    private final double ogShare;

    public TaxSummary(String clanKey, double tax, int paid, int failed, double total, double ogShare) {
        this.clanKey = clanKey;
        this.tax = tax;
        this.paid = paid;
        this.failed = failed;
        this.total = total;
        this.ogShare = ogShare;
    }

    public String getClanKey() {
        return clanKey;
    }

    public double getTax() {
        return tax;
    }

    public int getPaid() {
        return paid;
    }

    public int getFailed() {
        return failed;
    }

    public double getTotal() {
        return total;
    }

    public double getOgShare() {
        return ogShare;
    }
}
