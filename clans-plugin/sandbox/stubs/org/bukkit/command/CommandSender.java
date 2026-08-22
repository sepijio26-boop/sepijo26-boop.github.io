package org.bukkit.command;
import net.kyori.adventure.audience.Audience;
public interface CommandSender extends Audience {
    void sendMessage(String message);
    void sendMessage(String... messages);
    String getName();
    boolean hasPermission(String permission);
}
