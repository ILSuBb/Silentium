package org.IlSuBb.silentium.checks.blatant;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.checks.CheckCategory;
import org.IlSuBb.silentium.data.PlayerData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ReachCheck extends CheckBase {

    public ReachCheck(Silentium plugin) {
        super(plugin, "Reach", CheckCategory.BLATANT, "reach");
    }

    /** Subclass constructor for GhostReachCheck. */
    protected ReachCheck(Silentium plugin, String name, CheckCategory category, String configKey) {
        super(plugin, name, category, configKey);
    }

    public void check(Player player, PlayerData data, LivingEntity target) {
        double maxDist = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getDouble("reach.max-distance", 3.6);

        double dist = distance(player, target);
        double comp = lagCompensation(player);

        if (dist > maxDist + comp) {
            flag(player, "dist=%.2f, max=%.2f".formatted(dist, maxDist + comp));
        }
    }

    protected double distance(Player player, LivingEntity target) {
        return player.getEyeLocation().toVector()
                .distance(target.getLocation().add(0, target.getHeight() / 2.0, 0).toVector());
    }

    protected double lagCompensation(Player player) {
        if (!plugin.getConfigManager().isLagCompensationEnabled()) return 0.0;
        int ping = Math.min(player.getPing(), plugin.getConfigManager().getMaxCompensatedPing());
        return (ping / 50.0) * 0.1;
    }
}
