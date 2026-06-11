package org.IlSuBb.silentium;

import org.IlSuBb.silentium.commands.SilentiumCommand;
import org.IlSuBb.silentium.commands.SilentiumTabCompleter;
import org.IlSuBb.silentium.config.ConfigManager;
import org.IlSuBb.silentium.config.MessagesConfig;
import org.IlSuBb.silentium.listeners.*;
import org.IlSuBb.silentium.managers.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Silentium extends JavaPlugin {

    private static Silentium instance;

    private ConfigManager       configManager;
    private MessagesConfig      messagesConfig;
    private PlayerDataManager   playerDataManager;
    private CheckManager        checkManager;
    private ViolationManager    violationManager;
    private PunishmentManager   punishmentManager;
    private NotificationManager notificationManager;
    private SpecManager         specManager;
    private GhostTargetManager  ghostTargetManager;

    private final Set<UUID> whitelist = new HashSet<>();
    private BukkitTask decayTask;

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        instance = this;

        // 1. Configs
        configManager    = new ConfigManager(this);
        configManager.loadAll();
        messagesConfig   = new MessagesConfig(this);
        messagesConfig.load();

        // 2. Core managers (order matters — CheckManager needs config)
        playerDataManager   = new PlayerDataManager();
        notificationManager = new NotificationManager(this);
        punishmentManager   = new PunishmentManager(this);
        violationManager    = new ViolationManager(this);
        checkManager        = new CheckManager(this);   // registers all checks
        specManager         = new SpecManager(this);

        // Ghost-target honeypot — spawn only when the check is enabled in config
        if (configManager.getCheckConfig(org.IlSuBb.silentium.checks.CheckCategory.GHOST)
                .getBoolean("ghosttarget.enabled", true)) {
            ghostTargetManager = new GhostTargetManager(this);
        }

        // 3. Whitelist
        loadWhitelist();

        // 4. Listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerConnectionListener(this), this);
        pm.registerEvents(new MovementListener(this),         this);
        pm.registerEvents(new CombatListener(this),           this);
        pm.registerEvents(new BlockListener(this),            this);

        // 5. Commands
        var cmd = getServer().getPluginCommand("silentium");
        if (cmd != null) {
            var executor = new SilentiumCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(new SilentiumTabCompleter(this));
        }

        // 6. VL decay scheduler
        scheduleDecay();

        getLogger().info("Silentium v" + getPluginMeta().getVersion() + " enabled — "
                + checkManager.getEnabledCount() + "/" + checkManager.getChecks().size()
                + " checks active.");
    }

    @Override
    public void onDisable() {
        if (decayTask != null) { decayTask.cancel(); decayTask = null; }
        if (specManager       != null) specManager.stopAll();
        if (ghostTargetManager != null) ghostTargetManager.stopAll();
        saveWhitelist();
        instance = null;
        getLogger().info("Silentium disabled.");
    }

    // ── Decay scheduler ────────────────────────────────────────────────────

    public void rescheduleDecay() { scheduleDecay(); }

    private void scheduleDecay() {
        if (decayTask != null) decayTask.cancel();
        if (!configManager.isVlDecayEnabled()) return;
        long interval = configManager.getVlDecayIntervalTicks();
        decayTask = getServer().getScheduler().runTaskTimerAsynchronously(
                this, violationManager::tickDecay, interval, interval);
    }

    // ── Whitelist persistence ──────────────────────────────────────────────

    private File whitelistFile() {
        return new File(getDataFolder(), "whitelist.yml");
    }

    private void loadWhitelist() {
        File file = whitelistFile();
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        var list = cfg.getStringList("whitelist");
        whitelist.clear();
        for (String s : list) {
            try { whitelist.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveWhitelist() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("whitelist", whitelist.stream().map(UUID::toString).toList());
        try { cfg.save(whitelistFile()); } catch (Exception e) {
            getLogger().warning("Could not save whitelist: " + e.getMessage());
        }
    }

    // ── Whitelist API ──────────────────────────────────────────────────────

    public boolean isWhitelisted(UUID uuid)    { return whitelist.contains(uuid); }
    public void addToWhitelist(UUID uuid)      { whitelist.add(uuid); }
    public void removeFromWhitelist(UUID uuid) { whitelist.remove(uuid); }

    // ── Getters ────────────────────────────────────────────────────────────

    public static Silentium getInstance()          { return instance; }
    public ConfigManager       getConfigManager()       { return configManager; }
    public MessagesConfig      getMessagesConfig()      { return messagesConfig; }
    public PlayerDataManager   getPlayerDataManager()   { return playerDataManager; }
    public CheckManager        getCheckManager()        { return checkManager; }
    public ViolationManager    getViolationManager()    { return violationManager; }
    public PunishmentManager   getPunishmentManager()   { return punishmentManager; }
    public NotificationManager getNotificationManager() { return notificationManager; }
    public SpecManager         getSpecManager()         { return specManager; }
    public GhostTargetManager  getGhostTargetManager()  { return ghostTargetManager; }
}
