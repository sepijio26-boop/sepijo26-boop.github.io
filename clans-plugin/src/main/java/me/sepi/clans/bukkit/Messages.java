package me.sepi.clans.bukkit;

import me.sepi.clans.ClansPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and formats all user-facing strings from messages.yml.
 *
 * Every message is prefixed with the {@code prefix} key ("&9Clan: ").
 * Placeholders use the {name} syntax. {accept} and {decline} are special:
 * they are replaced with clickable chat buttons by {@link #acceptButton}
 * and {@link #declineButton}.
 */
public final class Messages {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final ClansPlugin plugin;
    private YamlConfiguration cfg;

    public Messages(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(File file) {
        cfg = YamlConfiguration.loadConfiguration(file);
    }

    public String raw(String key) {
        if (cfg == null) {
            return "&9Clan: &cMessage missing: " + key;
        }
        String value = cfg.getString(key, null);
        if (value == null) {
            value = "&c<missing message: " + key + ">";
        }
        return value;
    }

    public String prefix() {
        return raw("prefix");
    }

    /** Sends a simple formatted message (no clickable tokens). */
    public void sendRaw(org.bukkit.command.CommandSender who, String key) {
        who.sendMessage(msg(key));
    }

    /** Returns each line of a StringList message as a formatted component. */
    public List<Component> lines(String key) {
        List<Component> out = new java.util.ArrayList<Component>();
        List<String> list = cfg == null ? new java.util.ArrayList<String>() : cfg.getStringList(key);
        if (list.isEmpty()) {
            out.add(fromLegacy(prefix() + "&c<missing message: " + key + ">"));
            return out;
        }
        for (String line : list) {
            out.add(fromLegacy(prefix() + line));
        }
        return out;
    }

    /** Parses a legacy ampersand string into an Adventure component. */
    public static Component fromLegacy(String text) {
        return LEGACY.deserialize(text);
    }

    /**
     * Formats a message key with the prefix and placeholder pairs.
     * Pairs are {key},{value},{key},{value}...
     */
    public Component msg(String key, String... pairs) {
        String template = raw(key);
        Map<String, String> map = new LinkedHashMap<String, String>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        String text = prefix() + template;
        for (Map.Entry<String, String> e : map.entrySet()) {
            text = text.replace("{" + e.getKey() + "}", e.getValue());
        }
        return fromLegacy(text);
    }

    /** Replaces legacy format tokens while keeping Components for name/tag/message. */
    public static Component compose(String format, String tag, Component name, Component message) {
        Component out = Component.empty();
        int idx = 0;
        while (idx < format.length()) {
            int nameAt = format.indexOf("{name}", idx);
            int tagAt = format.indexOf("{tag}", idx);
            int msgAt = format.indexOf("{message}", idx);
            int next = Math.min(nameAt < 0 ? Integer.MAX_VALUE : nameAt,
                    Math.min(tagAt < 0 ? Integer.MAX_VALUE : tagAt, msgAt < 0 ? Integer.MAX_VALUE : msgAt));
            if (next == Integer.MAX_VALUE) {
                out = out.append(fromLegacy(format.substring(idx)));
                break;
            }
            if (next > idx) {
                out = out.append(fromLegacy(format.substring(idx, next)));
            }
            if (next == nameAt) {
                out = out.append(name);
                idx = next + 6;
            } else if (next == tagAt) {
                out = out.append(fromLegacy(tag));
                idx = next + 5;
            } else {
                out = out.append(message);
                idx = next + 9;
            }
        }
        return out;
    }

    public Component acceptButton(String clanKey) {
        Component label = fromLegacy("&d[&b&lACCEPT&d]");
        return label.clickEvent(ClickEvent.runCommand("/clan accept " + clanKey))
                .hoverEvent(HoverEvent.showText(fromLegacy("&bClick to accept the invite to &d[" + clanKey + "]&b.")));
    }

    public Component declineButton(String clanKey) {
        Component label = fromLegacy("&d[&c&lDECLINE&d]");
        return label.clickEvent(ClickEvent.runCommand("/clan decline " + clanKey))
                .hoverEvent(HoverEvent.showText(fromLegacy("&bClick to decline the invite to &d[" + clanKey + "]&b.")));
    }
}
