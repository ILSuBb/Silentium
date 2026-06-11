package org.IlSuBb.silentium.listeners;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.blatant.*;
import org.IlSuBb.silentium.checks.ghost.*;
import org.IlSuBb.silentium.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public class CombatListener implements Listener {

    private final Silentium plugin;
    private final KillAuraCheck killAuraCheck;
    private final ReachCheck reachCheck;
    private final GhostReachCheck ghostReachCheck;
    private final AutoClickerCheck autoClickerCheck;
    private final GhostAutoClickerCheck ghostAutoClickerCheck;
    private final AimAssistCheck aimAssistCheck;
    private final AntiKnockbackCheck antiKbCheck;
    private final VelocityCheck velocityCheck;
    private final NoFallCheck noFallCheck;

    public CombatListener(Silentium plugin) {
        this.plugin = plugin;
        killAuraCheck         = plugin.getCheckManager().get(KillAuraCheck.class);
        reachCheck            = plugin.getCheckManager().get(ReachCheck.class);
        ghostReachCheck       = plugin.getCheckManager().get(GhostReachCheck.class);
        autoClickerCheck      = plugin.getCheckManager().get(AutoClickerCheck.class);
        ghostAutoClickerCheck = plugin.getCheckManager().get(GhostAutoClickerCheck.class);
        aimAssistCheck        = plugin.getCheckManager().get(AimAssistCheck.class);
        antiKbCheck           = plugin.getCheckManager().get(AntiKnockbackCheck.class);
        velocityCheck         = plugin.getCheckManager().get(VelocityCheck.class);
        noFallCheck           = plugin.getCheckManager().get(NoFallCheck.class);
    }

    /** Attack checks — player is the damager. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (isNPC(player)) return;
        if (player.hasPermission("silentium.bypass")) return;

        PlayerData data = plugin.getPlayerDataManager().getOrCreate(player);
        data.addAttackTimestamp(System.currentTimeMillis());

        if (killAuraCheck != null) killAuraCheck.check(player, data, target);

        // Reach is skipped when the player is in a fast-movement state where the
        // server-side position lags behind the actual position:
        //  • Gliding (elytra + firework): packets arrive out-of-order during boost
        //  • Riptide: player launches at high speed, position packets lag
        //  • Fast fall (dy < -0.4): likely mace smash — player is above target
        boolean reachExempt = player.isGliding()
                || player.isRiptiding()
                || (data.getAirTicks() > 0 && data.getLastDeltaY() < -0.4);
        if (!reachExempt) {
            if (reachCheck      != null) reachCheck.check(player, data, target);
            if (ghostReachCheck != null) ghostReachCheck.check(player, data, target);
        }

        if (autoClickerCheck      != null) autoClickerCheck.check(player, data);
        if (ghostAutoClickerCheck != null) ghostAutoClickerCheck.check(player, data);
        if (aimAssistCheck        != null) aimAssistCheck.onAttack(player, data, target);
    }

    /** NoFall — player is taking fall damage. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data != null && noFallCheck != null) noFallCheck.onFallDamage(player, data);
    }

    /**
     * Knockback / AntiKB detection via PlayerVelocityEvent.
     * This fires when the server sets a player's velocity (including knockback).
     * We filter on significant horizontal velocity to isolate combat knockback.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("silentium.bypass")) return;

        var v = event.getVelocity();
        double horizMag = Math.sqrt(v.getX() * v.getX() + v.getZ() * v.getZ());
        if (horizMag < 0.15) return; // ignore trivial velocity changes

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        if (antiKbCheck   != null) antiKbCheck.onKnockback(player, data, v);
        if (velocityCheck != null) velocityCheck.onKnockback(player, data, v);
    }

    private boolean isNPC(Entity entity) {
        return plugin.getConfigManager().isCitizensCompatEnabled()
                && Bukkit.getPluginManager().isPluginEnabled("Citizens")
                && entity.hasMetadata("NPC");
    }
}
