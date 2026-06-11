package org.IlSuBb.silentium.checks.blatant;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.checks.CheckCategory;
import org.IlSuBb.silentium.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class ScaffoldCheck extends CheckBase {

    public ScaffoldCheck(Silentium plugin) {
        super(plugin, "Scaffold", CheckCategory.BLATANT, "scaffold");
    }

    /**
     * Called from BlockListener (BlockPlaceEvent).
     *
     * @param onGround server-computed ground state at time of placement
     */
    public void check(Player player, PlayerData data, Block placed, boolean onGround) {
        Location playerLoc = player.getLocation();

        // Block must be at foot level - 1 (directly beneath where the player will step)
        int expectedY = (int) Math.floor(playerLoc.getY()) - 1;
        if (placed.getY() != expectedY) {
            data.resetScaffoldCount();
            return;
        }

        // Player must be moving (not just standing and placing down)
        double horizSpeed = Math.sqrt(
                data.getLastDeltaX() * data.getLastDeltaX()
                + data.getLastDeltaZ() * data.getLastDeltaZ());

        double minSpeed = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getDouble("scaffold.min-speed", 1.5) / 20.0;

        if (horizSpeed < minSpeed) {
            data.resetScaffoldCount();
            return;
        }

        data.incrementScaffoldCount();

        int threshold = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getInt("scaffold.consecutive-threshold", 4);

        if (data.getScaffoldCount() >= threshold) {
            flag(player, "count=%d, speed=%.3f".formatted(data.getScaffoldCount(), horizSpeed * 20));
            data.resetScaffoldCount();
        }
    }
}
