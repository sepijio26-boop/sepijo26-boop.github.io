import me.sepi.clans.core.Bank;
import me.sepi.clans.core.ClanManager;
import me.sepi.clans.core.ClanName;
import me.sepi.clans.core.ClanRules;
import me.sepi.clans.core.ClanSettings;
import me.sepi.clans.core.ClanStore;
import me.sepi.clans.core.ColorRegistry;
import me.sepi.clans.core.PlayerLookup;
import me.sepi.clans.core.Result;
import me.sepi.clans.core.TagUpdater;
import me.sepi.clans.core.TaxSummary;
import me.sepi.clans.model.Clan;
import me.sepi.clans.model.ColorOption;
import me.sepi.clans.model.PlayerData;
import me.sepi.clans.model.Rank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Headless logic tests for the clan plugin. Runs inside the TraceJVM engine
 * (Java 23) - no Bukkit required.
 */
public final class TestHarness {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testClanName();
        testColorRegistry();
        testCreate();
        testJoinAndInvite();
        testKickCooldown();
        testPromote();
        testPvpRules();
        testTax();
        testTaxSalaryMode();
        testVault();
        System.out.println("RESULT total=" + (passed + failed) + " passed=" + passed + " failed=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
        System.exit(0);
    }

    // ---------------------------------------------------------- helpers

    private static void check(boolean condition, String name) {
        if (condition) {
            passed++;
            System.out.println("PASS " + name);
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }

    private static void checkResult(Result r, String name) {
        check(r != null && r.ok(), name + " (error=" + (r == null ? "null" : r.getErrorKey()) + ")");
    }

    private static void checkError(String expectedKey, Result r, String name) {
        check(r != null && !r.ok() && expectedKey.equals(r.getErrorKey()),
                name + " (expected=" + expectedKey + " got=" + (r == null ? "null" : r.getErrorKey()) + ")");
    }

    private static final class MemStore implements ClanStore {
        final Map<String, Clan> clans = new HashMap<String, Clan>();
        final Map<UUID, PlayerData> players = new HashMap<UUID, PlayerData>();

        public Clan getClan(String key) {
            return clans.get(key);
        }

        public Collection<Clan> allClans() {
            return new ArrayList<Clan>(clans.values());
        }

        public void saveClan(Clan clan) {
            clans.put(clan.getKey(), clan);
        }

        public void removeClan(String key) {
            clans.remove(key);
        }

        public PlayerData getPlayer(UUID uuid) {
            return players.get(uuid);
        }

        public Collection<PlayerData> allPlayers() {
            return new ArrayList<PlayerData>(players.values());
        }

        public void savePlayer(PlayerData data) {
            players.put(data.getUuid(), data);
        }

        public void removePlayer(UUID uuid) {
            players.remove(uuid);
        }
    }

    private static final class FakeBank implements Bank {
        final Map<UUID, Double> balances = new HashMap<UUID, Double>();

        void set(UUID uuid, double amount) {
            balances.put(uuid, Double.valueOf(amount));
        }

        public boolean isAvailable() {
            return true;
        }

        public double balance(UUID player) {
            Double d = balances.get(player);
            return d == null ? 0.0D : d.doubleValue();
        }

        public boolean withdraw(UUID player, double amount) {
            double b = balance(player);
            if (b < amount) {
                return false;
            }
            balances.put(player, Double.valueOf(b - amount));
            return true;
        }

        public boolean deposit(UUID player, double amount) {
            balances.put(player, Double.valueOf(balance(player) + amount));
            return true;
        }

        public String format(double amount) {
            return String.valueOf(amount);
        }
    }

    private static final class FakeLookup implements PlayerLookup {
        final Map<String, UUID> names = new HashMap<String, UUID>();

        public UUID findByName(String name) {
            return names.get(name.toLowerCase());
        }

        void add(String name, UUID uuid) {
            names.put(name.toLowerCase(), uuid);
        }
    }

    private static UUID uuid(String seed) {
        return UUID.nameUUIDFromBytes(("test-" + seed).getBytes());
    }

    private static ColorRegistry registry() {
        ColorRegistry colors = new ColorRegistry();
        colors.register("red", "&c", "RED", 0);
        colors.register("blue", "&9", "BLUE", 0);
        colors.register("yellow", "&e", "YELLOW", 0);
        colors.register("green", "&a", "GREEN", 20000);
        colors.register("gold", "&6", "GOLD", 25000);
        colors.register("aqua", "&b", "AQUA", 30000);
        colors.register("purple", "&5", "DARK_PURPLE", 35000);
        colors.register("pink", "&d", "LIGHT_PURPLE", 40000);
        colors.register("white", "&f", "WHITE", 50000);
        colors.register("gray", "&7", "GRAY", 75000);
        return colors;
    }

    private static ClanManager manager(MemStore store, FakeBank bank, FakeLookup lookup, TagUpdater updater) {
        ClanSettings settings = new ClanSettings();
        return new ClanManager(store, registry(), settings, lookup, updater, bank);
    }

    private static TagUpdater noop() {
        return new TagUpdater() {
            public void updatePlayer(UUID uuid) {
            }
        };
    }

    // ---------------------------------------------------------- tests

    private static void testClanName() {
        check("sepi".equalsIgnoreCase(ClanName.normalize("sepi")) || "SEPI".equals(ClanName.normalize("sepi")), "name normalizes to upper");
        check("SEPI".equals(ClanName.normalize("SePi")), "name mixed case uppercased");
        check("S3#".equals(ClanName.normalize("s3#")), "digits and hash allowed");
        check("12".equals(ClanName.normalize("12")), "digits only allowed");
        check(ClanName.normalize("A") == null, "1 char rejected");
        check(ClanName.normalize("ABCDE") == null, "5 chars rejected");
        check(ClanName.normalize("A B") == null, "space rejected");
        check(ClanName.normalize("A$B") == null, "symbol rejected");
    }

    private static void testColorRegistry() {
        ColorRegistry c = registry();
        check(c.size() == 10, "10 colours registered");
        ColorOption red = c.get("RED");
        check(red != null && red.isFree() && "&c".equals(red.getLegacyCode()), "red free with &c");
        ColorOption green = c.get("green");
        check(green != null && green.getCost() == 20000.0D, "green costs 20000");
        check(c.get("missing") == null, "unknown colour is null");
    }

    private static void testCreate() {
        MemStore store = new MemStore();
        FakeBank bank = new FakeBank();
        FakeLookup lookup = new FakeLookup();
        ClanManager m = manager(store, bank, lookup, noop());
        UUID a = uuid("alice");
        bank.set(a, 100000);

        checkResult(m.create(a, "sepi", "red"), "create red clan");
        Clan clan = store.getClan("SEPI");
        check(clan != null && clan.isOwner(a), "owner stored");
        check(clan.isPvp(), "pvp defaults on");
        check(clan.isPublic(), "public defaults on");
        check(clan.getColorId().equals("red"), "color stored");

        checkError("error.already-in-clan", m.create(a, "other", "red"), "cannot create twice");
        checkError("error.name-taken", m.create(uuid("b"), "SEPI", "red"), "duplicate name rejected");

        UUID rich = uuid("rich");
        bank.set(rich, 100000);
        checkResult(m.create(rich, "rich", "green"), "create paid colour rich");
        check(bank.balance(rich) == 80000.0D, "20000 deducted for green");

        UUID poor = uuid("poor");
        bank.set(poor, 100);
        checkError("error.insufficient-funds", m.create(poor, "poor", "gold"), "paid colour without money");
    }

    private static void testJoinAndInvite() {
        MemStore store = new MemStore();
        FakeBank bank = new FakeBank();
        FakeLookup lookup = new FakeLookup();
        ClanManager m = manager(store, bank, lookup, noop());
        UUID owner = uuid("owner");
        UUID b = uuid("bob");
        UUID c = uuid("carol");
        lookup.add("owner", owner);
        lookup.add("bob", b);
        lookup.add("carol", c);

        checkResult(m.create(owner, "SEPI", "red"), "create");
        checkResult(m.join(b, "bob", "sepi"), "public join");
        checkError("error.already-in-clan", m.join(b, "bob", "SEPI"), "double join");

        // private clan blocks join
        checkResult(m.setPublic(owner, false), "set private");
        checkError("error.clan-private", m.join(c, "carol", "SEPI"), "private join blocked");

        // invite + accept bypasses privacy
        checkResult(m.invite(owner, "carol"), "invite carol");
        checkResult(m.accept(c, null), "accept invite");
        Clan clan = store.getClan("SEPI");
        check(clan.hasMember(c), "carol in clan");

        // decline flow
        UUID d = uuid("dave");
        lookup.add("dave", d);
        checkResult(m.invite(owner, "dave"), "invite dave");
        checkResult(m.decline(d, null), "decline");
        checkError("error.no-invite", m.accept(d, null), "no invite after decline");

        // expired invite is ignored
        UUID e = uuid("eve");
        lookup.add("eve", e);
        PlayerData ed = m.playerData(e);
        ed.addInvite("SEPI", Long.valueOf(System.currentTimeMillis() - 1000L));
        store.savePlayer(ed);
        checkError("error.no-invite", m.accept(e, null), "expired invite ignored");

        // non-owner cannot invite
        UUID f = uuid("frank");
        lookup.add("frank", f);
        checkError("error.owner-only", m.invite(b, "frank"), "non-owner invite blocked");
    }

    private static void testKickCooldown() {
        MemStore store = new MemStore();
        FakeBank bank = new FakeBank();
        FakeLookup lookup = new FakeLookup();
        ClanManager m = manager(store, bank, lookup, noop());
        UUID owner = uuid("owner");
        UUID b = uuid("bob");
        lookup.add("owner", owner);
        lookup.add("bob", b);

        checkResult(m.create(owner, "SEPI", "red"), "create public");
        checkResult(m.join(b, "bob", "SEPI"), "join");
        checkResult(m.kick(owner, "bob"), "kick bob");
        Clan clan = store.getClan("SEPI");
        check(!clan.hasMember(b), "bob removed");
        checkError("error.kicked-cooldown", m.join(b, "bob", "SEPI"), "public rejoin blocked by cooldown");

        // Re-invite overrides the cooldown
        checkResult(m.invite(owner, "bob"), "re-invite bob");
        checkResult(m.accept(b, null), "bob rejoins via invite");
        check(clan.hasMember(b), "bob back in clan");
    }

    private static void testPromote() {
        MemStore store = new MemStore();
        FakeBank bank = new FakeBank();
        FakeLookup lookup = new FakeLookup();
        ClanManager m = manager(store, bank, lookup, noop());
        UUID owner = uuid("owner");
        UUID b = uuid("bob");
        lookup.add("owner", owner);
        lookup.add("bob", b);

        checkResult(m.create(owner, "SEPI", "red"), "create");
        checkResult(m.join(b, "bob", "SEPI"), "join");
        checkResult(m.promote(owner, "bob", "admin"), "promote admin");
        check(m.rankOf(b) == Rank.ADMIN, "rank admin");
        checkResult(m.promote(owner, "bob", "og"), "promote og");
        check(m.rankOf(b) == Rank.OG, "rank og");
        checkResult(m.demote(owner, "bob"), "demote");
        check(m.rankOf(b) == Rank.MEMBER, "rank member");
        checkError("error.not-promoted", m.demote(owner, "bob"), "demote member fails");
        checkError("error.owner-only", m.promote(b, "owner", "admin"), "non-owner cannot promote");
        checkError("error.cannot-promote-owner", m.promote(owner, "owner", "admin"), "cannot promote owner");
    }

    private static void testPvpRules() {
        MemStore store = new MemStore();
        FakeBank bank = new FakeBank();
        FakeLookup lookup = new FakeLookup();
        ClanManager m = manager(store, bank, lookup, noop());
        UUID owner = uuid("owner");
        UUID og = uuid("og");
        UUID member = uuid("member");
        lookup.add("owner", owner);
        lookup.add("og", og);
        lookup.add("member", member);

        checkResult(m.create(owner, "SEPI", "red"), "create");
        checkResult(m.join(og, "og", "SEPI"), "og joins");
        checkResult(m.join(member, "member", "SEPI"), "member joins");
        checkResult(m.promote(owner, "og", "og"), "promote og");
        Clan clan = store.getClan("SEPI");

        // pvp ON (default): member vs member allowed
        check(!ClanRules.isPvpBlocked(clan, member), "member can fight member (pvp on)");
        // owner always protected
        check(ClanRules.isPvpBlocked(clan, owner), "owner protected even when pvp on");
        // og always protected
        check(ClanRules.isPvpBlocked(clan, og), "og protected even when pvp on");

        checkResult(m.setPvp(owner, false), "pvp off");
        check(ClanRules.isPvpBlocked(clan, member), "all blocked when pvp off");
        check(ClanRules.isPvpBlocked(clan, owner), "owner blocked when pvp off");
    }

    private static void testTax() {
        MemStore store = new MemStore();
        FakeBank bank = new FakeBank();
        FakeLookup lookup = new FakeLookup();
        ClanManager m = manager(store, bank, lookup, noop());
        UUID owner = uuid("owner");
        UUID og = uuid("og");
        UUID m1 = uuid("m1");
        UUID m2 = uuid("m2");
        lookup.add("owner", owner);
        lookup.add("og", og);
        lookup.add("m1", m1);
        lookup.add("m2", m2);
        bank.set(owner, 100000);
        bank.set(og, 100000);
        bank.set(m1, 100000);
        bank.set(m2, 100000);

        checkResult(m.create(owner, "SEPI", "red"), "create");
        checkResult(m.join(og, "og", "SEPI"), "og join");
        checkResult(m.join(m1, "m1", "SEPI"), "m1 join");
        checkResult(m.join(m2, "m2", "SEPI"), "m2 join");
        checkResult(m.promote(owner, "og", "og"), "promote og");
        m.getSettings().setTaxMode("collect");
        checkResult(m.setTax(owner, 200.0D), "tax 200");

        List<TaxSummary> summaries = m.collectTaxes(System.currentTimeMillis());
        check(summaries.size() == 1, "one summary");
        TaxSummary s = summaries.get(0);
        check(s.getPaid() == 2, "2 members paid (owner+og exempt)");
        check(s.getTotal() == 400.0D, "total 400");
        check(s.getOgShare() == 80.0D, "og gets 20% = 80");
        Clan clan = store.getClan("SEPI");
        check(clan.getVault() == 320.0D, "vault keeps 320");
        check(bank.balance(m1) == 99800.0D, "m1 paid 200");
        check(bank.balance(m2) == 99800.0D, "m2 paid 200");
        check(bank.balance(og) == 100080.0D, "og received 80");
        check(bank.balance(owner) == 100000.0D, "owner did not pay");

        checkResult(m.setTax(owner, 0.0D), "tax off");
        List<TaxSummary> none = m.collectTaxes(System.currentTimeMillis());
        check(none.isEmpty(), "no collection when tax off");
    }

    private static void testTaxSalaryMode() {
        MemStore store = new MemStore();
        FakeBank bank = new FakeBank();
        FakeLookup lookup = new FakeLookup();
        ClanManager m = manager(store, bank, lookup, noop());
        UUID owner = uuid("owner");
        UUID og = uuid("og");
        UUID m1 = uuid("m1");
        UUID m2 = uuid("m2");
        lookup.add("owner", owner);
        lookup.add("og", og);
        lookup.add("m1", m1);
        lookup.add("m2", m2);
        bank.set(owner, 100000);
        bank.set(og, 100000);
        bank.set(m1, 100000);
        bank.set(m2, 100000);

        checkResult(m.create(owner, "SEPI", "red"), "salary create");
        checkResult(m.join(og, "og", "SEPI"), "salary og join");
        checkResult(m.join(m1, "m1", "SEPI"), "salary m1 join");
        checkResult(m.join(m2, "m2", "SEPI"), "salary m2 join");
        checkResult(m.promote(owner, "og", "og"), "salary promote og");
        checkResult(m.deposit(owner, 1000), "salary fund vault");
        checkResult(m.setTax(owner, 200.0D), "salary tax 200");

        List<TaxSummary> summaries = m.collectTaxes(System.currentTimeMillis());
        check(summaries.size() == 1, "salary one summary");
        TaxSummary s = summaries.get(0);
        check(s.getPaid() == 2, "salary 2 members paid");
        check(s.getTotal() == 400.0D, "salary total 400");
        check(s.getOgShare() == 80.0D, "salary og extra 80");
        Clan clan = store.getClan("SEPI");
        // vault: 1000 - (200+40) - (200+40) = 520
        check(clan.getVault() == 520.0D, "salary vault 520");
        check(bank.balance(m1) == 100200.0D, "salary m1 +200");
        check(bank.balance(m2) == 100200.0D, "salary m2 +200");
        check(bank.balance(og) == 100080.0D, "salary og +80");
        check(bank.balance(owner) == 99000.0D, "salary owner paid 1000 deposit only");

        // Not enough money in the vault -> skipped
        checkResult(m.setTax(owner, 5000.0D), "salary high tax");
        List<TaxSummary> next = m.collectTaxes(System.currentTimeMillis());
        TaxSummary s2 = next.get(0);
        check(s2.getPaid() == 0 && s2.getFailed() == 2, "salary insufficient vault skipped");
    }

    private static void testVault() {
        MemStore store = new MemStore();
        FakeBank bank = new FakeBank();
        FakeLookup lookup = new FakeLookup();
        ClanManager m = manager(store, bank, lookup, noop());
        UUID owner = uuid("owner");
        UUID b = uuid("bob");
        lookup.add("owner", owner);
        lookup.add("bob", b);
        bank.set(owner, 100000);
        bank.set(b, 100000);

        checkResult(m.create(owner, "SEPI", "red"), "create");
        checkResult(m.join(b, "bob", "SEPI"), "join");
        checkResult(m.deposit(b, 500), "bob deposits 500");
        check(store.getClan("SEPI").getVault() == 500.0D, "vault 500");
        checkError("error.owner-only", m.withdraw(b, 100), "member cannot withdraw");
        checkResult(m.withdraw(owner, 100), "owner withdraws 100");
        check(store.getClan("SEPI").getVault() == 400.0D, "vault 400 after withdraw");
        checkError("error.vault-insufficient", m.withdraw(owner, 999999), "cannot withdraw more than vault");
    }
}
