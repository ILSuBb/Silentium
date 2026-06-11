package org.IlSuBb.silentium.listeners;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.blatant.*;
import org.IlSuBb.silentium.checks.ghost.*;
import org.IlSuBb.silentium.checks.anarchy.PhaseCheck;
import org.IlSuBb.silentium.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MovementListener implements Listener {

    private final Silentium plugin;
    private final FlyCheck flyCheck;
    private final SpeedCheck speedCheck;
    private final NoFallCheck noFallCheck;
    private final AntiKnockbackCheck antiKbCheck;
    private final TimerCheck timerCheck;
    private final PhaseCheck phaseCheck;
    private final VelocityCheck velocityCheck;
    private final GhostTargetCheck ghostTargetCheck;

    public MovementListener(Silentium plugin) {
        this.plugin = plugin;
        flyCheck         = plugin.getCheckManager().get(FlyCheck.class);
        speedCheck       = plugin.getCheckManager().get(SpeedCheck.class);
        noFallCheck      = plugin.getCheckManager().get(NoFallCheck.class);
        antiKbCheck      = plugin.getCheckManager().get(AntiKnockbackCheck.class);
        timerCheck       = plugin.getCheckManager().get(TimerCheck.class);
        phaseCheck       = plugin.getCheckManager().get(PhaseCheck.class);
        velocityCheck    = plugin.getCheckManager().get(VelocityCheck.class);
        ghostTargetCheck = plugin.getCheckManager().get(GhostTargetCheck.class);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isNPC(player)) return;
        if (player.hasPermission("silentium.bypass")) return;

        PlayerData data = plugin.getPlayerDataManager().getOrCreate(player);
        Location from = event.getFrom();
        Location to   = event.getTo();
        if (to == null) return;

        // Elytra glide-end tracking: record the moment the player stops gliding so
        // SpeedCheck can apply a grace window for post-elytra momentum.
        boolean nowGliding = player.isGliding();
        if (data.isCurrentlyGliding() && !nowGliding) {
            data.setLastGlideEndMs(System.currentTimeMillis());
        }
        data.setCurrentlyGliding(nowGliding);

        // Rotation tracking (needed for KillAura / AimAssist checks)
        // Normalize yaw to (-180, 180] before taking absolute value to avoid
        // wrap-around artifacts (e.g. 1° → 359° would produce 358° without this).
        float rawYaw = to.getYaw() - from.getYaw();
        if (rawYaw >  180f) rawYaw -= 360f;
        if (rawYaw < -180f) rawYaw += 360f;
        double yawDelta   = Math.abs(rawYaw);
        double pitchDelta = Math.abs(to.getPitch() - from.getPitch());
        if (yawDelta > 0 || pitchDelta > 0) {
            data.addRotationDelta(yawDelta, pitchDelta);
        }
        data.setLastYaw(to.getYaw());
        data.setLastPitch(to.getPitch());

        // Ghost-target honeypot: check if the player snapped their aim onto the invisible entity
        if (ghostTargetCheck != null && plugin.getGhostTargetManager() != null) {
            var ghostMgr = plugin.getGhostTargetManager();
            Location ghostLoc = ghostMgr.getGhostLocation(player.getUniqueId());
            if (ghostLoc != null) {
                ghostTargetCheck.check(player, data, ghostLoc, ghostMgr.getGhostAgeMs(player.getUniqueId()));
            }
        }

        if (!event.hasChangedPosition()) return;

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();

        boolean onGround = isServerOnGround(to);
        data.addMovementTimestamp(System.currentTimeMillis());

        // NoFall landing detection (before state update so wasOnGround is previous tick)
        if (!data.wasOnGround() && onGround && data.getAirTicks() > 0 && noFallCheck != null) {
            noFallCheck.onLand(player, data);
        }

        // Run checks
        if (flyCheck    != null) flyCheck.check(player, data, to, dy, onGround);
        if (speedCheck  != null) speedCheck.check(player, data, dx, dz, onGround);
        if (timerCheck  != null) timerCheck.check(player, data);
        if (phaseCheck  != null) phaseCheck.check(player, data, from, to);
        if (antiKbCheck != null) antiKbCheck.checkMovement(player, data, dx, dz);
        if (velocityCheck != null) velocityCheck.checkMovement(player, data, dx, dz);

        // Update state
        data.setDeltas(dx, dy, dz);
        data.setWasOnGround(onGround);
        data.setLastLocation(to.clone());
        if (!onGround) data.incrementAirTicks(); else data.resetAirTicks();
    }

    /** Server-side ground check — does not use the deprecated Player#isOnGround(). */
    private static boolean isServerOnGround(Location loc) {
        return loc.clone().subtract(0, 0.05, 0).getBlock().getType().isSolid()
                || loc.clone().subtract(0, 0.2, 0).getBlock().getType().isSolid();
    }

    private boolean isNPC(Entity entity) {
        return plugin.getConfigManager().isCitizensCompatEnabled()
                && Bukkit.getPluginManager().isPluginEnabled("Citizens")
                && entity.hasMetadata("NPC");
    }
}
