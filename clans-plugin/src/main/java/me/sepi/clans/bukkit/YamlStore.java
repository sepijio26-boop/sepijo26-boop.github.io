package me.sepi.clans.bukkit;

import me.sepi.clans.ClansPlugin;

import me.sepi.clans.core.ClanStore;
import me.sepi.clans.model.Clan;
import me.sepi.clans.model.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * YAML-backed implementation of {@link ClanStore}. Data is kept in memory and
 * written to plugins/Clans/data/clans.yml and players.yml on every change.
 */
public final class YamlStore implements ClanStore {

    private final ClansPlugin plugin;
    private final File clansFile;
    private final File playersFile;
    private final Map<String, Clan> clans = new ConcurrentHashMap<String, Clan>();
    private final Map<UUID, PlayerData> players = new ConcurrentHashMap<UUID, PlayerData>();

    public YamlStore(ClansPlugin plugin) {
        this.plugin = plugin;
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        this.clansFile = new File(dataDir, "clans.yml");
        this.playersFile = new File(dataDir, "players.yml");
    }

    public void load() {
        clans.clear();
        players.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(clansFile);
        ConfigurationSection root = yaml.getConfigurationSection("clans");
        if (root != null) {
            for (String key : root.getKeys(false)) {
                ConfigurationSection c = root.getConfigurationSection(key);
                if (c == null) {
                    continue;
                }
                UUID owner = uuid(c.getString("owner"));
                if (owner == null) {
                    continue;
                }
                Clan clan = new Clan(key, owner, c.getLong("created", 0L),
                        c.getString("color", "red"),
                        c.getBoolean("pvp", true),
                        c.getBoolean("public", true));
                clan.setTax(c.getDouble("tax", 0.0D));
                clan.setVault(c.getDouble("vault", 0.0D));
                ConfigurationSection members = c.getConfigurationSection("members");
                if (members != null) {
                    for (String memberUuid : members.getKeys(false)) {
                        UUID id = uuid(memberUuid);
                        if (id != null) {
                            clan.getMembers().put(id, members.getString(memberUuid, "?"));
                        }
                    }
                }
                for (String s : c.getStringList("admins")) {
                    UUID id = uuid(s);
                    if (id != null) {
                        clan.getAdmins().add(id);
                    }
                }
                for (String s : c.getStringList("ogs")) {
                    UUID id = uuid(s);
                    if (id != null) {
                        clan.getOgs().add(id);
                    }
                }
                ConfigurationSection kicks = c.getConfigurationSection("kicks");
                if (kicks != null) {
                    for (String k : kicks.getKeys(false)) {
                        UUID id = uuid(k);
                        if (id != null) {
                            clan.getKicks().put(id, Long.valueOf(kicks.getLong(k, 0L)));
                        }
                    }
                }
                clans.put(key, clan);
            }
        }
        YamlConfiguration py = YamlConfiguration.loadConfiguration(playersFile);
        ConfigurationSection prow = py.getConfigurationSection("players");
        if (prow != null) {
            for (String id : prow.getKeys(false)) {
                UUID uuid = uuid(id);
                if (uuid == null) {
                    continue;
                }
                ConfigurationSection p = prow.getConfigurationSection(id);
                if (p == null) {
                    continue;
                }
                PlayerData data = new PlayerData(uuid, p.getString("name", null));
                data.setClanKey(p.getString("clan", null));
                ConfigurationSection invites = p.getConfigurationSection("invites");
                if (invites != null) {
                    for (String clanKey : invites.getKeys(false)) {
                        data.addInvite(clanKey, Long.valueOf(invites.getLong(clanKey, 0L)));
                    }
                }
                players.put(uuid, data);
            }
        }
    }

    public Clan getClan(String key) {
        return key == null ? null : clans.get(key);
    }

    public Collection<Clan> allClans() {
        return new ArrayList<Clan>(clans.values());
    }

    public void saveClan(Clan clan) {
        clans.put(clan.getKey(), clan);
        writeClans();
    }

    public void removeClan(String key) {
        clans.remove(key);
        writeClans();
    }

    public PlayerData getPlayer(UUID uuid) {
        return uuid == null ? null : players.get(uuid);
    }

    public Collection<PlayerData> allPlayers() {
        return new ArrayList<PlayerData>(players.values());
    }

    public void savePlayer(PlayerData data) {
        players.put(data.getUuid(), data);
        writePlayers();
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
        writePlayers();
    }

    private void writeClans() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Clan clan : clans.values()) {
            String base = "clans." + clan.getKey();
            yaml.set(base + ".owner", clan.getOwner().toString());
            yaml.set(base + ".created", Long.valueOf(clan.getCreated()));
            yaml.set(base + ".color", clan.getColorId());
            yaml.set(base + ".pvp", Boolean.valueOf(clan.isPvp()));
            yaml.set(base + ".tax", Double.valueOf(clan.getTax()));
            yaml.set(base + ".public", Boolean.valueOf(clan.isPublic()));
            yaml.set(base + ".vault", Double.valueOf(clan.getVault()));
            for (Map.Entry<UUID, String> e : clan.getMembers().entrySet()) {
                yaml.set(base + ".members." + e.getKey().toString(), e.getValue());
            }
            yaml.set(base + ".admins", uuidStrings(clan.getAdmins()));
            yaml.set(base + ".ogs", uuidStrings(clan.getOgs()));
            ConfigurationSection kicks = yaml.createSection(base + ".kicks");
            for (Map.Entry<UUID, Long> e : clan.getKicks().entrySet()) {
                kicks.set(e.getKey().toString(), e.getValue());
            }
        }
        save(yaml, clansFile);
    }

    private void writePlayers() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (PlayerData data : players.values()) {
            String base = "players." + data.getUuid().toString();
            yaml.set(base + ".name", data.getLastName());
            if (data.getClanKey() != null) {
                yaml.set(base + ".clan", data.getClanKey());
            }
            ConfigurationSection invites = yaml.createSection(base + ".invites");
            for (Map.Entry<String, Long> e : data.getInvites().entrySet()) {
                invites.set(e.getKey(), e.getValue());
            }
        }
        save(yaml, playersFile);
    }

    private void save(YamlConfiguration yaml, File file) {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }

    private static List<String> uuidStrings(Set<UUID> ids) {
        List<String> out = new ArrayList<String>();
        for (UUID id : ids) {
            out.add(id.toString());
        }
        return out;
    }

    private static UUID uuid(String s) {
        if (s == null) {
            return null;
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
