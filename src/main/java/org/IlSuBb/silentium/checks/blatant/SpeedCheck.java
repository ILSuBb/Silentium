package org.IlSuBb.silentium.checks.blatant;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.checks.CheckCategory;
import org.IlSuBb.silentium.data.PlayerData;
import org.IlSuBb.silentium.utils.MovementUtils;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SpeedCheck extends CheckBase {

    // Grace window after player stops gliding or riptiding (ms).
    // Elytra/riptide leave high horizontal momentum that bleeds into the first
    // ground-ticks after landing.
    private static final long POST_FLIGHT_GRACE_MS = 2500L;

    public SpeedCheck(Silentium plugin) {
        super(plugin, "Speed", CheckCategory.BLATANT, "speed");
    }

    public void check(Player player, PlayerData data, double dx, double dz, boolean onGround) {
        if (MovementUtils.isLegitimatelyFlying(player)) return;
        if (MovementUtils.isClimbing(player)) return;
        if (MovementUtils.isOnSlipperyBlock(player)) return;
        if (!onGround) return;

        // Active riptide: player is mid-launch
        if (player.isRiptiding()) return;

        long now = System.currentTimeMillis();

        // Post-elytra landing: horizontal momentum from the glide bleeds over
        if (now - data.getLastGlideEndMs() < POST_FLIGHT_GRACE_MS) return;

        // Fast-fall landing grace: if the previous movement packet had large negative dy,
        // the player just landed from a high-speed descent (riptide, explosion, fast elytra).
        // The first ground-tick still carries that horizontal momentum.
        if (data.getLastDeltaY() < -0.5) return;

        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double maxSpeed = MovementUtils.getMaxHorizontalSpeed(player);

        // Soul Speed boots on soul sand / soul soil: speed is boosted by the enchantment.
        // Adjust max instead of skipping so we still catch blatant hacks.
        if (MovementUtils.isOnSoulSpeedSurface(player)) {
            ItemStack boots = player.getInventory().getBoots();
            int soulLevel = (boots != null) ? boots.getEnchantmentLevel(Enchantment.SOUL_SPEED) : 0;
            if (soulLevel > 0) {
                // Soul Speed multiplier: ×(1 + 0.3 × level), add 20 % safety margin
                maxSpeed *= (1.0 + 0.3 * soulLevel) * 1.2;
            } else {
                // Soul sand without enchantment slows the player — no speed hack possible
                return;
            }
        }

        double multiplier = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getDouble("speed.multiplier", 1.15);

        if (horizontal > maxSpeed * multiplier) {
            flag(player, "speed=%.4f, max=%.4f".formatted(horizontal, maxSpeed * multiplier));
        }
    }
}
