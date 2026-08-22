package org.bukkit;
import java.util.UUID;
public interface OfflinePlayer {
    String getName();
    UUID getUniqueId();
    boolean isOnline();
}
