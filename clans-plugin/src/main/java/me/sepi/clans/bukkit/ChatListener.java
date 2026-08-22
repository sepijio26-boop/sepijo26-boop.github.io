package me.sepi.clans.bukkit;

import me.sepi.clans.ClansPlugin;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.sepi.clans.core.ClanManager;
import me.sepi.clans.model.Clan;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Adds the coloured clan tag to chat messages (configurable via chat.format).
 */
public final class ChatListener implements Listener {

    private final ClansPlugin plugin;
    private final ClanManager manager;
    private final TagManager tags;
    private final DisplaySettings display;

    public ChatListener(ClansPlugin plugin, ClanManager manager, TagManager tags, DisplaySettings display) {
        this.plugin = plugin;
        this.manager = manager;
        this.tags = tags;
        this.display = display;
    }

    @EventHandler(ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        if (!display.isChatEnabled()) {
            return;
        }
        final String format = display.getChatFormat();
        final String tag = tags.tagOf(manager.clanOf(event.getPlayer().getUniqueId()));
        event.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, message) ->
                Messages.compose(format, tag, sourceDisplayName, message)));
    }
}
