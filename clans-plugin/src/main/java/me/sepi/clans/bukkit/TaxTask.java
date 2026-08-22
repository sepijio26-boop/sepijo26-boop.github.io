package me.sepi.clans.bukkit;

import me.sepi.clans.ClansPlugin;

import me.sepi.clans.core.ClanManager;
import me.sepi.clans.core.TaxSummary;
import me.sepi.clans.model.Clan;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Hourly (configurable) tax collection. The task keeps running even while the
 * owner is offline; whenever a collection happens the owner is notified.
 */
public final class TaxTask implements Runnable {

    private final ClansPlugin plugin;
    private final ClanManager manager;
    private final Messages messages;

    public TaxTask(ClansPlugin plugin, ClanManager manager, Messages messages) {
        this.plugin = plugin;
        this.manager = manager;
        this.messages = messages;
    }

    public void run() {
        List<TaxSummary> summaries = manager.collectTaxes(System.currentTimeMillis());
        if (summaries.isEmpty()) {
            return;
        }
        for (TaxSummary summary : summaries) {
            Clan clan = manager.clan(summary.getClanKey());
            if (clan == null) {
                continue;
            }
            Player owner = Bukkit.getPlayer(clan.getOwner());
            if (owner == null || !owner.isOnline()) {
                continue;
            }
            String money = manager.getBank().format(summary.getTotal());
            owner.sendMessage(messages.msg("tax.summary",
                    "clan", "[" + clan.getKey() + "]",
                    "amount", money,
                    "paid", String.valueOf(summary.getPaid()),
                    "failed", String.valueOf(summary.getFailed()),
                    "og", manager.getBank().format(summary.getOgShare()),
                    "tax", manager.getBank().format(summary.getTax())));
        }
    }
}
