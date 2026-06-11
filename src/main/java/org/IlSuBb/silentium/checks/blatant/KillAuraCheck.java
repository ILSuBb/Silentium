package org.IlSuBb.silentium.checks.blatant;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.checks.CheckCategory;
import org.IlSuBb.silentium.data.PlayerData;
import org.IlSuBb.silentium.utils.MathUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class KillAuraCheck extends CheckBase {

    public KillAuraCheck(Silentium plugin) {
        super(plugin, "KillAura", CheckCategory.BLATANT, "killaura");
    }

    public void check(Player player, PlayerData data, LivingEntity target) {
        double maxAngle = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getDouble("killaura.max-angle", 90.0);

        Vector lookDir  = player.getEyeLocation().getDirection().normalize();
        Vector toTarget = target.getLocation()
                .add(0, target.getHeight() / 2.0, 0)
                .toVector()
                .subtract(player.getEyeLocation().toVector())
                .normalize();

        double angle = MathUtils.angleBetween(lookDir, toTarget);

        if (angle > maxAngle) {
            flag(player, "angle=%.1f°, max=%.1f°".formatted(angle, maxAngle));
        }
    }
}
