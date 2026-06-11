package org.IlSuBb.silentium.checks;

import org.IlSuBb.silentium.Silentium;
import org.bukkit.entity.Player;

public abstract class CheckBase {

    protected final Silentium plugin;
    private final String name;
    private final CheckCategory category;
    private final String configKey;

    protected CheckBase(Silentium plugin, String name, CheckCategory category, String configKey) {
        this.plugin = plugin;
        this.name = name;
        this.category = category;
        this.configKey = configKey;
    }

    // ── Config helpers ─────────────────────────────────────────────────────

    public boolean isEnabled() {
        return plugin.getConfigManager().getCheckConfig(category).getBoolean(configKey + ".enabled", true);
    }

    public int getMaxVL() {
        return plugin.getConfigManager().getCheckConfig(category).getInt(configKey + ".max-vl", 20);
    }

    public int getVlPerFlag() {
        return plugin.getConfigManager().getCheckConfig(category).getInt(configKey + ".vl-per-flag", 1);
    }

    // ── Flagging ───────────────────────────────────────────────────────────

    protected void flag(Player player, String details) {
        flagWith(player, details, getVlPerFlag());
    }

    protected void flagWith(Player player, String details, int vlAmount) {
        if (!isEnabled()) return;
        if (player.hasPermission("silentium.bypass")) return;
        if (plugin.isWhitelisted(player.getUniqueId())) return;
        plugin.getViolationManager().addViolation(player, this, details, vlAmount);
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    public String getName() { return name; }
    public CheckCategory getCategory() { return category; }
    public String getConfigKey() { return configKey; }
}
