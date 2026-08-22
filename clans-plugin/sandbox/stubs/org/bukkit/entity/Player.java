package org.bukkit.entity;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import java.util.UUID;
public interface Player extends LivingEntity, CommandSender {
    String getName();
    UUID getUniqueId();
    boolean isOnline();
    void playerListName(Component name);
    Component playerListName();
}
