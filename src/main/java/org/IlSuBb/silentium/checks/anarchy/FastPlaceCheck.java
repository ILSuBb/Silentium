package org.IlSuBb.silentium.checks.anarchy;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.checks.CheckCategory;
import org.IlSuBb.silentium.data.PlayerData;
import org.bukkit.entity.Player;

public class FastPlaceCheck extends CheckBase {

    public FastPlaceCheck(Silentium plugin) {
        super(plugin, "FastPlace", CheckCategory.ANARCHY, "fastplace");
    }

    public void check(Player player, PlayerData data) {
        int maxBps = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getInt("fastplace.max-blocks-per-second", 8);

        int bps = data.getBlocksPlacedPerSecond();
        if (bps > maxBps) {
            flag(player, "bps=%d, max=%d".formatted(bps, maxBps));
        }
    }
}
