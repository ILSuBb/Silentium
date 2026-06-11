package org.IlSuBb.silentium.managers;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.data.PlayerData;
import org.IlSuBb.silentium.data.ViolationEntry;
import org.bukkit.entity.Player;

public class ViolationManager {

    private final Silentium plugin;

    public ViolationManager(Silentium plugin) {
        this.plugin = plugin;
    }

    public void addViolation(Player player, CheckBase check, String details, int vlAmount) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        int current = data.getViolationLevel(check.getName());
        int newVl = Math.min(current + vlAmount, check.getMaxVL());
        data.setViolationLevel(check.getName(), newVl);

        ViolationEntry entry = new ViolationEntry(
                check.getName(), check.getCategory(), details, newVl, player.getPing());
        data.addViolationEntry(entry);

        plugin.getNotificationManager().sendAlert(player, check, details, newVl);
        plugin.getPunishmentManager().process(player, check.getName(), newVl);
    }

    public int getViolationLevel(Player player, String checkName) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        return data == null ? 0 : data.getViolationLevel(checkName);
    }

    public void resetViolations(Player player) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data != null) data.resetViolations();
    }

    /** Called by the decay scheduler — reduces all VL values by the configured amount. */
    public void tickDecay() {
        if (!plugin.getConfigManager().isVlDecayEnabled()) return;
        int amount = plugin.getConfigManager().getVlDecayAmount();
        for (PlayerData data : plugin.getPlayerDataManager().getAllData()) {
            data.getViolationLevels().replaceAll((k, vl) -> Math.max(0, vl - amount));
        }
    }
}
