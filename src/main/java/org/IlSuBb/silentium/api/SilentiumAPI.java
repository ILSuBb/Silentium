package org.IlSuBb.silentium.api;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.data.PlayerData;
import org.IlSuBb.silentium.data.ViolationEntry;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public API for other plugins to interact with Silentium.
 *
 * <pre>{@code
 * SilentiumAPI api = SilentiumAPI.get();
 * int vl = api.getVL(player, "KillAura");
 * }</pre>
 */
public final class SilentiumAPI {

    private static SilentiumAPI instance;

    private final Silentium plugin;

    private SilentiumAPI(Silentium plugin) {
        this.plugin = plugin;
    }

    /** Returns the API instance. Null if Silentium is not loaded. */
    public static SilentiumAPI get() {
        if (instance == null) {
            Silentium p = Silentium.getInstance();
            if (p != null) instance = new SilentiumAPI(p);
        }
        return instance;
    }

    /** Current VL for the given check name. 0 if player has no data. */
    public int getVL(Player player, String checkName) {
        return plugin.getViolationManager().getViolationLevel(player, checkName);
    }

    /** All VLs for a player. Returns an unmodifiable snapshot. */
    public Map<String, Integer> getAllVLs(Player player) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        return data == null ? Map.of() : Map.copyOf(data.getViolationLevels());
    }

    /** Recent violation history (up to 100 entries). */
    public List<ViolationEntry> getViolationHistory(Player player) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        return data == null ? List.of() : List.copyOf(data.getViolationHistory());
    }

    /** Returns true if the player is on the Silentium bypass whitelist. */
    public boolean isWhitelisted(UUID uuid) {
        return plugin.isWhitelisted(uuid);
    }

    /** Returns true if the player is on the Silentium bypass whitelist. */
    public boolean isWhitelisted(Player player) {
        return isWhitelisted(player.getUniqueId());
    }

    /** Adds a player to the bypass whitelist (call saveWhitelist() to persist). */
    public void addToWhitelist(UUID uuid) {
        plugin.addToWhitelist(uuid);
    }

    /** Removes a player from the bypass whitelist. */
    public void removeFromWhitelist(UUID uuid) {
        plugin.removeFromWhitelist(uuid);
    }

    /** Resets all VLs for a player. */
    public void resetViolations(Player player) {
        plugin.getViolationManager().resetViolations(player);
    }
}
