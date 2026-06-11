package org.IlSuBb.silentium.data;

import org.bukkit.Location;

import java.util.*;

public class PlayerData {

    private final UUID uuid;
    private final String playerName;

    // ── Movement tracking ──────────────────────────────────────────────────
    private Location lastLocation;
    private double lastDeltaX, lastDeltaY, lastDeltaZ;
    private double lastYaw, lastPitch;
    private boolean wasOnGround;
    private int airTicks;            // consecutive ticks spent airborne
    private float serverFallDist;    // accumulated fall distance tracked server-side
    private boolean pendingFallCheck; // player fell > threshold, awaiting damage event
    private final List<Long> movementTimestamps = new ArrayList<>();

    // ── Combat tracking ────────────────────────────────────────────────────
    private final Deque<Long> attackTimestamps = new ArrayDeque<>();
    private final List<Double> yawDeltas = new ArrayList<>();
    private final List<Double> pitchDeltas = new ArrayList<>();

    // AntiKnockback / Velocity
    private double expectedKnockbackMagnitude;
    private long expectedKnockbackTime; // ms timestamp when KB was applied
    private int knockbackCheckTicks;    // movement ticks since KB was applied

    // ── Special-state timestamps (for bypass logic in checks) ─────────────
    private boolean currentlyGliding = false;
    private long    lastGlideEndMs   = 0L;  // ms when player stopped gliding
    private long    lastRiptideMs    = 0L;  // ms when last riptide fired

    // ── Block interaction tracking ─────────────────────────────────────────
    private final Deque<Long> blockPlaceTimestamps = new ArrayDeque<>();
    private int scaffoldCount;         // consecutive scaffold-style block placements

    // XRay
    private int stoneBreaksInWindow;
    private int oreBreaksInWindow;
    private long oreWindowStart;
    private Location lastOreLocation;

    // ── Violation tracking ─────────────────────────────────────────────────
    private final Map<String, Integer> violationLevels = new HashMap<>();
    private final List<ViolationEntry> violationHistory = new ArrayList<>();

    // ── Staff flags ────────────────────────────────────────────────────────
    private boolean alertsEnabled = true;
    private boolean verboseTarget = false;

    public PlayerData(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.oreWindowStart = System.currentTimeMillis();
    }

    // ── Identity ───────────────────────────────────────────────────────────

    public UUID getUuid() { return uuid; }
    public String getPlayerName() { return playerName; }

    // ── Movement ───────────────────────────────────────────────────────────

    public Location getLastLocation() { return lastLocation; }
    public void setLastLocation(Location loc) { this.lastLocation = loc; }

    public double getLastDeltaX() { return lastDeltaX; }
    public double getLastDeltaY() { return lastDeltaY; }
    public double getLastDeltaZ() { return lastDeltaZ; }

    public void setDeltas(double dx, double dy, double dz) {
        this.lastDeltaX = dx;
        this.lastDeltaY = dy;
        this.lastDeltaZ = dz;
    }

    public double getLastYaw() { return lastYaw; }
    public void setLastYaw(double yaw) { this.lastYaw = yaw; }

    public double getLastPitch() { return lastPitch; }
    public void setLastPitch(double pitch) { this.lastPitch = pitch; }

    public boolean wasOnGround() { return wasOnGround; }
    public void setWasOnGround(boolean onGround) { this.wasOnGround = onGround; }

    public int getAirTicks() { return airTicks; }
    public void incrementAirTicks() { airTicks++; }
    public void resetAirTicks() { airTicks = 0; }

    public float getServerFallDist() { return serverFallDist; }
    public void setServerFallDist(float dist) { this.serverFallDist = dist; }
    public void accumulateFall(float delta) { this.serverFallDist += delta; }
    public void resetFallDist() { this.serverFallDist = 0f; }

    public boolean isPendingFallCheck() { return pendingFallCheck; }
    public void setPendingFallCheck(boolean v) { this.pendingFallCheck = v; }

    public List<Long> getMovementTimestamps() { return movementTimestamps; }

    public boolean isCurrentlyGliding()            { return currentlyGliding; }
    public void setCurrentlyGliding(boolean v)    { this.currentlyGliding = v; }

    public long getLastGlideEndMs()               { return lastGlideEndMs; }
    public void setLastGlideEndMs(long ms)        { this.lastGlideEndMs = ms; }

    public long getLastRiptideMs()                { return lastRiptideMs; }
    public void setLastRiptideMs(long ms)         { this.lastRiptideMs = ms; }

    public void addMovementTimestamp(long ts) {
        movementTimestamps.add(ts);
        movementTimestamps.removeIf(t -> t < ts - 2000L);
    }

    /** Returns movement events in the past {@code windowMs} milliseconds. */
    public int getMovementEventsInWindow(long windowMs) {
        long cutoff = System.currentTimeMillis() - windowMs;
        int count = 0;
        for (long t : movementTimestamps) {
            if (t >= cutoff) count++;
        }
        return count;
    }

    // ── Combat ─────────────────────────────────────────────────────────────

    public void addAttackTimestamp(long ts) {
        attackTimestamps.addLast(ts);
        while (!attackTimestamps.isEmpty() && attackTimestamps.peekFirst() < ts - 1000L) {
            attackTimestamps.pollFirst();
        }
    }

    /** Returns CPS over the last second. */
    public int getCps() { return attackTimestamps.size(); }

    public void addRotationDelta(double yawDelta, double pitchDelta) {
        yawDeltas.add(Math.abs(yawDelta));
        pitchDeltas.add(Math.abs(pitchDelta));
        if (yawDeltas.size() > 10) yawDeltas.remove(0);
        if (pitchDeltas.size() > 10) pitchDeltas.remove(0);
    }

    public List<Double> getYawDeltas() { return yawDeltas; }
    public List<Double> getPitchDeltas() { return pitchDeltas; }

    public double getExpectedKnockbackMagnitude() { return expectedKnockbackMagnitude; }
    public void setExpectedKnockbackMagnitude(double mag) { this.expectedKnockbackMagnitude = mag; }

    public long getExpectedKnockbackTime() { return expectedKnockbackTime; }
    public void setExpectedKnockbackTime(long ts) { this.expectedKnockbackTime = ts; }

    public int getKnockbackCheckTicks() { return knockbackCheckTicks; }
    public void incrementKnockbackCheckTicks() { knockbackCheckTicks++; }
    public void resetKnockbackCheck() {
        knockbackCheckTicks = 0;
        expectedKnockbackMagnitude = 0;
        expectedKnockbackTime = 0;
    }

    // ── Block placement ────────────────────────────────────────────────────

    public void addBlockPlaceTimestamp(long ts) {
        blockPlaceTimestamps.addLast(ts);
        while (!blockPlaceTimestamps.isEmpty() && blockPlaceTimestamps.peekFirst() < ts - 1000L) {
            blockPlaceTimestamps.pollFirst();
        }
    }

    public int getBlocksPlacedPerSecond() { return blockPlaceTimestamps.size(); }

    public int getScaffoldCount() { return scaffoldCount; }
    public void incrementScaffoldCount() { scaffoldCount++; }
    public void resetScaffoldCount() { scaffoldCount = 0; }

    // ── XRay ───────────────────────────────────────────────────────────────

    public int getStoneBreaksInWindow() { return stoneBreaksInWindow; }
    public void incrementStoneBreaks() { stoneBreaksInWindow++; }

    public int getOreBreaksInWindow() { return oreBreaksInWindow; }
    public void incrementOreBreaks() { oreBreaksInWindow++; }

    public long getOreWindowStart() { return oreWindowStart; }

    public void resetOreWindow() {
        stoneBreaksInWindow = 0;
        oreBreaksInWindow = 0;
        oreWindowStart = System.currentTimeMillis();
    }

    public Location getLastOreLocation() { return lastOreLocation; }
    public void setLastOreLocation(Location loc) { this.lastOreLocation = loc; }

    // ── Violations ─────────────────────────────────────────────────────────

    public Map<String, Integer> getViolationLevels() { return violationLevels; }

    public int getViolationLevel(String checkName) {
        return violationLevels.getOrDefault(checkName, 0);
    }

    public void setViolationLevel(String checkName, int vl) {
        violationLevels.put(checkName, vl);
    }

    public void addViolationEntry(ViolationEntry entry) {
        violationHistory.add(0, entry);
        if (violationHistory.size() > 100) violationHistory.remove(violationHistory.size() - 1);
    }

    public List<ViolationEntry> getViolationHistory() {
        return Collections.unmodifiableList(violationHistory);
    }

    public void resetViolations() {
        violationLevels.clear();
        violationHistory.clear();
    }

    // ── Staff ──────────────────────────────────────────────────────────────

    public boolean isAlertsEnabled() { return alertsEnabled; }
    public void setAlertsEnabled(boolean v) { this.alertsEnabled = v; }

    public boolean isVerboseTarget() { return verboseTarget; }
    public void setVerboseTarget(boolean v) { this.verboseTarget = v; }
}
