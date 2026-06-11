package org.IlSuBb.silentium.checks.ghost;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.checks.CheckCategory;
import org.IlSuBb.silentium.data.PlayerData;
import org.IlSuBb.silentium.utils.MathUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AimAssistCheck extends CheckBase {

    // How many consecutive suspicious snaps before flagging.
    private static final int SNAP_STREAK_REQUIRED = 3;

    // Per-player counters for consecutive snap hits/misses.
    private final Map<UUID, Integer> snapStreak = new HashMap<>();

    public AimAssistCheck(Silentium plugin) {
        super(plugin, "AimAssist", CheckCategory.GHOST, "aimassist");
    }

    /**
     * Called from CombatListener when the player attacks a living entity.
     */
    public void onAttack(Player player, PlayerData data, LivingEntity target) {
        double snapThreshold = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getDouble("aimassist.snap-threshold", 65.0);
        double snapMaxAngle = plugin.getConfigManager()
                .getCheckConfig(getCategory()).getDouble("aimassist.snap-max-angle", 2.0);

        UUID uuid = player.getUniqueId();

        // ── Snap check ───────────────────────────────────────────────────────
        // Aim-assist snaps: the player rotated sharply in the last movement packet
        // AND the view direction is now nearly perfectly aligned to the target.
        // We require SNAP_STREAK_REQUIRED consecutive suspicious attacks to avoid
        // flagging players who legitimately pivot to face a mob.
        List<Double> yawDeltas = data.getYawDeltas();
        if (!yawDeltas.isEmpty()) {
            double lastYawDelta = yawDeltas.get(yawDeltas.size() - 1);

            Vector look     = player.getEyeLocation().getDirection().normalize();
            Vector toTarget = target.getLocation()
                    .add(0, target.getHeight() / 2.0, 0)
                    .toVector()
                    .subtract(player.getEyeLocation().toVector())
                    .normalize();
            double angle = MathUtils.angleBetween(look, toTarget);

            if (lastYawDelta >= snapThreshold && angle < snapMaxAngle) {
                int streak = snapStreak.merge(uuid, 1, Integer::sum);
                if (streak >= SNAP_STREAK_REQUIRED) {
                    flag(player, "type=Snap, yawDelta=%.1f°, angle=%.2f°, streak=%d"
                            .formatted(lastYawDelta, angle, streak));
                }
            } else {
                // Attack was not suspicious — reset streak
                snapStreak.remove(uuid);
            }
        }

        // NOTE: GCD analysis on server-side float rotation values is unreliable —
        // the Euclidean algorithm on floats converges to machine epsilon (~1e-15)
        // for any non-integer input (e.g. 1.1°, 2.3°), producing guaranteed false
        // positives for all legitimate players. Accurate GCD detection requires the
        // raw fixed-point values from the network packet, which are not available
        // through the standard Bukkit/Paper API. GCD sub-check is intentionally omitted.
    }

    /** Clean up state when player leaves. */
    public void removePlayer(UUID uuid) {
        snapStreak.remove(uuid);
    }
}
