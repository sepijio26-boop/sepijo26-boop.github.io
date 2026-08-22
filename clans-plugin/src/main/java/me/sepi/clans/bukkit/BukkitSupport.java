package me.sepi.clans.bukkit;

import me.sepi.clans.ClansPlugin;

import me.sepi.clans.core.Bank;
import me.sepi.clans.core.PlayerLookup;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;

import java.util.UUID;

/**
 * Vault-backed {@link Bank} and Bukkit-backed player name lookup.
 */
public final class BukkitSupport implements PlayerLookup, Bank {

    private final ClansPlugin plugin;
    private volatile Economy economy;

    public BukkitSupport(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void hook() {
        economy = null;
        ServicesManager services = Bukkit.getServer().getServicesManager();
        RegisteredServiceProvider<Economy> registration = services.getRegistration(Economy.class);
        if (registration != null) {
            economy = registration.getProvider();
            plugin.getLogger().info("Hooked into Vault economy: " + economy.getName());
        } else {
            plugin.getLogger().warning("Vault economy not found - paid colours and taxes are disabled.");
        }
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public double balance(UUID player) {
        if (economy == null) {
            return 0.0D;
        }
        return economy.getBalance(Bukkit.getOfflinePlayer(player));
    }

    public boolean withdraw(UUID player, double amount) {
        if (economy == null) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(Bukkit.getOfflinePlayer(player), amount);
        return response != null && response.transactionSuccess();
    }

    public boolean deposit(UUID player, double amount) {
        if (economy == null) {
            return false;
        }
        EconomyResponse response = economy.depositPlayer(Bukkit.getOfflinePlayer(player), amount);
        return response != null && response.transactionSuccess();
    }

    public String format(double amount) {
        if (economy == null) {
            return String.valueOf(amount);
        }
        return economy.format(amount);
    }

    public UUID findByName(String name) {
        if (name == null) {
            return null;
        }
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline != null && offline.getUniqueId() != null) {
            return offline.getUniqueId();
        }
        return null;
    }
}
