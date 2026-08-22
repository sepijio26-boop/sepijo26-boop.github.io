package net.kyori.adventure.text;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
public interface Component {
    static Component empty() { return new Impl(); }
    static Component text(String text) { return new Impl(); }
    Component append(Component other);
    Component clickEvent(ClickEvent event);
    Component hoverEvent(HoverEvent<?> event);
    final class Impl implements Component {
        public Component append(Component other) { return this; }
        public Component clickEvent(ClickEvent event) { return this; }
        public Component hoverEvent(HoverEvent<?> event) { return this; }
    }
}
