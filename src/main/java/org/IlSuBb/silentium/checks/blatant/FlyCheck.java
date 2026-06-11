package org.IlSuBb.silentium.checks.blatant;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.checks.CheckCategory;
import org.IlSuBb.silentium.data.PlayerData;
import org.IlSuBb.silentium.utils.MovementUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class FlyCheck extends CheckBase {

    // Minecraft gravity applied per movement tick when airborne
    private static final double GRAVITY = 0.08;
    // Tolerance added on top of the physics-computed maximum dy
    private static final double ASCEND_TOLERANCE = 0.08;

    public FlyCheck(Silentium plugin) {
        super(plugin, "Fly", CheckCategory.BLATANT, "fly");
    }

    public void check(Player player, PlayerData data, Location to, double dy, boolean onGround) {
        if (MovementUtils.isLegitimatelyFlying(player)) {
            data.resetAirTicks();
            return;
        }
        if (MovementUtils.isClimbing(player)) {
            data.resetAirTicks();
            return;
        }
        // Riptide trident: player launches like a projectile — not detectable as fly
        if (player.isRiptiding()) {
            data.resetAirTicks();
            return;
        }
        if (onGround) {
            data.resetAirTicks();
            data.resetFallDist();
            return;
        }

        double maxJump = MovementUtils.getMaxJumpVelocity(player);

        // ── Type A: ascending beyond what jump physics allow ─────────────────
        // At air-tick N the maximum physically-possible upward velocity is:
        //   maxJump - (N-1) * GRAVITY
        // If the player's dy exceeds this (+ tolerance), they are not just jumping.
        if (dy > 0 && !MovementUtils.hasSlowFalling(player)) {
            double expectedMax = Math.max(0.0, maxJump - (data.getAirTicks() - 1) * GRAVITY);
            if (dy > expectedMax + ASCEND_TOLERANCE) {
                flag(player, "type=Ascend, dy=%.3f, maxDy=%.3f, airTicks=%d"
                        .formatted(dy, expectedMax, data.getAirTicks()));
            }
        }

        // ── Type B: first-tick velocity far exceeds even jump-boost maximum ──
        if (dy > maxJump + 0.05 && data.getAirTicks() == 1) {
            flag(player, "type=HighJump, dy=%.3f, maxJump=%.3f".formatted(dy, maxJump));
        }

        // ── Type C: hovering — check only well past the natural jump peak ────
        // A normal jump (no boost) peaks around airTick 5-6 and lands by tick 12.
        // With Jump Boost the peak is later, so we wait airTick > 14 to be safe.
        int hoverThreshold = 14;
        if (MovementUtils.hasJumpBoost(player)) hoverThreshold = 18;
        if (Math.abs(dy) < 0.03 && data.getAirTicks() > hoverThreshold
                && !MovementUtils.hasSlowFalling(player)) {
            Location check = to.clone().subtract(0, 0.15, 0);
            if (!check.getBlock().getType().isSolid()) {
                flag(player, "type=Hover, dy=%.4f, airTicks=%d".formatted(dy, data.getAirTicks()));
            }
        }

        if (dy < 0) data.accumulateFall((float) -dy);
    }
}
