package net.kyori.adventure.text.serializer.legacy;
import net.kyori.adventure.text.Component;
public class LegacyComponentSerializer {
    public static LegacyComponentSerializer legacyAmpersand() { return new LegacyComponentSerializer(); }
    public Component deserialize(String text) { return Component.empty(); }
    public String serialize(Component component) { return ""; }
}
