package io.papermc.paper.event.player;
import io.papermc.paper.chat.ChatRenderer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
public class AsyncChatEvent extends Event {
    public Player getPlayer() { return null; }
    public void renderer(ChatRenderer renderer) { }
}
