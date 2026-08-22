package me.sepi.clans.bukkit;

import me.sepi.clans.ClansPlugin;

import me.sepi.clans.core.ClanManager;
import me.sepi.clans.core.TagUpdater;
import me.sepi.clans.model.Clan;
import me.sepi.clans.model.ColorOption;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.UUID;

/**
 * Applies clan tags to the tab list and colours the player name tag through a
 * per-player scoreboard team. The team carries only a colour, so the name tag
 * shows the clan colour without any extra text.
 */
public final class TagManager implements TagUpdater {

    private final ClansPlugin plugin;
    private final ClanManager manager;
    private final DisplaySettings display;

    public TagManager(ClansPlugin plugin, ClanManager manager, DisplaySettings display) {
        this.plugin = plugin;
        this.manager = manager;
        this.display = display;
    }

    /** Coloured tag string like "&c[SEPI]" (empty when the player is clanless). */
    public String tagOf(Clan clan) {
        if (clan == null) {
            return "";
        }
        ColorOption color = manager.getColors().get(clan.getColorId());
        String code = color == null ? "&7" : color.getLegacyCode();
        return code + "[" + clan.getKey() + "]&r";
    }

    public void updatePlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        Clan clan = manager.clanOf(uuid);
        if (player == null || !player.isOnline()) {
            // Keep scoreboard teams clean even when the player is offline.
            if (clan == null) {
                unregisterTeam(uuid);
            }
            return;
        }
        applyTab(player, clan);
        applyTeam(player, clan);
    }

    private void unregisterTeam(UUID uuid) {
        ScoreboardManager sbManager = Bukkit.getScoreboardManager();
        if (sbManager == null) {
            return;
        }
        Team team = sbManager.getMainScoreboard().getTeam(teamName(uuid));
        if (team != null) {
            team.unregister();
        }
    }

    public void applyAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player.getUniqueId());
        }
    }

    private void applyTab(Player player, Clan clan) {
        if (!display.isTabEnabled()) {
            return;
        }
        String tag = tagOf(clan);
        String text = display.getTabFormat()
                .replace("{name}", player.getName())
                .replace("{tag}", tag);
        player.playerListName(Messages.fromLegacy(text));
    }

    private void applyTeam(Player player, Clan clan) {
        if (!display.isNametagEnabled()) {
            return;
        }
        ScoreboardManager sbManager = Bukkit.getScoreboardManager();
        if (sbManager == null) {
            return;
        }
        Scoreboard scoreboard = sbManager.getMainScoreboard();
        String teamName = teamName(player.getUniqueId());
        Team team = scoreboard.getTeam(teamName);
        if (clan == null) {
            if (team != null) {
                team.unregister();
            }
            return;
        }
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        ColorOption color = manager.getColors().get(clan.getColorId());
        if (color != null) {
            try {
                team.setColor(ChatColor.valueOf(color.getChatColor()));
            } catch (IllegalArgumentException ignored) {
                team.setColor(ChatColor.WHITE);
            }
        } else {
            team.setColor(ChatColor.WHITE);
        }
        team.addEntry(player.getName());
    }

    static String teamName(UUID uuid) {
        String hex = Long.toHexString(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());
        if (hex.length() > 8) {
            hex = hex.substring(0, 8);
        }
        return "c" + hex;
    }
}
