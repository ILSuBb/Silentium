package org.IlSuBb.silentium.config;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckCategory;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private final Silentium plugin;
    private FileConfiguration mainConfig;
    private FileConfiguration punishmentsConfig;
    private FileConfiguration blatantConfig;
    private FileConfiguration ghostConfig;
    private FileConfiguration anarchyConfig;

    public ConfigManager(Silentium plugin) {
        this.plugin = plugin;
        loadAll();
    }

    public void loadAll() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();

        punishmentsConfig = loadOrSave("punishments.yml");
        blatantConfig     = loadOrSave("checks/blatant.yml");
        ghostConfig       = loadOrSave("checks/ghost.yml");
        anarchyConfig     = loadOrSave("checks/anarchy.yml");
    }

    private FileConfiguration loadOrSave(String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            plugin.saveResource(path, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    // ── Accessor methods ───────────────────────────────────────────────────

    public FileConfiguration getMainConfig() { return mainConfig; }
    public FileConfiguration getPunishmentsConfig() { return punishmentsConfig; }

    public FileConfiguration getCheckConfig(CheckCategory category) {
        return switch (category) {
            case BLATANT -> blatantConfig;
            case GHOST   -> ghostConfig;
            case ANARCHY -> anarchyConfig;
        };
    }

    // ── Shortcuts ──────────────────────────────────────────────────────────

    public boolean isDebug() {
        return mainConfig.getBoolean("debug", false);
    }

    public String getPrefix() {
        return mainConfig.getString("prefix", "<gray>[<aqua>Silentium<gray>]");
    }

    public String getNotificationFormat() {
        return mainConfig.getString("notifications.format",
                "<gray>[<aqua>Silentium<gray>] <red>{player} <gray>failed <red>{check} <gray>(VL: <red>{vl}<gray>)");
    }

    public boolean isBroadcastToStaff() {
        return mainConfig.getBoolean("notifications.broadcast-to-staff", true);
    }

    public boolean isLogFile() {
        return mainConfig.getBoolean("log.file", true);
    }

    public boolean isLogConsoleDebug() {
        return mainConfig.getBoolean("log.console-debug", false);
    }

    public boolean isLagCompensationEnabled() {
        return mainConfig.getBoolean("lag-compensation.enabled", true);
    }

    public int getMaxCompensatedPing() {
        return mainConfig.getInt("lag-compensation.max-compensated-ping", 300);
    }

    public boolean isVlDecayEnabled() {
        return mainConfig.getBoolean("vl-decay.enabled", true);
    }

    public int getVlDecayIntervalTicks() {
        return mainConfig.getInt("vl-decay.interval-ticks", 200);
    }

    public int getVlDecayAmount() {
        return mainConfig.getInt("vl-decay.amount", 1);
    }

    // ── Spec ──────────────────────────────────────────────────────────────

    public double getSpecFollowDistance() {
        return mainConfig.getDouble("spec.follow-distance", 15.0);
    }

    public boolean isSpecNightVision() {
        return mainConfig.getBoolean("spec.night-vision", true);
    }

    public boolean isSpecGlowEnabled() {
        return mainConfig.getBoolean("spec.glow.enabled", true);
    }

    public String getSpecGlowColor() {
        return mainConfig.getString("spec.glow.color", "YELLOW");
    }

    public boolean isCitizensCompatEnabled() {
        return mainConfig.getBoolean("compatibility.citizens", true);
    }

    public boolean isWorldGuardCompatEnabled() {
        return mainConfig.getBoolean("compatibility.worldguard", false);
    }

    public boolean isEssentialsCompatEnabled() {
        return mainConfig.getBoolean("compatibility.essentialsx", true);
    }
}
