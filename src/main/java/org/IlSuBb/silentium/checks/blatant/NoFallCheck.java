package org.IlSuBb.silentium.checks.blatant;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.checks.CheckCategory;
import org.IlSuBb.silentium.data.PlayerData;
import org.bukkit.entity.Player;

public class NoFallCheck extends CheckBase {

    public NoFallCheck(Silentium plugin) {
        super(plugin, "NoFall", CheckCategory.BLATANT, "nofall");
    }

    /**
     * Called from MovementListener when server determines the player just landed
     * (transitioned from airborne to on-ground).
     */
    public void onLand(Player player, PlayerData data) {
        float fallDist = data.getServerFallDist();
        double minDist = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getDouble("nofall.min-fall-distance", 3.5);

        data.resetFallDist();
        data.setPendingFallCheck(false);

        if (fallDist < minDist) return;

        // Schedule a 2-tick window: if no fall-damage event fires, flag
        float capturedDist = fallDist;
        data.setPendingFallCheck(true);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (data.isPendingFallCheck()) {
                flag(player, "fallDist=%.2f".formatted(capturedDist));
                data.setPendingFallCheck(false);
            }
        }, 2L);
    }

    /**
     * Called from CombatListener when FALL EntityDamageEvent fires — clears the
     * pending check so we don't false-flag legitimate fall damage.
     */
    public void onFallDamage(Player player, PlayerData data) {
        data.setPendingFallCheck(false);
    }
}
