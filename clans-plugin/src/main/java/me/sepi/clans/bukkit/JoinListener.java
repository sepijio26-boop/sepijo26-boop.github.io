package me.sepi.clans.bukkit;

import me.sepi.clans.ClansPlugin;

import me.sepi.clans.core.ClanManager;
import me.sepi.clans.model.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Keeps last-known player names, applies tab/name-tag decorations on join and
 * re-sends pending clan invites.
 */
public final class JoinListener implements Listener {

    private final ClansPlugin plugin;
    private final ClanManager manager;
    private final TagManager tags;

    public JoinListener(ClansPlugin plugin, ClanManager manager, TagManager tags) {
        this.plugin = plugin;
        this.manager = manager;
        this.tags = tags;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        PlayerData data = manager.playerData(player.getUniqueId());
        data.setLastName(player.getName());
        manager.getStore().savePlayer(data);
        tags.updatePlayer(player.getUniqueId());
        if (!data.getInvites().isEmpty()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                public void run() {
                    if (player.isOnline() && manager.pendingInvite(player.getUniqueId(), null) != null) {
                        plugin.getMessages().sendRaw(player, "invite.reminder");
                    }
                }
            }, 5L);
        }
    }
}
