package io.papermc.paper.chat;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
public abstract class ChatRenderer {
    public abstract Component render(Player source, Component sourceDisplayName, Component message, Audience viewer);
    public static ChatRenderer defaultRenderer() { return null; }
    public static ChatRenderer viewerUnaware(ViewerUnaware renderer) { return null; }
    @FunctionalInterface
    public interface ViewerUnaware {
        Component render(Player source, Component sourceDisplayName, Component message);
    }
}
