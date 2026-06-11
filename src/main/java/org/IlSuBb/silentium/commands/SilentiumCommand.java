package org.IlSuBb.silentium.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.data.PlayerData;
import org.IlSuBb.silentium.data.ViolationEntry;
import org.IlSuBb.silentium.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class SilentiumCommand implements CommandExecutor {

    private final Silentium plugin;

    public SilentiumCommand(Silentium plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "help"      -> sendHelp(sender);
            case "version"   -> handleVersion(sender);
            case "status"    -> handleStatus(sender);
            case "reload"    -> handleReload(sender);
            case "logs"      -> handleLogs(sender, args);
            case "alerts"    -> handleAlerts(sender, args);
            case "verbose"   -> handleVerbose(sender, args);
            case "info"      -> handleInfo(sender, args);
            case "reset"     -> handleReset(sender, args);
            case "kick"      -> handleKick(sender, args);
            case "spec"      -> handleSpec(sender, args);
            case "whitelist" -> handleWhitelist(sender, args);
            default          -> sendHelp(sender);
        }
        return true;
    }

    // ── /sac help ──────────────────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.parse(plugin.getMessagesConfig().getRaw("help-header")));
        if (hasMod(sender)) {
            helpLine(sender, "sac info <игрок>",                 "Нарушения игрока");
            helpLine(sender, "sac alerts [on|off]",              "Вкл/выкл оповещения");
            helpLine(sender, "sac verbose <игрок>",              "Подробный режим");
            helpLine(sender, "sac reset <игрок>",                "Сбросить нарушения");
            helpLine(sender, "sac kick <игрок> [причина]",       "Кикнуть игрока");
            helpLine(sender, "sac spec <игрок>",                 "Наблюдать за игроком");
            helpLine(sender, "sac spec stop",                    "Остановить наблюдение");
        }
        if (hasAdmin(sender)) {
            helpLine(sender, "sac whitelist add|remove <игрок>", "Белый список");
            helpLine(sender, "sac reload",                       "Перезагрузить конфигурацию");
            helpLine(sender, "sac status",                       "Статус плагина");
            helpLine(sender, "sac logs [N]",                     "Последние логи");
            helpLine(sender, "sac version",                      "Версия плагина");
        }
    }

    private void helpLine(CommandSender sender, String cmd, String desc) {
        String raw = plugin.getMessagesConfig().getRaw("help-line")
                .replace("{cmd}", cmd).replace("{desc}", desc).replace("{args}", "");
        sender.sendMessage(ColorUtils.parse(raw));
    }

    // ── /sac version ───────────────────────────────────────────────────────

    private void handleVersion(CommandSender sender) {
        if (!hasAdmin(sender)) { noPermission(sender); return; }
        sender.sendMessage(plugin.getMessagesConfig().get("version-info",
                "version", plugin.getPluginMeta().getVersion()));
    }

    // ── /sac status ────────────────────────────────────────────────────────

    private void handleStatus(CommandSender sender) {
        if (!hasAdmin(sender)) { noPermission(sender); return; }
        sender.sendMessage(ColorUtils.parse(plugin.getMessagesConfig().getRaw("status-header")));
        long enabled = plugin.getCheckManager().getEnabledCount();
        long total   = plugin.getCheckManager().getChecks().size();
        int players  = plugin.getPlayerDataManager().getTrackedCount();
        sender.sendMessage(plugin.getMessagesConfig().get("status-checks",
                "enabled", String.valueOf(enabled), "total", String.valueOf(total)));
        sender.sendMessage(plugin.getMessagesConfig().get("status-players",
                "players", String.valueOf(players)));
    }

    // ── /sac reload ────────────────────────────────────────────────────────

    private void handleReload(CommandSender sender) {
        if (!hasAdmin(sender)) { noPermission(sender); return; }
        long start = System.currentTimeMillis();
        // Full self-reload (disablePlugin+enablePlugin from within the plugin) is
        // impossible — it closes the plugin classloader's JAR file, crashing any code
        // still running on that classloader.  Instead we reload all configuration files
        // and reschedule the VL-decay task so interval changes take effect immediately.
        plugin.getConfigManager().loadAll();
        plugin.getMessagesConfig().load();
        plugin.rescheduleDecay();
        long ms = System.currentTimeMillis() - start;
        sender.sendMessage(plugin.getMessagesConfig().get("reload-success", "time", ms + "ms"));
    }

    // ── /sac logs [N] ──────────────────────────────────────────────────────

    private void handleLogs(CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) { noPermission(sender); return; }
        int count = 20;
        if (args.length >= 2) {
            try { count = Integer.parseInt(args[args.length - 1]); } catch (NumberFormatException ignored) {}
        }
        List<String> logs = plugin.getNotificationManager().getRecentLogs(count);
        if (logs.isEmpty()) {
            sender.sendMessage(Component.text("Нет записей в лог-файле.", NamedTextColor.YELLOW));
            return;
        }
        logs.forEach(line -> sender.sendMessage(Component.text(line, NamedTextColor.GRAY)));
    }

    // ── /sac alerts [on|off] ───────────────────────────────────────────────

    private void handleAlerts(CommandSender sender, String[] args) {
        if (!hasMod(sender)) { noPermission(sender); return; }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessagesConfig().get("player-only")); return;
        }
        PlayerData data = plugin.getPlayerDataManager().getOrCreate(player);
        boolean enable = args.length < 2 ? !data.isAlertsEnabled() : args[1].equalsIgnoreCase("on");
        data.setAlertsEnabled(enable);
        sender.sendMessage(plugin.getMessagesConfig().get(enable ? "alerts-enabled" : "alerts-disabled"));
    }

    // ── /sac verbose <player> ──────────────────────────────────────────────

    private void handleVerbose(CommandSender sender, String[] args) {
        if (!hasMod(sender)) { noPermission(sender); return; }
        if (args.length < 2) { sender.sendMessage(Component.text("Укажите игрока.", NamedTextColor.RED)); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { playerNotFound(sender, args[1]); return; }
        PlayerData data = plugin.getPlayerDataManager().getOrCreate(target);
        boolean verbose = !data.isVerboseTarget();
        data.setVerboseTarget(verbose);
        sender.sendMessage(plugin.getMessagesConfig().get(
                verbose ? "verbose-enabled" : "verbose-disabled", "player", target.getName()));
    }

    // ── /sac info <player> ─────────────────────────────────────────────────

    private void handleInfo(CommandSender sender, String[] args) {
        if (!hasMod(sender)) { noPermission(sender); return; }
        if (args.length < 2) { sender.sendMessage(Component.text("Укажите игрока.", NamedTextColor.RED)); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { playerNotFound(sender, args[1]); return; }
        PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
        if (data == null || data.getViolationLevels().isEmpty()) {
            sender.sendMessage(plugin.getMessagesConfig().get("info-no-violations", "player", target.getName()));
            return;
        }
        sender.sendMessage(plugin.getMessagesConfig().get("info-header", "player", target.getName()));
        for (CheckBase check : plugin.getCheckManager().getChecks()) {
            int vl = data.getViolationLevel(check.getName());
            if (vl > 0) {
                sender.sendMessage(plugin.getMessagesConfig().get("info-check-line",
                        "check", check.getName(),
                        "category", check.getCategory().getDisplayName(),
                        "vl", String.valueOf(vl)));
            }
        }
        List<ViolationEntry> history = data.getViolationHistory();
        if (!history.isEmpty()) {
            sender.sendMessage(ColorUtils.parse("<dark_gray>── Последние нарушения:"));
            history.stream().limit(5)
                    .map(e -> ColorUtils.parse("<dark_gray>  " + e.toString()))
                    .forEach(sender::sendMessage);
        }
        sender.sendMessage(ColorUtils.parse(plugin.getMessagesConfig().getRaw("info-footer")));
    }

    // ── /sac reset <player> ────────────────────────────────────────────────

    private void handleReset(CommandSender sender, String[] args) {
        if (!hasMod(sender)) { noPermission(sender); return; }
        if (args.length < 2) { sender.sendMessage(Component.text("Укажите игрока.", NamedTextColor.RED)); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { playerNotFound(sender, args[1]); return; }
        plugin.getViolationManager().resetViolations(target);
        sender.sendMessage(plugin.getMessagesConfig().get("reset-success", "player", target.getName()));
    }

    // ── /sac kick <player> [reason] ────────────────────────────────────────

    private void handleKick(CommandSender sender, String[] args) {
        if (!hasMod(sender)) { noPermission(sender); return; }
        if (args.length < 2) { sender.sendMessage(Component.text("Укажите игрока.", NamedTextColor.RED)); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { playerNotFound(sender, args[1]); return; }
        String reason = args.length > 2 ? joinFrom(args, 2) : "Подозрение в использовании читов";
        target.kick(plugin.getMessagesConfig().get("kick-message", "reason", reason));
        sender.sendMessage(plugin.getMessagesConfig().get("kick-success",
                "player", target.getName(), "reason", reason));
    }

    // ── /sac spec <player> | /sac spec stop ────────────────────────────────

    private void handleSpec(CommandSender sender, String[] args) {
        if (!hasMod(sender)) { noPermission(sender); return; }
        if (!(sender instanceof Player mod)) {
            sender.sendMessage(plugin.getMessagesConfig().get("player-only")); return;
        }

        var specManager = plugin.getSpecManager();

        // /sac spec  OR  /sac spec stop/off — остановить наблюдение
        if (args.length < 2 || args[1].equalsIgnoreCase("stop") || args[1].equalsIgnoreCase("off")) {
            if (specManager.isSpecting(mod)) {
                specManager.stopSpec(mod);
            } else {
                sender.sendMessage(Component.text(
                        "Использование: /sac spec <игрок>  |  /sac spec stop", NamedTextColor.RED));
            }
            return;
        }

        // /sac spec <player> — начать наблюдение
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null)       { playerNotFound(sender, args[1]); return; }
        if (target.equals(mod))   {
            sender.sendMessage(Component.text("Нельзя наблюдать за собой.", NamedTextColor.RED));
            return;
        }

        specManager.startSpec(mod, target);
    }

    // ── /sac whitelist add|remove <player> ────────────────────────────────

    private void handleWhitelist(CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) { noPermission(sender); return; }
        if (args.length < 3) {
            sender.sendMessage(Component.text(
                    "Использование: /sac whitelist add|remove <игрок>", NamedTextColor.RED));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        java.util.UUID uuid = target.getUniqueId();
        switch (args[1].toLowerCase()) {
            case "add" -> {
                if (plugin.isWhitelisted(uuid)) {
                    sender.sendMessage(plugin.getMessagesConfig().get("whitelist-already", "player", args[2]));
                } else {
                    plugin.addToWhitelist(uuid); plugin.saveWhitelist();
                    sender.sendMessage(plugin.getMessagesConfig().get("whitelist-added", "player", args[2]));
                }
            }
            case "remove", "rm", "del" -> {
                if (!plugin.isWhitelisted(uuid)) {
                    sender.sendMessage(plugin.getMessagesConfig().get("whitelist-not-in", "player", args[2]));
                } else {
                    plugin.removeFromWhitelist(uuid); plugin.saveWhitelist();
                    sender.sendMessage(plugin.getMessagesConfig().get("whitelist-removed", "player", args[2]));
                }
            }
            default -> sender.sendMessage(Component.text("Команды: add, remove", NamedTextColor.RED));
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private boolean hasMod(CommandSender s) {
        return s.hasPermission("silentium.mod") || s.hasPermission("silentium.admin");
    }

    private boolean hasAdmin(CommandSender s) {
        return s.hasPermission("silentium.admin");
    }

    private void noPermission(CommandSender s) {
        s.sendMessage(plugin.getMessagesConfig().get("no-permission"));
    }

    private void playerNotFound(CommandSender s, String name) {
        s.sendMessage(plugin.getMessagesConfig().get("player-not-found", "player", name));
    }

    private static String joinFrom(String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length));
    }
}
