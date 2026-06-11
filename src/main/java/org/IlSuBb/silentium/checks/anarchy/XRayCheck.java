package org.IlSuBb.silentium.checks.anarchy;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.checks.CheckCategory;
import org.IlSuBb.silentium.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class XRayCheck extends CheckBase {

    public XRayCheck(Silentium plugin) {
        super(plugin, "XRay", CheckCategory.ANARCHY, "xray");
    }

    public void onBlockBreak(Player player, PlayerData data, Block block) {
        boolean isOre     = isTrackedOre(block.getType());
        boolean isStone   = isStoneVariant(block.getType());

        long windowMs = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getLong("xray.window-seconds", 30) * 1000L;

        // Reset window if expired
        if (System.currentTimeMillis() - data.getOreWindowStart() > windowMs) {
            data.resetOreWindow();
        }

        if (isOre) {
            data.incrementOreBreaks();
            checkPattern(player, data, block.getLocation());
            data.setLastOreLocation(block.getLocation());
        } else if (isStone) {
            data.incrementStoneBreaks();
        }
    }

    private void checkPattern(Player player, PlayerData data, Location oreLoc) {
        int oreThreshold = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getInt("xray.ore-threshold", 3);
        double stoneRatioThreshold = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getDouble("xray.stone-ratio-threshold", 0.15);

        int ores   = data.getOreBreaksInWindow();
        int stones = data.getStoneBreaksInWindow();

        if (ores < oreThreshold) return;

        // Suspicious: very few stone blocks between ore finds
        double ratio = stones == 0 ? 0.0 : (double) stones / ores;
        if (ratio < stoneRatioThreshold) {
            flag(player, "ores=%d, stone=%d, ratio=%.2f".formatted(ores, stones, ratio));
            data.resetOreWindow();
            return;
        }

        // Suspicious: navigating directly between ores (short distance despite new ore find)
        Location lastOre = data.getLastOreLocation();
        if (lastOre != null && lastOre.getWorld() != null
                && lastOre.getWorld().equals(oreLoc.getWorld())) {
            double dist = lastOre.distance(oreLoc);
            if (dist < 8 && stones < 3) {
                flag(player, "directPath, ores=%d, dist=%.1f, stone=%d"
                        .formatted(ores, dist, stones));
                data.resetOreWindow();
            }
        }
    }

    private boolean isTrackedOre(Material mat) {
        List<String> tracked = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getStringList("xray.track-ores");
        Set<String> names = tracked.stream().map(String::toUpperCase).collect(Collectors.toSet());
        return names.contains(mat.name());
    }

    private boolean isStoneVariant(Material mat) {
        return switch (mat) {
            case STONE, COBBLESTONE, DEEPSLATE, COBBLED_DEEPSLATE,
                    GRANITE, DIORITE, ANDESITE, TUFF -> true;
            default -> false;
        };
    }
}
