package me.sepi.clans.bukkit;

import me.sepi.clans.ClansPlugin;

import me.sepi.clans.core.ClanManager;
import me.sepi.clans.core.Result;
import me.sepi.clans.model.Clan;
import me.sepi.clans.model.ColorOption;
import me.sepi.clans.model.PlayerData;
import me.sepi.clans.model.Rank;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * /clan command implementation, tab completion and error reporting.
 */
public final class CommandHandler implements CommandExecutor, TabCompleter {

    private static final String[] SUBCOMMANDS = {
            "create", "join", "accept", "decline", "leave", "disband", "kick", "invite",
            "pvp", "tax", "public", "private", "promote", "demote", "color", "deposit",
            "withdraw", "vault", "info", "list", "colors", "help", "reload"
    };

    private final ClansPlugin plugin;
    private final ClanManager manager;
    private final Messages messages;

    public CommandHandler(ClansPlugin plugin, ClanManager manager, Messages messages) {
        this.plugin = plugin;
        this.manager = manager;
        this.messages = messages;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("clans.use")) {
            sender.sendMessage(messages.msg("error.no-permission"));
            return true;
        }
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("help")) {
            sendHelp(sender);
            return true;
        }
        if (sub.equals("reload")) {
            if (!sender.hasPermission("clans.admin")) {
                sender.sendMessage(messages.msg("error.no-permission"));
                return true;
            }
            plugin.reloadAll();
            sender.sendMessage(messages.msg("command.reloaded"));
            return true;
        }
        Player player = asPlayer(sender);
        if (player == null) {
            sender.sendMessage(messages.msg("error.must-be-player"));
            return true;
        }
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        if (sub.equals("create")) {
            if (args.length < 3) {
                return usage(sender, "create");
            }
            Result r = manager.create(uuid, name, args[2]);
            if (!r.ok()) {
                return error(sender, r);
            }
            ColorOption color = manager.getColors().get(args[2]);
            sender.sendMessage(messages.msg("clan.created",
                    "clan", tag(args[1]),
                    "color", color == null ? String.valueOf(args[2]) : color.getId(),
                    "cost", manager.getBank().format(color == null ? 0.0D : color.getCost())));
            return true;
        }
        if (sub.equals("join")) {
            if (args.length < 2) {
                return usage(sender, "join");
            }
            Result r = manager.join(uuid, name, args[1]);
            return finish(sender, r, "clan.joined", "clan", tag(args[1]));
        }
        if (sub.equals("accept") || sub.equals("decline")) {
            String key = args.length >= 2 ? args[1] : null;
            Result r = sub.equals("accept") ? manager.accept(uuid, key) : manager.decline(uuid, key);
            if (!r.ok()) {
                return error(sender, r);
            }
            return finish(sender, Result.OK, sub.equals("accept") ? "clan.invite-accepted" : "clan.invite-declined",
                    "clan", key == null ? "" : tag(key));
        }
        if (sub.equals("leave")) {
            Result r = manager.leave(uuid);
            return finish(sender, r, "clan.left");
        }
        if (sub.equals("disband")) {
            Result r = manager.disband(uuid);
            return finish(sender, r, "clan.disbanded");
        }
        if (sub.equals("kick")) {
            if (args.length < 2) {
                return usage(sender, "kick");
            }
            Result r = manager.kick(uuid, args[1]);
            return finish(sender, r, "clan.kicked", "target", args[1]);
        }
        if (sub.equals("invite")) {
            if (args.length < 2) {
                return usage(sender, "invite");
            }
            Result r = manager.invite(uuid, args[1]);
            if (!r.ok()) {
                return error(sender, r);
            }
            Clan clan = manager.clanOf(uuid);
            String key = clan == null ? "" : clan.getKey();
            sender.sendMessage(messages.msg("clan.invite-sent", "target", args[1], "clan", tag(key)));
            UUID target = manager.playerDataLookup(args[1]);
            Player targetOnline = target == null ? null : Bukkit.getPlayer(target);
            if (targetOnline != null && targetOnline.isOnline()) {
                targetOnline.sendMessage(messages.msg("clan.invite-received",
                                "inviter", name, "clan", tag(key))
                        .append(Component.text(" "))
                        .append(messages.acceptButton(key))
                        .append(Component.text(" "))
                        .append(messages.declineButton(key)));
            }
            return true;
        }
        if (sub.equals("pvp")) {
            if (args.length < 2) {
                return usage(sender, "pvp");
            }
            boolean on = args[1].equalsIgnoreCase("on");
            if (!on && !args[1].equalsIgnoreCase("off")) {
                return usage(sender, "pvp");
            }
            Result r = manager.setPvp(uuid, on);
            return finish(sender, r, on ? "clan.pvp-on" : "clan.pvp-off");
        }
        if (sub.equals("tax")) {
            Result r;
            boolean on = false;
            if (args.length >= 2 && args[1].equalsIgnoreCase("off")) {
                r = manager.setTax(uuid, 0.0D);
            } else if (args.length >= 2) {
                try {
                    double amount = Double.parseDouble(args[1]);
                    if (amount > 0.0D) {
                        on = true;
                        r = manager.setTax(uuid, amount);
                    } else {
                        r = Result.error("error.amount-positive");
                    }
                } catch (NumberFormatException e) {
                    return usage(sender, "tax");
                }
            } else {
                return usage(sender, "tax");
            }
            return finish(sender, r, on ? "clan.tax-on" : "clan.tax-off", "amount", args.length >= 2 ? args[1] : "0");
        }
        if (sub.equals("public") || sub.equals("private")) {
            boolean on = sub.equals("public");
            Result r = manager.setPublic(uuid, on);
            return finish(sender, r, on ? "clan.public-on" : "clan.public-off");
        }
        if (sub.equals("promote")) {
            if (args.length < 3) {
                return usage(sender, "promote");
            }
            Result r = manager.promote(uuid, args[1], args[2]);
            return finish(sender, r, "clan.promoted", "target", args[1], "rank", args[2].toUpperCase(Locale.ROOT));
        }
        if (sub.equals("demote")) {
            if (args.length < 2) {
                return usage(sender, "demote");
            }
            Result r = manager.demote(uuid, args[1]);
            return finish(sender, r, "clan.demoted", "target", args[1]);
        }
        if (sub.equals("color")) {
            if (args.length < 2) {
                return usage(sender, "color");
            }
            Result r = manager.setColor(uuid, args[1]);
            return finish(sender, r, "clan.color-changed", "color", args[1]);
        }
        if (sub.equals("deposit")) {
            if (args.length < 2) {
                return usage(sender, "deposit");
            }
            Double amount = parseAmount(args[1]);
            if (amount == null) {
                return usage(sender, "deposit");
            }
            Result r = manager.deposit(uuid, amount.doubleValue());
            return finish(sender, r, "clan.vault-deposit", "amount", format(amount.doubleValue()));
        }
        if (sub.equals("withdraw")) {
            if (args.length < 2) {
                return usage(sender, "withdraw");
            }
            Double amount = parseAmount(args[1]);
            if (amount == null) {
                return usage(sender, "withdraw");
            }
            Result r = manager.withdraw(uuid, amount.doubleValue());
            return finish(sender, r, "clan.vault-withdraw", "amount", format(amount.doubleValue()));
        }
        if (sub.equals("vault")) {
            Clan clan = manager.clanOf(uuid);
            if (clan == null) {
                return error(sender, Result.error("error.no-clan"));
            }
            sender.sendMessage(messages.msg("vault.info",
                    "clan", tag(clan.getKey()),
                    "vault", format(clan.getVault()),
                    "tax", clan.getTax() > 0.0D ? format(clan.getTax()) : messages.raw("vault.tax-off"),
                    "interval", String.valueOf(manager.getSettings().getTaxIntervalMinutes())));
            return true;
        }
        if (sub.equals("info")) {
            Clan clan = args.length >= 2 ? manager.clan(args[1]) : manager.clanOf(uuid);
            if (clan == null) {
                return error(sender, Result.error("error.clan-not-found", args.length >= 2 ? args[1] : "?"));
            }
            showInfo(sender, clan);
            return true;
        }
        if (sub.equals("list")) {
            List<Clan> clans = manager.allClans();
            sender.sendMessage(messages.msg("clan.list-header", "count", String.valueOf(clans.size())));
            int i = 1;
            for (Clan clan : clans) {
                sender.sendMessage(messages.msg("clan.list-entry",
                        "number", String.valueOf(i),
                        "clan", tag(clan.getKey()),
                        "members", String.valueOf(clan.memberIds().size()),
                        "status", clan.isPublic() ? "public" : "private"));
                i++;
            }
            return true;
        }
        if (sub.equals("colors")) {
            List<ColorOption> colors = manager.getColors().all();
            sender.sendMessage(messages.msg("clan.colors-header"));
            for (ColorOption color : colors) {
                sender.sendMessage(messages.msg("clan.colors-entry",
                        "color", color.getLegacyCode() + color.getId(),
                        "cost", color.isFree() ? messages.raw("clan.colors-free") : format(color.getCost())));
            }
            return true;
        }
        return usage(sender, "help");
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(sub);
                }
            }
            return out;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("join") || sub.equals("info")) {
                for (Clan clan : manager.allClans()) {
                    if (clan.getKey().startsWith(args[1].toUpperCase(Locale.ROOT))) {
                        out.add(clan.getKey());
                    }
                }
                return out;
            }
            if (sub.equals("kick") || sub.equals("invite") || sub.equals("promote") || sub.equals("demote")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                        out.add(p.getName());
                    }
                }
                return out;
            }
            if (sub.equals("pvp")) {
                if ("on".startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add("on");
                }
                if ("off".startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add("off");
                }
                return out;
            }
            if (sub.equals("tax") || sub.equals("promote")) {
                if (sub.equals("tax") && "off".startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add("off");
                }
                if (sub.equals("promote")) {
                    if ("admin".startsWith(args[1].toLowerCase(Locale.ROOT))) {
                        out.add("admin");
                    }
                    if ("og".startsWith(args[1].toLowerCase(Locale.ROOT))) {
                        out.add("og");
                    }
                }
                return out;
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("create") || sub.equals("color")) {
                for (ColorOption color : manager.getColors().all()) {
                    if (color.getId().startsWith(args[2].toLowerCase(Locale.ROOT))) {
                        out.add(color.getId());
                    }
                }
                return out;
            }
        }
        return out;
    }

    private void showInfo(CommandSender sender, Clan clan) {
        sender.sendMessage(messages.msg("info.header",
                "clan", tag(clan.getKey()),
                "color", clan.getColorId(),
                "owner", formatName(clan.getOwner()),
                "pvp", clan.isPvp() ? messages.raw("info.on") : messages.raw("info.off"),
                "tax", clan.getTax() > 0.0D ? format(clan.getTax()) : messages.raw("info.off"),
                "status", clan.isPublic() ? messages.raw("info.public") : messages.raw("info.private"),
                "vault", format(clan.getVault()),
                "members", String.valueOf(clan.memberIds().size())));
        for (UUID member : clan.memberIds()) {
            Rank rank = clan.rankOf(member);
            sender.sendMessage(messages.msg("info.member",
                    "name", formatName(member),
                    "rank", rank == null ? "" : rank.getDisplay()));
        }
    }

    private String formatName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        PlayerData data = manager.playerData(uuid);
        return data.getLastName() == null ? uuid.toString().substring(0, 8) : data.getLastName();
    }

    private String tag(String key) {
        Clan clan = manager.clan(key);
        return clan == null ? "&7[" + key + "]" : tagOf(clan);
    }

    private String tagOf(Clan clan) {
        ColorOption color = manager.getColors().get(clan.getColorId());
        String code = color == null ? "&7" : color.getLegacyCode();
        return code + "[" + clan.getKey() + "]&r";
    }

    private boolean finish(CommandSender sender, Result r, String okKey, String... pairs) {
        if (!r.ok()) {
            return error(sender, r);
        }
        sender.sendMessage(messages.msg(okKey, pairs));
        return true;
    }

    private boolean error(CommandSender sender, Result r) {
        sender.sendMessage(messages.msg(r.getErrorKey(), r.getArgs()));
        return true;
    }

    private boolean usage(CommandSender sender, String command) {
        sender.sendMessage(messages.msg("usage." + command));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        for (Component line : plugin.getMessages().lines("help.lines")) {
            sender.sendMessage(line);
        }
    }

    private Player asPlayer(CommandSender sender) {
        return sender instanceof Player ? (Player) sender : null;
    }

    private Double parseAmount(String raw) {
        try {
            double v = Double.parseDouble(raw);
            return v > 0.0D ? Double.valueOf(v) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String format(double v) {
        return manager.getBank().format(v);
    }
}
