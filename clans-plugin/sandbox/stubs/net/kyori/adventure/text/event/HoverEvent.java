package net.kyori.adventure.text.event;
import net.kyori.adventure.text.Component;
public final class HoverEvent<V> {
    public static HoverEvent<Component> showText(Component text) { return new HoverEvent<Component>(); }
}
