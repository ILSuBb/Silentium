package org.IlSuBb.silentium.utils;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public final class MovementUtils {

    private MovementUtils() {}

    /**
     * Returns the maximum expected horizontal speed in blocks-per-tick for a player,
     * accounting for sprint, Speed potion, and Slowness potion.
     * Does NOT account for ice / soul-sand — callers should skip those surface types.
     */
    public static double getMaxHorizontalSpeed(Player player) {
        // Sprint speed: ~0.2806, walk: ~0.2160, sneak: ~0.065
        double base = player.isSprinting() ? 0.2806 : 0.2160;

        var speed = player.getPotionEffect(PotionEffectType.SPEED);
        if (speed != null) {
            base += (speed.getAmplifier() + 1) * 0.0206;
        }

        var slowness = player.getPotionEffect(PotionEffectType.SLOWNESS);
        if (slowness != null) {
            base = Math.max(0.05, base - (slowness.getAmplifier() + 1) * 0.015);
        }

        return base;
    }

    public static boolean isLegitimatelyFlying(Player player) {
        return player.getAllowFlight()
                || player.isFlying()
                || player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR
                || player.isGliding()
                || player.hasPotionEffect(PotionEffectType.LEVITATION);
    }

    public static boolean hasSlowFalling(Player player) {
        return player.hasPotionEffect(PotionEffectType.SLOW_FALLING);
    }

    public static boolean hasJumpBoost(Player player) {
        return player.hasPotionEffect(PotionEffectType.JUMP_BOOST);
    }

    /** Max upward velocity at the start of a jump, accounting for Jump Boost. */
    public static double getMaxJumpVelocity(Player player) {
        double base = 0.42;
        var jumpBoost = player.getPotionEffect(PotionEffectType.JUMP_BOOST);
        if (jumpBoost != null) {
            base += (jumpBoost.getAmplifier() + 1) * 0.1;
        }
        return base;
    }

    public static boolean isOnSlipperyBlock(Player player) {
        Block below = player.getLocation().clone().subtract(0, 0.1, 0).getBlock();
        return below.getType() == Material.ICE
                || below.getType() == Material.PACKED_ICE
                || below.getType() == Material.BLUE_ICE
                || below.getType() == Material.FROSTED_ICE
                || below.getType() == Material.SLIME_BLOCK
                || below.getType() == Material.HONEY_BLOCK;
    }

    public static boolean isOnSoulSand(Player player) {
        Block below = player.getLocation().clone().subtract(0, 0.1, 0).getBlock();
        return below.getType() == Material.SOUL_SAND;
    }

    /** True when player is standing on soul sand or soul soil (Soul Speed boots apply here). */
    public static boolean isOnSoulSpeedSurface(Player player) {
        Block below = player.getLocation().clone().subtract(0, 0.1, 0).getBlock();
        return below.getType() == Material.SOUL_SAND || below.getType() == Material.SOUL_SOIL;
    }

    public static boolean isClimbing(Player player) {
        return player.isClimbing();
    }
}
