package org.IlSuBb.silentium.checks.ghost;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.checks.CheckCategory;
import org.IlSuBb.silentium.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ghost-Target (HoneyPot) check.
 *
 * An invisible Zombie entity is spawned behind each player by GhostTargetManager.
 * The entity is invisible to human eyes but present in the client's entity list.
 * Aim-assist cheats scan that list, detect the entity, and snap the player's crosshair
 * onto it.  We detect that snap: a large yaw-delta that ends with the player nearly
 * looking directly at the invisible ghost.
 *
 * Because no real player or visible mob is at the ghost's position, any intentional
 * snap there can only be explained by automated targeting.
 */
public class GhostTargetCheck extends CheckBase {

    // Grace period after ghost placement (ms) — avoids false positives right after spawn.
    private static final long GRACE_MS = 4_000L;

    private final Map<UUID, Double> prevAngle = new HashMap<>();

    public GhostTargetCheck(Silentium plugin) {
        super(plugin, "GhostTarget", CheckCategory.GHOST, "ghosttarget");
    }

    /**
     * Called every movement packet in MovementListener.
     *
     * @param ghostCenter torso-centre Location of the ghost (already offset)
     * @param ghostAgeMs  milliseconds since the ghost was last placed/repositioned
     */
    public void check(Player player, PlayerData data, Location ghostCenter, long ghostAgeMs) {
        if (ghostAgeMs < GRACE_MS) return;

        var cfg = plugin.getConfigManager().getCheckConfig(getCategory());
        double minPrevAngle = cfg.getDouble("ghosttarget.min-prev-angle", 25.0);
        double maxFinalAngle = cfg.getDouble("ghosttarget.max-final-angle", 3.5);
        double minSnapDelta  = cfg.getDouble("ghosttarget.min-snap-delta",  20.0);

        UUID uuid = player.getUniqueId();
        double angle = angleToPoint(player, ghostCenter);

        Double prev = prevAngle.put(uuid, angle);
        if (prev == null) return;

        double delta = prev - angle; // positive → player rotated toward ghost

        if (delta >= minSnapDelta && prev >= minPrevAngle && angle <= maxFinalAngle) {
            // Player snapped from a large angle to nearly-perfect alignment with an
            // invisible entity that no legitimate input would target.
            flag(player, "type=GhostSnap, angle=%.2f°, delta=%.1f°, age=%dms"
                    .formatted(angle, delta, ghostAgeMs));
        }
    }

    public void clearPlayer(UUID uuid) {
        prevAngle.remove(uuid);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static double angleToPoint(Player player, Location point) {
        Vector toPoint = point.toVector()
                .subtract(player.getEyeLocation().toVector())
                .normalize();
        Vector look = player.getEyeLocation().getDirection().normalize();
        double dot  = Math.max(-1.0, Math.min(1.0, look.dot(toPoint)));
        return Math.toDegrees(Math.acos(dot));
    }
}
