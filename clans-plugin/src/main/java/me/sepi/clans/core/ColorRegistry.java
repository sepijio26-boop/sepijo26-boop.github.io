package me.sepi.clans.core;

import me.sepi.clans.model.ColorOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registry of purchasable clan tag colours.
 */
public final class ColorRegistry {

    private final Map<String, ColorOption> byId = new LinkedHashMap<String, ColorOption>();

    public void clear() {
        byId.clear();
    }

    public void register(String id, String legacyCode, String chatColor, double cost) {
        String key = id.toLowerCase(Locale.ROOT);
        byId.put(key, new ColorOption(key, legacyCode, chatColor, cost));
    }

    public ColorOption get(String id) {
        if (id == null) {
            return null;
        }
        return byId.get(id.toLowerCase(Locale.ROOT));
    }

    public List<ColorOption> all() {
        List<ColorOption> list = new ArrayList<ColorOption>(byId.values());
        Collections.sort(list, new Comparator<ColorOption>() {
            public int compare(ColorOption a, ColorOption b) {
                return a.getId().compareTo(b.getId());
            }
        });
        return list;
    }

    public int size() {
        return byId.size();
    }
}
