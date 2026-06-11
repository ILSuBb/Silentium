package org.IlSuBb.silentium.managers;

import net.kyori.adventure.text.format.NamedTextColor;
import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.utils.GlowUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class SpecManager {

    private static final long CHECK_TICKS = 20L;
    private static final long GLOW_TICKS  = 3L;

    // Team name added to the mod's own scoreboard — short to fit within 16 chars
    private static final String TEAM_NAME = "sac_spec";

    private final Silentium plugin;

    private final Map<UUID, SpecSession> sessions    = new HashMap<>();
    private final Map<UUID, Set<UUID>>   targetToMods = new HashMap<>();

    public SpecManager(Silentium plugin) {
        this.plugin = plugin;
    }

    // ── Public API ─────────────────────────────────────────────────────────

    public void startSpec(Player mod, Player target) {
        if (isSpecting(mod)) stopSession(mod.getUniqueId(), true);

        Location savedLoc  = mod.getLocation().clone();
        GameMode savedMode = mod.getGameMode();
        UUID modUuid       = mod.getUniqueId();
        UUID targetUuid    = target.getUniqueId();
        String targetName  = target.getName();

        mod.setGameMode(GameMode.SPECTATOR);
        mod.teleport(target.getLocation());

        if (plugin.getConfigManager().isSpecNightVision()) {
            mod.addPotionEffect(new PotionEffect(
                    PotionEffectType.NIGHT_VISION,
                    Integer.MAX_VALUE, 0, true, false, false));
        }

        boolean glowEnabled = plugin.getConfigManager().isSpecGlowEnabled();
        if (glowEnabled) {
            // Register team on the mod's CURRENT scoreboard (not necessarily main scoreboard —
            // plugins like TAB give players their own custom scoreboard).
            applyGlowTeam(mod, targetName);
            // Delay one tick so the team packet reaches the client before the glow flag.
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> GlowUtil.sendGlow(mod, target, true), 2L);
        }

        mod.sendActionBar(plugin.getMessagesConfig().get("spec-started", "player", targetName));
        targetToMods.computeIfAbsent(targetUuid, k -> new HashSet<>()).add(modUuid);

        // ── Distance / online check (every 1 s)
        BukkitTask checkTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Player m = plugin.getServer().getPlayer(modUuid);
            Player t = plugin.getServer().getPlayer(targetUuid);
            if (m == null || !m.isOnline()) { stopSession(modUuid, false); return; }
            if (t == null || !t.isOnline()) {
                stopSession(modUuid, true);
                m.sendMessage(plugin.getMessagesConfig().get("spec-target-offline", "player", targetName));
                return;
            }
            double followDist = plugin.getConfigManager().getSpecFollowDistance();
            boolean tooFar = !m.getWorld().equals(t.getWorld())
                    || m.getLocation().distance(t.getLocation()) > followDist;
            if (tooFar) {
                m.teleport(t.getLocation());
                m.sendActionBar(plugin.getMessagesConfig().get("spec-teleporting", "player", t.getName()));
            }
        }, CHECK_TICKS, CHECK_TICKS);

        // ── Glow refresh (every 3 ticks) — re-ensures team and re-sends packet in
        //    case TAB / scoreboard plugins replaced the mod's scoreboard or team.
        BukkitTask glowTask = glowEnabled
                ? plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                    Player m = plugin.getServer().getPlayer(modUuid);
                    Player t = plugin.getServer().getPlayer(targetUuid);
                    if (m != null && m.isOnline() && t != null && t.isOnline()) {
                        applyGlowTeam(m, t.getName());
                        GlowUtil.sendGlow(m, t, true);
                    }
                }, GLOW_TICKS, GLOW_TICKS)
                : null;

        sessions.put(modUuid, new SpecSession(
                targetUuid, targetName,
                savedMode, savedLoc,
                checkTask, glowTask));
    }

    public void stopSpec(Player mod) {
        if (!isSpecting(mod)) {
            mod.sendMessage(plugin.getMessagesConfig().get("spec-not-active"));
            return;
        }
        stopSession(mod.getUniqueId(), true);
    }

    public void onModDisconnect(UUID modUuid)  { stopSession(modUuid, false); }

    public void onTargetDisconnect(UUID targetUuid, String targetName) {
        Set<UUID> watchers = new HashSet<>(targetToMods.getOrDefault(targetUuid, Set.of()));
        for (UUID modUuid : watchers) {
            SpecSession s = sessions.remove(modUuid);
            if (s == null) continue;
            cancelTasks(s);
            Player mod = plugin.getServer().getPlayer(modUuid);
            if (mod != null && mod.isOnline()) {
                mod.setGameMode(s.originalMode());
                mod.teleport(s.originalLocation());
                mod.removePotionEffect(PotionEffectType.NIGHT_VISION);
                removeGlowTeam(mod);
                mod.sendMessage(plugin.getMessagesConfig().get("spec-target-offline", "player", targetName));
            }
        }
        targetToMods.remove(targetUuid);
    }

    public boolean isSpecting(Player mod) { return sessions.containsKey(mod.getUniqueId()); }

    public void stopAll() {
        for (var entry : new ArrayList<>(sessions.entrySet())) {
            SpecSession s = entry.getValue();
            cancelTasks(s);
            Player mod = plugin.getServer().getPlayer(entry.getKey());
            if (mod != null && mod.isOnline()) {
                mod.setGameMode(s.originalMode());
                mod.teleport(s.originalLocation());
                mod.removePotionEffect(PotionEffectType.NIGHT_VISION);
                removeGlowTeam(mod);
                Player target = plugin.getServer().getPlayer(s.targetUuid());
                if (target != null) GlowUtil.sendGlow(mod, target, false);
            }
        }
        sessions.clear();
        targetToMods.clear();
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private void stopSession(UUID modUuid, boolean restore) {
        SpecSession s = sessions.remove(modUuid);
        if (s == null) return;
        cancelTasks(s);

        Set<UUID> watchers = targetToMods.get(s.targetUuid());
        if (watchers != null) {
            watchers.remove(modUuid);
            if (watchers.isEmpty()) targetToMods.remove(s.targetUuid());
        }

        if (restore) {
            Player mod = plugin.getServer().getPlayer(modUuid);
            if (mod != null && mod.isOnline()) {
                mod.setGameMode(s.originalMode());
                mod.teleport(s.originalLocation());
                mod.removePotionEffect(PotionEffectType.NIGHT_VISION);
                Player target = plugin.getServer().getPlayer(s.targetUuid());
                if (target != null) GlowUtil.sendGlow(mod, target, false);
                removeGlowTeam(mod);
                mod.sendActionBar(plugin.getMessagesConfig().get("spec-stopped"));
            }
        }
    }

    private void cancelTasks(SpecSession s) {
        s.checkTask().cancel();
        if (s.glowTask() != null) s.glowTask().cancel();
    }

    /**
     * Creates or refreshes the glow team on the mod's CURRENT scoreboard.
     * Uses the legacy setColor(ChatColor) because it writes directly to the MC
     * team-color protocol field that controls the glow outline colour.
     */
    @SuppressWarnings("deprecation")
    private void applyGlowTeam(Player mod, String targetName) {
        Scoreboard sb = mod.getScoreboard();
        Team team = sb.getTeam(TEAM_NAME);
        if (team == null) {
            team = sb.registerNewTeam(TEAM_NAME);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            team.setOption(Team.Option.COLLISION_RULE,      Team.OptionStatus.NEVER);
        }

        String colorStr = plugin.getConfigManager().getSpecGlowColor().toUpperCase();

        // Legacy setColor — guaranteed to write the MC protocol team-colour field.
        ChatColor chatColor;
        try { chatColor = ChatColor.valueOf(colorStr); }
        catch (IllegalArgumentException e) { chatColor = ChatColor.YELLOW; }
        team.setColor(chatColor);

        // Also set via Adventure API for display-name colouring.
        NamedTextColor ntColor = NamedTextColor.NAMES.value(colorStr.toLowerCase());
        if (ntColor != null) team.color(ntColor);

        if (!team.hasEntry(targetName)) team.addEntry(targetName);
    }

    @SuppressWarnings("deprecation")
    private void removeGlowTeam(Player mod) {
        Team team = mod.getScoreboard().getTeam(TEAM_NAME);
        if (team != null) try { team.unregister(); } catch (Exception ignored) {}
    }

    // ── Session record ─────────────────────────────────────────────────────

    private record SpecSession(
            UUID       targetUuid,
            String     targetName,
            GameMode   originalMode,
            Location   originalLocation,
            BukkitTask checkTask,
            BukkitTask glowTask
    ) {}
}
