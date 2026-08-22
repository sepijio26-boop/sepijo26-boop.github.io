package me.sepi.clans;

import me.sepi.clans.bukkit.BukkitSupport;
import me.sepi.clans.bukkit.ChatListener;
import me.sepi.clans.bukkit.CommandHandler;
import me.sepi.clans.bukkit.DisplaySettings;
import me.sepi.clans.bukkit.JoinListener;
import me.sepi.clans.bukkit.Messages;
import me.sepi.clans.bukkit.PvpListener;
import me.sepi.clans.bukkit.TagManager;
import me.sepi.clans.bukkit.TaxTask;
import me.sepi.clans.bukkit.YamlStore;
import me.sepi.clans.core.ClanManager;
import me.sepi.clans.core.ClanSettings;
import me.sepi.clans.core.ColorRegistry;
import me.sepi.clans.core.TagUpdater;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.UUID;

public final class ClansPlugin extends JavaPlugin {

    private ClanSettings settings;
    private ColorRegistry colors;
    private DisplaySettings display;
    private Messages messages;
    private YamlStore store;
    private BukkitSupport support;
    private TagManager tags;
    private ClanManager manager;
    private BukkitTask taxTask;

    @Override
    public void onEnable() {
        saveResource("config.yml", false);
        saveResource("messages.yml", false);

        settings = new ClanSettings();
        colors = new ColorRegistry();
        display = new DisplaySettings();
        loadConfiguration();

        messages = new Messages(this);
        messages.load(new File(getDataFolder(), "messages.yml"));

        support = new BukkitSupport(this);
        support.hook();

        store = new YamlStore(this);
        store.load();

        final TagUpdater updater = new TagUpdater() {
            public void updatePlayer(UUID uuid) {
                if (tags != null) {
                    tags.updatePlayer(uuid);
                }
            }
        };
        manager = new ClanManager(store, colors, settings, support, updater, support);
        tags = new TagManager(this, manager, display);

        getServer().getPluginManager().registerEvents(new ChatListener(this, manager, tags, display), this);
        getServer().getPluginManager().registerEvents(new PvpListener(manager), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this, manager, tags), this);

        PluginCommand command = getCommand("clan");
        if (command != null) {
            CommandHandler handler = new CommandHandler(this, manager, messages);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }

        scheduleTax();
        tags.applyAll();
        getLogger().info("Clans enabled. " + colors.size() + " colours, data folder " + getDataFolder().getPath());
    }

    @Override
    public void onDisable() {
        if (taxTask != null) {
            taxTask.cancel();
            taxTask = null;
        }
        getLogger().info("Clans disabled.");
    }

    public void reloadAll() {
        reloadConfig();
        loadConfiguration();
        messages.load(new File(getDataFolder(), "messages.yml"));
        if (taxTask != null) {
            taxTask.cancel();
        }
        scheduleTax();
        if (tags != null) {
            tags.applyAll();
        }
    }

    private void loadConfiguration() {
        FileConfiguration cfg = getConfig();
        settings.setDefaultPublic(cfg.getBoolean("default-public", true));
        settings.setDefaultPvp(cfg.getBoolean("default-pvp", true));
        settings.setInviteExpiryMinutes(Math.max(1, cfg.getInt("invite.expiry-minutes", 10)));
        settings.setKickCooldownDays(Math.max(1, cfg.getInt("kick-cooldown-days", 7)));
        settings.setTaxIntervalMinutes(Math.max(1, cfg.getInt("tax.interval-minutes", 60)));
        settings.setTaxOgPercent(Math.max(0.0D, Math.min(100.0D, cfg.getDouble("tax.og-percent", 20.0D))));
        settings.setTaxExemptOwner(cfg.getBoolean("tax.exempt-owner", true));
        settings.setTaxMode(cfg.getString("tax.mode", "salary"));
        display.load(cfg);

        colors.clear();
        ConfigurationSection section = cfg.getConfigurationSection("colors");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                String base = "colors." + id;
                colors.register(
                        id,
                        cfg.getString(base + ".legacy", "&f"),
                        cfg.getString(base + ".chat", "WHITE"),
                        cfg.getDouble(base + ".cost", 0.0D));
            }
        }
    }

    private void scheduleTax() {
        long interval = Math.max(1L, settings.getTaxIntervalMinutes()) * 60L * 20L;
        taxTask = getServer().getScheduler().runTaskTimer(this, new TaxTask(this, manager, messages), interval, interval);
    }

    public ClanManager getManager() {
        return manager;
    }

    public Messages getMessages() {
        return messages;
    }

    public BukkitSupport getBank() {
        return support;
    }

    public DisplaySettings getDisplay() {
        return display;
    }
}
