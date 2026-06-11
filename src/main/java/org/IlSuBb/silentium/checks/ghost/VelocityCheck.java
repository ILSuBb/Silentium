package org.IlSuBb.silentium.checks.ghost;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.checks.CheckCategory;
import org.IlSuBb.silentium.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Detects subtle knockback reduction (ghost Velocity / AntiKB).
 * Mirrors AntiKnockbackCheck logic but with a less strict ratio threshold.
 */
public class VelocityCheck extends CheckBase {

    public VelocityCheck(Silentium plugin) {
        super(plugin, "Velocity", CheckCategory.GHOST, "velocity");
    }

    public void onKnockback(Player player, PlayerData data, Vector knockback) {
        double mag = Math.sqrt(knockback.getX() * knockback.getX() + knockback.getZ() * knockback.getZ());
        if (mag < 0.05) return;

        // Store only if blatant check hasn't already stored a knockback
        if (data.getExpectedKnockbackMagnitude() <= 0) {
            data.setExpectedKnockbackMagnitude(mag);
            data.setExpectedKnockbackTime(System.currentTimeMillis());
            data.resetKnockbackCheck();
        }
    }

    public void checkMovement(Player player, PlayerData data, double dx, double dz) {
        if (data.getExpectedKnockbackMagnitude() <= 0) return;
        if (System.currentTimeMillis() - data.getExpectedKnockbackTime() > 500) {
            data.resetKnockbackCheck();
            return;
        }

        data.incrementKnockbackCheckTicks();
        if (data.getKnockbackCheckTicks() != 2) return;

        double minRatio = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getDouble("velocity.min-velocity-ratio", 0.5);
        double actualSpeed = Math.sqrt(dx * dx + dz * dz);
        double expected = data.getExpectedKnockbackMagnitude();

        if (expected > 0.05 && actualSpeed / expected < minRatio) {
            flag(player, "ratio=%.2f, expected=%.3f, actual=%.3f"
                    .formatted(actualSpeed / expected, expected, actualSpeed));
        }
        data.resetKnockbackCheck();
    }
}
