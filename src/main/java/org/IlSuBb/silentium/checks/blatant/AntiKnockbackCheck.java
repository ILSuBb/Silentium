package org.IlSuBb.silentium.checks.blatant;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.checks.CheckCategory;
import org.IlSuBb.silentium.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AntiKnockbackCheck extends CheckBase {

    public AntiKnockbackCheck(Silentium plugin) {
        super(plugin, "AntiKnockback", CheckCategory.BLATANT, "antiknockback");
    }

    /**
     * Called from CombatListener (EntityKnockbackEvent) to record the expected knockback.
     */
    public void onKnockback(Player player, PlayerData data, Vector knockback) {
        double mag = Math.sqrt(knockback.getX() * knockback.getX() + knockback.getZ() * knockback.getZ());
        if (mag < 0.05) return; // ignore negligible pushes

        data.setExpectedKnockbackMagnitude(mag);
        data.setExpectedKnockbackTime(System.currentTimeMillis());
        data.resetKnockbackCheck();
    }

    /**
     * Called from MovementListener on each position change after knockback was recorded.
     */
    public void checkMovement(Player player, PlayerData data, double dx, double dz) {
        if (data.getExpectedKnockbackMagnitude() <= 0) return;
        if (System.currentTimeMillis() - data.getExpectedKnockbackTime() > 500) {
            data.resetKnockbackCheck();
            return;
        }

        data.incrementKnockbackCheckTicks();
        if (data.getKnockbackCheckTicks() != 2) return; // check on tick 2

        double minRatio = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getDouble("antiknockback.min-velocity-ratio", 0.25);
        double actualSpeed = Math.sqrt(dx * dx + dz * dz);
        double expected = data.getExpectedKnockbackMagnitude();

        if (expected > 0.05 && actualSpeed / expected < minRatio) {
            flag(player, "ratio=%.2f, expected=%.3f, actual=%.3f"
                    .formatted(actualSpeed / expected, expected, actualSpeed));
        }
        data.resetKnockbackCheck();
    }
}
