package org.bukkit.event.entity;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
public class EntityDamageByEntityEvent extends Event {
    public Entity getDamager() { return null; }
    public Entity getEntity() { return null; }
    public boolean isCancelled() { return false; }
    public void setCancelled(boolean cancel) { }
}
