package org.IlSuBb.silentium.commands;

import org.IlSuBb.silentium.Silentium;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SilentiumTabCompleter implements TabCompleter {

    private static final List<String> MOD_SUBS = List.of(
            "help", "info", "alerts", "verbose", "reset", "kick", "spec");
    private static final List<String> ADMIN_SUBS = List.of(
            "whitelist", "reload", "status", "logs", "version");

    public SilentiumTabCompleter(Silentium plugin) {
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (hasMod(sender)) completions.addAll(MOD_SUBS);
            if (hasAdmin(sender)) completions.addAll(ADMIN_SUBS);
            return filter(completions, args[0]);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "info", "verbose", "reset" -> filterPlayers(args[1]);
                case "kick" -> {
                    String self = sender instanceof Player p ? p.getName() : null;
                    yield filterPlayers(args[1], self);
                }
                case "spec" -> {
                    // complete both player names (excluding self) and the keyword "stop"
                    String selfName = sender instanceof Player p ? p.getName() : null;
                    List<String> opts = new ArrayList<>(filterPlayers(args[1], selfName));
                    opts.addAll(filter(List.of("stop"), args[1]));
                    yield opts;
                }
                case "alerts"    -> filter(List.of("on", "off"), args[1]);
                case "whitelist" -> filter(List.of("add", "remove"), args[1]);
                default -> List.of();
            };
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("whitelist")) {
            return filterPlayers(args[2]);
        }

        return List.of();
    }

    private List<String> filterPlayers(String prefix) {
        return filterPlayers(prefix, null);
    }

    private List<String> filterPlayers(String prefix, String exclude) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> !n.equals(exclude))
                .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private static List<String> filter(List<String> options, String prefix) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private boolean hasMod(CommandSender s) {
        return s.hasPermission("silentium.mod") || s.hasPermission("silentium.admin");
    }

    private boolean hasAdmin(CommandSender s) {
        return s.hasPermission("silentium.admin");
    }
}
