package org.IlSuBb.silentium.managers;

import org.IlSuBb.silentium.Silentium;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages one invisible "ghost" Zombie per online player.
 *
 * The ghost entity exists on the server (real entity) but:
 *  • Has setInvisible(true) — no visual model, but a live hitbox remains in the
 *    client's entity list.  Aim-assist cheats scan that list and lock onto the
 *    ghost, producing a detectable rotation snap.
 *  • Is hidden from every other player via Paper's hideEntity API.
 *
 * The ghost is repositioned every N seconds behind and to the side of the player
 * so that it cannot be learned by pattern.
 */
public class GhostTargetManager {

    private static final long REPOSITION_TICKS = 200L; // 10 s
    private static final int  TORSO_HEIGHT_OFFSET = 1; // add to ghost feet Y for centre

    private final Silentium plugin;
    private final Map<UUID, GhostSession> sessions = new HashMap<>();
    private BukkitTask repositionTask;

    public GhostTargetManager(Silentium plugin) {
        this.plugin = plugin;
        repositionTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin, this::repositionAll, REPOSITION_TICKS, REPOSITION_TICKS);
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /** Spawn a ghost for {@code player}. Noop if one already exists. */
    public void spawnGhost(Player player) {
        if (sessions.containsKey(player.getUniqueId())) return;

        Location loc = calcGhostLoc(player);
        if (loc == null) return;

        Zombie ghost = player.getWorld().spawn(loc, Zombie.class, z -> {
            z.setAI(false);
            z.setSilent(true);
            z.setGravity(false);
            z.setInvisible(true);      // entity metadata invisible bit → no model, hitbox stays
            z.setInvulnerable(true);
            z.setCustomNameVisible(false);
            z.setPersistent(false);
            z.setRemoveWhenFarAway(false);
            if (z.getEquipment() != null) z.getEquipment().clear();
        });

        // Hide from all currently online players except the owner
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(player.getUniqueId())) {
                p.hideEntity(plugin, ghost);
            }
        }

        sessions.put(player.getUniqueId(), new GhostSession(ghost, System.currentTimeMillis()));
    }

    /** Remove ghost when the owner disconnects. */
    public void removeGhost(UUID ownerUuid) {
        GhostSession s = sessions.remove(ownerUuid);
        if (s != null) s.entity().remove();
    }

    /**
     * Returns the ghost's torso-centre location for the given player,
     * or null if no ghost exists / ghost is in a different world.
     */
    public Location getGhostLocation(UUID ownerUuid) {
        Player owner = Bukkit.getPlayer(ownerUuid);
        GhostSession s = sessions.get(ownerUuid);
        if (s == null || owner == null) return null;
        if (!s.entity().isValid()) { sessions.remove(ownerUuid); return null; }
        if (!s.entity().getWorld().equals(owner.getWorld())) return null;
        return s.entity().getLocation().clone().add(0, TORSO_HEIGHT_OFFSET, 0);
    }

    /** Milliseconds since the ghost was last placed for this player. */
    public long getGhostAgeMs(UUID ownerUuid) {
        GhostSession s = sessions.get(ownerUuid);
        return s == null ? Long.MAX_VALUE : System.currentTimeMillis() - s.spawnTime();
    }

    /** Called on PlayerJoinEvent: hide all existing ghosts from the newcomer. */
    public void hideAllFromPlayer(Player joiner) {
        for (GhostSession s : sessions.values()) {
            if (s.entity().isValid()) joiner.hideEntity(plugin, s.entity());
        }
    }

    /** Cancel task and remove all ghost entities (plugin disable). */
    public void stopAll() {
        if (repositionTask != null) repositionTask.cancel();
        sessions.values().forEach(s -> { if (s.entity().isValid()) s.entity().remove(); });
        sessions.clear();
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private void repositionAll() {
        for (var it = sessions.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            Player owner = Bukkit.getPlayer(entry.getKey());

            if (owner == null || !owner.isOnline() || !entry.getValue().entity().isValid()) {
                entry.getValue().entity().remove();
                it.remove();
                continue;
            }

            Location newLoc = calcGhostLoc(owner);
            if (newLoc != null) {
                entry.getValue().entity().teleport(newLoc);
                // Reset spawn time so the grace window restarts after reposition
                entry.setValue(new GhostSession(entry.getValue().entity(), System.currentTimeMillis()));
            }
        }
    }

    /**
     * Calculates a ghost position 5-8 blocks BEHIND and to the side of the player.
     * "Behind" is defined as 140-180 degrees offset from the player's look direction,
     * ensuring the ghost is never in the player's natural FOV at the moment of placement.
     */
    private Location calcGhostLoc(Player player) {
        double dist = plugin.getConfigManager().getCheckConfig(
                org.IlSuBb.silentium.checks.CheckCategory.GHOST)
                .getDouble("ghosttarget.ghost-distance", 6.0);

        Location loc = player.getLocation();
        double yawRad = Math.toRadians(loc.getYaw());
        // 140-180 degrees offset from forward = behind the player
        double offset = Math.toRadians(140 + Math.random() * 40);
        double angle  = yawRad + offset;

        double gx = loc.getX() - Math.sin(angle) * dist;
        double gz = loc.getZ() + Math.cos(angle) * dist;

        Location candidate = new Location(loc.getWorld(), gx, loc.getY(), gz);

        // Walk upward to find a non-solid position
        for (int i = 0; i < 4; i++) {
            if (!candidate.getBlock().getType().isSolid()) return candidate;
            candidate.setY(candidate.getY() + 1);
        }
        return null; // surrounded by blocks — skip
    }

    // ── Session record ─────────────────────────────────────────────────────

    private record GhostSession(Zombie entity, long spawnTime) {}
}
