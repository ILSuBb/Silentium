package org.IlSuBb.silentium.checks.ghost;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.checks.CheckCategory;
import org.IlSuBb.silentium.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * Detects Timer cheat by counting PlayerMoveEvent firings per second.
 * A normal client sends ≤20–22 position updates per second; timer cheats exceed this.
 */
public class TimerCheck extends CheckBase {

    public TimerCheck(Silentium plugin) {
        super(plugin, "Timer", CheckCategory.GHOST, "timer");
    }

    public void check(Player player, PlayerData data) {
        int max = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getInt("timer.max-events-per-second", 25);

        int events = data.getMovementEventsInWindow(1000L);
        if (events > max) {
            flag(player, "events/s=%d, max=%d".formatted(events, max));
        }
    }
}
