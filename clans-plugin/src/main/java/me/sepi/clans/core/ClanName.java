package me.sepi.clans.core;

/**
 * Clan tag validation.
 *
 * Rules:
 * <ul>
 *   <li>2 to 4 characters long.</li>
 *   <li>English letters are automatically uppercased (SEPI, SEpI, sepi all become SEPI).</li>
 *   <li>Digits and the '#' character are allowed.</li>
 *   <li>Any other symbol/space is rejected.</li>
 * </ul>
 */
public final class ClanName {

    private ClanName() {
    }

    /** @return normalized (uppercased) tag, or null when invalid. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() < 2 || trimmed.length() > 4) {
            return null;
        }
        StringBuilder out = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '#') {
                out.append('#');
            } else if (Character.isDigit(c)) {
                out.append(c);
            } else if (Character.isLetter(c)) {
                out.append(Character.toUpperCase(c));
            } else {
                return null;
            }
        }
        return out.toString();
    }

    public static boolean isValid(String raw) {
        return normalize(raw) != null;
    }
}
