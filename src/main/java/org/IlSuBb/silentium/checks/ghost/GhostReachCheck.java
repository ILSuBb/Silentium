package org.IlSuBb.silentium.checks.ghost;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckCategory;
import org.IlSuBb.silentium.checks.blatant.ReachCheck;
import org.IlSuBb.silentium.data.PlayerData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Detects subtle reach extensions (3.1–3.5 blocks) typical of ghost clients.
 * Re-uses distance/lag-compensation logic from ReachCheck.
 */
public class GhostReachCheck extends ReachCheck {

    public GhostReachCheck(Silentium plugin) {
        super(plugin, "GhostReach", CheckCategory.GHOST, "reach");
    }

    @Override
    public void check(Player player, PlayerData data, LivingEntity target) {
        double maxDist = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getDouble("reach.max-distance", 3.5);
        double minDist = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getDouble("reach.min-distance", 3.1);

        double dist = distance(player, target);
        double comp = lagCompensation(player);

        if (dist >= minDist && dist > maxDist + comp) {
            flag(player, "dist=%.2f, max=%.2f".formatted(dist, maxDist + comp));
        }
    }
}
