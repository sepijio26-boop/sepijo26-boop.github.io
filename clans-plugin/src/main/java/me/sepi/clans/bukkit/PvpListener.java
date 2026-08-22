package me.sepi.clans.bukkit;

import me.sepi.clans.ClansPlugin;

import me.sepi.clans.core.ClanManager;
import me.sepi.clans.core.ClanRules;
import me.sepi.clans.model.Clan;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ProjectileSource;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Clan PvP rules.
 *
 * <ul>
 *   <li>pvp off: clan members cannot damage each other at all.</li>
 *   <li>pvp on (default): members can fight each other, but the owner and OGs
 *       are always protected from clan member damage.</li>
 * </ul>
 */
public final class PvpListener implements Listener {

    private final ClanManager manager;

    public PvpListener(ClanManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = false)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        Player damager = playerDamager(event.getDamager());
        if (damager == null) {
            return;
        }
        Clan clan = manager.clanOf(victim.getUniqueId());
        if (clan == null || !clan.hasMember(damager.getUniqueId())) {
            return;
        }
        if (ClanRules.isPvpBlocked(clan, victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private Player playerDamager(Entity entity) {
        if (entity instanceof Player) {
            return (Player) entity;
        }
        if (entity instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) entity).getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        }
        return null;
    }
}
