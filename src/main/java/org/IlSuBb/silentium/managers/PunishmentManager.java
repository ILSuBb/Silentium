package org.IlSuBb.silentium.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.IlSuBb.silentium.Silentium;
import io.papermc.paper.ban.BanListType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.TreeMap;

public class PunishmentManager {

    private final Silentium plugin;

    public PunishmentManager(Silentium plugin) {
        this.plugin = plugin;
    }

    public void process(Player player, String checkName, int currentVl) {
        ConfigurationSection root = plugin.getConfigManager()
                .getPunishmentsConfig().getConfigurationSection(checkName);
        if (root == null) return;

        TreeMap<Integer, ConfigurationSection> thresholds = new TreeMap<>();
        for (String key : root.getKeys(false)) {
            try {
                int threshold = Integer.parseInt(key);
                ConfigurationSection sec = root.getConfigurationSection(key);
                if (sec != null) thresholds.put(threshold, sec);
            } catch (NumberFormatException ignored) {}
        }

        Integer floor = thresholds.floorKey(currentVl);
        if (floor == null) return;
        ConfigurationSection sec = thresholds.get(floor);

        String action   = sec.getString("action", "ALERT").toUpperCase();
        String message  = sec.getString("message", "Cheating");
        String duration = sec.getString("duration", "60m");

        Bukkit.getScheduler().runTask(plugin, () ->
                executePunishment(player, checkName, action, message, duration));
    }

    private void executePunishment(Player player, String checkName,
                                   String action, String message, String duration) {
        if (!player.isOnline()) return;

        switch (action) {
            case "ALERT" -> {} // handled by NotificationManager

            case "KICK" -> player.kick(Component.text(message, NamedTextColor.RED));

            case "TEMPBAN" -> {
                if (plugin.getConfigManager().isEssentialsCompatEnabled()
                        && isPluginEnabled("Essentials")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "tempban " + player.getName() + " " + duration + " " + message);
                } else {
                    player.kick(Component.text("[Temp Ban] " + message, NamedTextColor.RED));
                }
            }

            case "BAN" -> {
                if (plugin.getConfigManager().isEssentialsCompatEnabled()
                        && isPluginEnabled("Essentials")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "ban " + player.getName() + " " + message);
                } else {
                    Bukkit.getBanList(io.papermc.paper.ban.BanListType.PROFILE)
                            .addBan(player.getPlayerProfile(), message,
                                    (java.time.Instant) null, "Silentium");
                    player.kick(Component.text("Banned: " + message, NamedTextColor.RED));
                }
            }

            default -> plugin.getLogger().warning(
                    "Unknown punishment action '" + action + "' for check " + checkName);
        }
    }

    private boolean isPluginEnabled(String name) {
        return Bukkit.getPluginManager().isPluginEnabled(name);
    }
}
