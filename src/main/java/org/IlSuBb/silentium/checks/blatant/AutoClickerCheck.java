package org.IlSuBb.silentium.checks.blatant;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.checks.CheckCategory;
import org.IlSuBb.silentium.data.PlayerData;
import org.bukkit.entity.Player;

public class AutoClickerCheck extends CheckBase {

    public AutoClickerCheck(Silentium plugin) {
        super(plugin, "AutoClicker", CheckCategory.BLATANT, "autoclicker");
    }

    public void check(Player player, PlayerData data) {
        int maxCps = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getInt("autoclicker.max-cps", 20);

        int cps = data.getCps();
        if (cps > maxCps) {
            flag(player, "cps=%d, max=%d".formatted(cps, maxCps));
        }
    }
}
