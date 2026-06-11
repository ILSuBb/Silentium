package org.IlSuBb.silentium.checks.anarchy;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.checks.CheckCategory;
import org.IlSuBb.silentium.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class PhaseCheck extends CheckBase {

    public PhaseCheck(Silentium plugin) {
        super(plugin, "Phase", CheckCategory.ANARCHY, "phase");
    }

    public void check(Player player, PlayerData data, Location from, Location to) {
        double dist = from.distance(to);

        // Only check significant movement — tiny movements and lag-teleports are skipped
        if (dist < 0.5 || dist > 5.0) return;

        double step = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getDouble("phase.check-step", 0.25);

        Vector direction = to.toVector().subtract(from.toVector());
        int steps = (int) Math.ceil(dist / step);

        for (int i = 1; i <= steps; i++) {
            double fraction = (i * step) / dist;
            fraction = Math.min(fraction, 1.0);
            Location point = from.clone().add(direction.clone().multiply(fraction));
            if (isSolidBlock(point)) {
                flag(player, "passedThrough=%s, dist=%.2f".formatted(
                        point.getBlock().getType().name(), dist));
                return;
            }
        }
    }

    private boolean isSolidBlock(Location loc) {
        var block = loc.getBlock();
        return block.getType().isSolid()
                && block.getBoundingBox().contains(loc.toVector());
    }
}
