package org.IlSuBb.silentium.checks.ghost;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.checks.CheckCategory;
import org.IlSuBb.silentium.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * Detects sustained high CPS in the ghost range (14–20) typical of subtle auto-clickers.
 */
public class GhostAutoClickerCheck extends CheckBase {

    public GhostAutoClickerCheck(Silentium plugin) {
        super(plugin, "GhostAutoClicker", CheckCategory.GHOST, "autoclicker");
    }

    public void check(Player player, PlayerData data) {
        int maxCps = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getInt("autoclicker.max-cps", 20);
        int minCps = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getInt("autoclicker.min-cps", 14);

        int cps = data.getCps();
        if (cps >= minCps && cps <= maxCps) {
            flag(player, "cps=%d (ghost range %d–%d)".formatted(cps, minCps, maxCps));
        }
    }
}
