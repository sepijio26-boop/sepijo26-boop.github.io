package me.sepi.clans.core;

import me.sepi.clans.model.Clan;
import me.sepi.clans.model.ColorOption;
import me.sepi.clans.model.PlayerData;
import me.sepi.clans.model.Rank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * All clan business rules live here. It has no Bukkit dependencies, which
 * keeps the rules unit-testable.
 */
public final class ClanManager {

    private static final long MS_PER_DAY = 24L * 60L * 60L * 1000L;

    private final ClanStore store;
    private final ColorRegistry colors;
    private final ClanSettings settings;
    private final PlayerLookup lookup;
    private final TagUpdater updater;
    private final Bank bank;

    public ClanManager(ClanStore store, ColorRegistry colors, ClanSettings settings,
                       PlayerLookup lookup, TagUpdater updater, Bank bank) {
        this.store = store;
        this.colors = colors;
        this.settings = settings;
        this.lookup = lookup;
        this.updater = updater;
        this.bank = bank;
    }

    public ClanStore getStore() {
        return store;
    }

    public Bank getBank() {
        return bank;
    }

    public UUID playerDataLookup(String name) {
        return lookup.findByName(name);
    }

    public ClanSettings getSettings() {
        return settings;
    }

    public ColorRegistry getColors() {
        return colors;
    }

    public Clan clan(String key) {
        return store.getClan(ClanName.normalize(key));
    }

    public Clan clanOf(UUID uuid) {
        PlayerData data = store.getPlayer(uuid);
        if (data == null || data.getClanKey() == null) {
            return null;
        }
        return store.getClan(data.getClanKey());
    }

    public PlayerData playerData(UUID uuid) {
        PlayerData data = store.getPlayer(uuid);
        if (data == null) {
            data = new PlayerData(uuid, null);
            store.savePlayer(data);
        }
        return data;
    }

    public Rank rankOf(UUID uuid) {
        Clan clan = clanOf(uuid);
        return clan == null ? null : clan.rankOf(uuid);
    }

    // -------------------------------------------------------------- create/join

    public Result create(UUID uuid, String name, String colorId) {
        if (clanOf(uuid) != null) {
            return Result.error("error.already-in-clan");
        }
        String key = ClanName.normalize(name);
        if (key == null) {
            return Result.error("error.name-invalid");
        }
        if (store.getClan(key) != null) {
            return Result.error("error.name-taken", key);
        }
        ColorOption color = colors.get(colorId);
        if (color == null) {
            return Result.error("error.color-invalid", String.valueOf(colorId));
        }
        if (!color.isFree()) {
            if (!bank.isAvailable()) {
                return Result.error("error.economy-unavailable");
            }
            if (!bank.withdraw(uuid, color.getCost())) {
                return Result.error("error.insufficient-funds", bank.format(color.getCost()));
            }
        }
        Clan clan = new Clan(key, uuid, System.currentTimeMillis(), color.getId(),
                settings.isDefaultPvp(), settings.isDefaultPublic());
        clan.addMember(uuid, name, Rank.OWNER);
        store.saveClan(clan);
        PlayerData data = playerData(uuid);
        data.setClanKey(key);
        data.clearInvites();
        store.savePlayer(data);
        updater.updatePlayer(uuid);
        return Result.OK;
    }

    public Result join(UUID uuid, String name, String rawKey) {
        if (clanOf(uuid) != null) {
            return Result.error("error.already-in-clan");
        }
        String key = ClanName.normalize(rawKey);
        if (key == null) {
            return Result.error("error.clan-not-found", String.valueOf(rawKey));
        }
        Clan clan = store.getClan(key);
        if (clan == null) {
            return Result.error("error.clan-not-found", key);
        }
        if (!clan.isPublic()) {
            return Result.error("error.clan-private", key);
        }
        Long blockedUntil = clan.getKicks().get(uuid);
        if (blockedUntil != null && blockedUntil.longValue() > System.currentTimeMillis()) {
            return Result.error("error.kicked-cooldown", key,
                    String.valueOf((blockedUntil.longValue() - System.currentTimeMillis()) / (60L * 1000L)));
        }
        addMember(clan, uuid, name);
        return Result.OK;
    }

    // -------------------------------------------------------------- invites

    public Result invite(UUID ownerUuid, String targetName) {
        Clan clan = clanOf(ownerUuid);
        if (clan == null) {
            return Result.error("error.no-clan");
        }
        if (!clan.isOwner(ownerUuid)) {
            return Result.error("error.owner-only");
        }
        UUID target = lookup.findByName(targetName);
        if (target == null) {
            return Result.error("error.player-not-found", targetName);
        }
        if (clan.isOwner(target)) {
            return Result.error("error.cannot-invite-self-owner", targetName);
        }
        if (clanOf(target) != null) {
            return Result.error("error.target-in-clan", targetName);
        }
        if (target.equals(ownerUuid)) {
            return Result.error("error.cannot-invite-self", targetName);
        }
        PlayerData data = playerData(target);
        data.addInvite(clan.getKey(), System.currentTimeMillis() + settings.getInviteExpiryMinutes() * 60L * 1000L);
        store.savePlayer(data);
        return Result.OK;
    }

    public Result accept(UUID uuid, String rawKey) {
        if (clanOf(uuid) != null) {
            return Result.error("error.already-in-clan");
        }
        Clan clan = pendingInvite(uuid, rawKey);
        if (clan == null) {
            return Result.error("error.no-invite");
        }
        PlayerData data = playerData(uuid);
        data.removeInvite(clan.getKey());
        data.clearInvites();
        store.savePlayer(data);
        // An accepted invite overrides an active kick cooldown.
        clan.getKicks().remove(uuid);
        addMember(clan, uuid, data.getLastName());
        return Result.OK;
    }

    public Result decline(UUID uuid, String rawKey) {
        Clan clan = pendingInvite(uuid, rawKey);
        if (clan == null) {
            return Result.error("error.no-invite");
        }
        PlayerData data = playerData(uuid);
        data.removeInvite(clan.getKey());
        store.savePlayer(data);
        return Result.OK;
    }

    /** @return the most recent non-expired pending invite for the player, optionally filtered by key. */
    public Clan pendingInvite(UUID uuid, String rawKey) {
        PlayerData data = store.getPlayer(uuid);
        if (data == null) {
            return null;
        }
        String want = rawKey == null ? null : ClanName.normalize(rawKey);
        long now = System.currentTimeMillis();
        Clan best = null;
        long bestExpiry = -1L;
        Map<String, Long> invites = data.getInvites();
        for (Map.Entry<String, Long> e : invites.entrySet()) {
            long expiry = e.getValue().longValue();
            if (expiry <= now) {
                continue;
            }
            if (want != null && !want.equals(e.getKey())) {
                continue;
            }
            if (expiry > bestExpiry) {
                bestExpiry = expiry;
                best = store.getClan(e.getKey());
            }
        }
        return best;
    }

    private void addMember(Clan clan, UUID uuid, String name) {
        clan.addMember(uuid, name == null ? name : name, Rank.MEMBER);
        store.saveClan(clan);
        PlayerData data = playerData(uuid);
        data.setClanKey(clan.getKey());
        data.clearInvites();
        store.savePlayer(data);
        updater.updatePlayer(uuid);
    }

    // -------------------------------------------------------------- leave/disband

    public Result leave(UUID uuid) {
        Clan clan = clanOf(uuid);
        if (clan == null) {
            return Result.error("error.no-clan");
        }
        if (clan.isOwner(uuid)) {
            return Result.error("error.owner-must-disband");
        }
        clan.removeMember(uuid);
        store.saveClan(clan);
        PlayerData data = playerData(uuid);
        data.setClanKey(null);
        data.clearInvites();
        store.savePlayer(data);
        updater.updatePlayer(uuid);
        return Result.OK;
    }

    public Result disband(UUID uuid) {
        Clan clan = clanOf(uuid);
        if (clan == null) {
            return Result.error("error.no-clan");
        }
        if (!clan.isOwner(uuid)) {
            return Result.error("error.owner-only");
        }
        for (UUID member : clan.memberIds()) {
            PlayerData data = store.getPlayer(member);
            if (data != null) {
                data.setClanKey(null);
                data.clearInvites();
                store.savePlayer(data);
            }
            if (clan.isOwner(member)) {
                clan.getMembers().remove(member);
            }
        }
        store.removeClan(clan.getKey());
        for (UUID member : clan.memberIds()) {
            updater.updatePlayer(member);
        }
        updater.updatePlayer(uuid);
        return Result.OK;
    }

    // -------------------------------------------------------------- kick

    public Result kick(UUID ownerUuid, String targetName) {
        Clan clan = clanOf(ownerUuid);
        if (clan == null) {
            return Result.error("error.no-clan");
        }
        if (!clan.isOwner(ownerUuid)) {
            return Result.error("error.owner-only");
        }
        UUID target = lookup.findByName(targetName);
        if (target == null) {
            return Result.error("error.player-not-found", targetName);
        }
        if (target.equals(ownerUuid)) {
            return Result.error("error.cannot-kick-self");
        }
        if (!clan.hasMember(target)) {
            return Result.error("error.target-not-in-clan", targetName);
        }
        clan.removeMember(target);
        if (clan.isPublic()) {
            clan.getKicks().put(target, Long.valueOf(System.currentTimeMillis() + settings.getKickCooldownDays() * MS_PER_DAY));
        }
        store.saveClan(clan);
        PlayerData data = playerData(target);
        data.setClanKey(null);
        data.clearInvites();
        store.savePlayer(data);
        updater.updatePlayer(target);
        return Result.OK;
    }

    // -------------------------------------------------------------- settings

    public Result setPvp(UUID ownerUuid, boolean enabled) {
        return mutateOwner(ownerUuid, "error.owner-only", new ClanMutator() {
            public Result apply(Clan clan) {
                clan.setPvp(enabled);
                return Result.OK;
            }
        });
    }

    public Result setTax(UUID ownerUuid, double amount) {
        if (amount < 0.0D) {
            return Result.error("error.amount-positive");
        }
        return mutateOwner(ownerUuid, "error.owner-only", new ClanMutator() {
            public Result apply(Clan clan) {
                clan.setTax(amount);
                return Result.OK;
            }
        });
    }

    public Result setPublic(UUID ownerUuid, boolean enabled) {
        return mutateOwner(ownerUuid, "error.owner-only", new ClanMutator() {
            public Result apply(Clan clan) {
                clan.setPublic(enabled);
                return Result.OK;
            }
        });
    }

    public Result setColor(UUID ownerUuid, String colorId) {
        ColorOption color = colors.get(colorId);
        if (color == null) {
            return Result.error("error.color-invalid", String.valueOf(colorId));
        }
        final Clan clan = clanOf(ownerUuid);
        if (clan == null) {
            return Result.error("error.no-clan");
        }
        if (!clan.isOwner(ownerUuid)) {
            return Result.error("error.owner-only");
        }
        if (clan.getColorId().equals(color.getId())) {
            return Result.error("error.color-already", color.getId());
        }
        if (!color.isFree()) {
            if (!bank.isAvailable()) {
                return Result.error("error.economy-unavailable");
            }
            if (!bank.withdraw(ownerUuid, color.getCost())) {
                return Result.error("error.insufficient-funds", bank.format(color.getCost()));
            }
        }
        clan.setColorId(color.getId());
        store.saveClan(clan);
        for (UUID member : clan.memberIds()) {
            updater.updatePlayer(member);
        }
        return Result.OK;
    }

    // -------------------------------------------------------------- promotions

    public Result promote(UUID ownerUuid, String targetName, String rankArg) {
        Clan clan = clanOf(ownerUuid);
        if (clan == null) {
            return Result.error("error.no-clan");
        }
        if (!clan.isOwner(ownerUuid)) {
            return Result.error("error.owner-only");
        }
        UUID target = lookup.findByName(targetName);
        if (target == null) {
            return Result.error("error.player-not-found", targetName);
        }
        if (clan.isOwner(target)) {
            return Result.error("error.cannot-promote-owner", targetName);
        }
        if (!clan.hasMember(target)) {
            return Result.error("error.target-not-in-clan", targetName);
        }
        Rank rank = rankArg == null ? null : rankArg.equalsIgnoreCase("og") ? Rank.OG
                : rankArg.equalsIgnoreCase("admin") ? Rank.ADMIN : null;
        if (rank == Rank.OG && clan.isOwner(target)) {
            rank = null;
        }
        if (rank == null) {
            return Result.error("error.rank-invalid", String.valueOf(rankArg));
        }
        Rank current = clan.rankOf(target);
        if (current == rank) {
            return Result.error("error.already-rank", targetName, rank.getDisplay());
        }
        clan.setRank(target, rank);
        store.saveClan(clan);
        updater.updatePlayer(target);
        return Result.OK;
    }

    public Result demote(UUID ownerUuid, String targetName) {
        Clan clan = clanOf(ownerUuid);
        if (clan == null) {
            return Result.error("error.no-clan");
        }
        if (!clan.isOwner(ownerUuid)) {
            return Result.error("error.owner-only");
        }
        UUID target = lookup.findByName(targetName);
        if (target == null) {
            return Result.error("error.player-not-found", targetName);
        }
        if (clan.isOwner(target)) {
            return Result.error("error.cannot-demote-owner", targetName);
        }
        if (!clan.hasMember(target)) {
            return Result.error("error.target-not-in-clan", targetName);
        }
        Rank current = clan.rankOf(target);
        if (current != Rank.ADMIN && current != Rank.OG) {
            return Result.error("error.not-promoted", targetName);
        }
        clan.setRank(target, Rank.MEMBER);
        store.saveClan(clan);
        updater.updatePlayer(target);
        return Result.OK;
    }

    // -------------------------------------------------------------- vault

    public Result deposit(UUID uuid, double amount) {
        if (!bank.isAvailable()) {
            return Result.error("error.economy-unavailable");
        }
        if (amount <= 0.0D) {
            return Result.error("error.amount-positive");
        }
        Clan clan = clanOf(uuid);
        if (clan == null) {
            return Result.error("error.no-clan");
        }
        if (!bank.withdraw(uuid, amount)) {
            return Result.error("error.insufficient-funds", bank.format(amount));
        }
        clan.addToVault(amount);
        store.saveClan(clan);
        return Result.OK;
    }

    public Result withdraw(UUID uuid, double amount) {
        if (!bank.isAvailable()) {
            return Result.error("error.economy-unavailable");
        }
        if (amount <= 0.0D) {
            return Result.error("error.amount-positive");
        }
        Clan clan = clanOf(uuid);
        if (clan == null) {
            return Result.error("error.no-clan");
        }
        if (!clan.isOwner(uuid)) {
            return Result.error("error.owner-only");
        }
        if (clan.getVault() < amount) {
            return Result.error("error.vault-insufficient", bank.format(clan.getVault()));
        }
        if (!bank.deposit(uuid, amount)) {
            return Result.error("error.economy-transfer-failed");
        }
        clan.removeFromVault(amount);
        store.saveClan(clan);
        return Result.OK;
    }

    // -------------------------------------------------------------- tax

    public List<TaxSummary> collectTaxes(long now) {
        List<TaxSummary> out = new ArrayList<TaxSummary>();
        if (!bank.isAvailable()) {
            return out;
        }
        boolean salary = !"collect".equalsIgnoreCase(settings.getTaxMode());
        for (Clan clan : store.allClans()) {
            if (clan.getTax() <= 0.0D) {
                continue;
            }
            double tax = clan.getTax();
            int paid = 0;
            int failed = 0;
            double total = 0.0D;
            double ogShare = 0.0D;
            for (UUID member : clan.memberIds()) {
                if (clan.isOg(member)) {
                    continue;
                }
                if (settings.isTaxExemptOwner() && clan.isOwner(member)) {
                    continue;
                }
                if (salary) {
                    // Vault pays the member; OGs additionally receive <og-percent> of the amount.
                    double share = ogShareFor(clan, tax);
                    double due = tax + share;
                    if (clan.getVault() + 0.0001D >= due) {
                        clan.removeFromVault(due);
                        bank.deposit(member, tax);
                        payOgs(clan, share);
                        total += tax;
                        ogShare += share;
                        paid++;
                    } else {
                        failed++;
                    }
                } else {
                    if (bank.withdraw(member, tax)) {
                        total += tax;
                        paid++;
                    } else {
                        failed++;
                    }
                }
            }
            if (!salary && total > 0.0D) {
                ogShare = total * settings.getTaxOgPercent() / 100.0D;
                if (ogShare > total) {
                    ogShare = total;
                }
                List<UUID> ogs = new ArrayList<UUID>(clan.getOgs());
                if (ogs.isEmpty()) {
                    ogShare = 0.0D;
                } else {
                    double perOg = ogShare / ogs.size();
                    for (UUID og : ogs) {
                        bank.deposit(og, perOg);
                    }
                }
                clan.addToVault(total - ogShare);
            }
            if (paid > 0 || failed > 0) {
                store.saveClan(clan);
            }
            out.add(new TaxSummary(clan.getKey(), tax, paid, failed, total, ogShare));
        }
        return out;
    }

    private double ogShareFor(Clan clan, double amount) {
        if (clan.getOgs().isEmpty()) {
            return 0.0D;
        }
        double share = amount * settings.getTaxOgPercent() / 100.0D;
        if (share > amount) {
            share = amount;
        }
        return share;
    }

    private void payOgs(Clan clan, double share) {
        if (share <= 0.0D || clan.getOgs().isEmpty()) {
            return;
        }
        double perOg = share / clan.getOgs().size();
        for (UUID og : clan.getOgs()) {
            bank.deposit(og, perOg);
        }
    }

    // -------------------------------------------------------------- helpers

    public boolean isKickBanned(UUID uuid, Clan clan, long now) {
        Long until = clan.getKicks().get(uuid);
        return until != null && until.longValue() > now;
    }

    public List<Clan> allClans() {
        List<Clan> list = new ArrayList<Clan>(store.allClans());
        java.util.Collections.sort(list, new Comparator<Clan>() {
            public int compare(Clan a, Clan b) {
                return a.getKey().compareTo(b.getKey());
            }
        });
        return list;
    }

    public Collection<PlayerData> allPlayers() {
        return store.allPlayers();
    }

    private Result mutateOwner(UUID ownerUuid, String key, ClanMutator mutator) {
        Clan clan = clanOf(ownerUuid);
        if (clan == null) {
            return Result.error("error.no-clan");
        }
        if (!clan.isOwner(ownerUuid)) {
            return Result.error(key);
        }
        Result r = mutator.apply(clan);
        if (r.ok()) {
            store.saveClan(clan);
        }
        return r;
    }

    private interface ClanMutator {
        Result apply(Clan clan);
    }
}
