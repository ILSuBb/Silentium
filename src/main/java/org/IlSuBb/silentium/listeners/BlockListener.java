package org.IlSuBb.silentium.listeners;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.anarchy.FastPlaceCheck;
import org.IlSuBb.silentium.checks.anarchy.XRayCheck;
import org.IlSuBb.silentium.checks.blatant.ScaffoldCheck;
import org.IlSuBb.silentium.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockListener implements Listener {

    private final Silentium plugin;
    private final XRayCheck xRayCheck;
    private final FastPlaceCheck fastPlaceCheck;
    private final ScaffoldCheck scaffoldCheck;

    public BlockListener(Silentium plugin) {
        this.plugin = plugin;
        xRayCheck     = plugin.getCheckManager().get(XRayCheck.class);
        fastPlaceCheck = plugin.getCheckManager().get(FastPlaceCheck.class);
        scaffoldCheck  = plugin.getCheckManager().get(ScaffoldCheck.class);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isNPC(player)) return;
        if (player.hasPermission("silentium.bypass")) return;

        PlayerData data = plugin.getPlayerDataManager().getOrCreate(player);
        if (xRayCheck != null) xRayCheck.onBlockBreak(player, data, event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (isNPC(player)) return;
        if (player.hasPermission("silentium.bypass")) return;

        PlayerData data = plugin.getPlayerDataManager().getOrCreate(player);
        data.addBlockPlaceTimestamp(System.currentTimeMillis());

        boolean onGround = isServerOnGround(player.getLocation());

        if (fastPlaceCheck != null) fastPlaceCheck.check(player, data);
        if (scaffoldCheck  != null) scaffoldCheck.check(player, data, event.getBlock(), onGround);
    }

    private static boolean isServerOnGround(Location loc) {
        return loc.clone().subtract(0, 0.05, 0).getBlock().getType().isSolid()
                || loc.clone().subtract(0, 0.2, 0).getBlock().getType().isSolid();
    }

    private boolean isNPC(Entity entity) {
        return plugin.getConfigManager().isCitizensCompatEnabled()
                && Bukkit.getPluginManager().isPluginEnabled("Citizens")
                && entity.hasMetadata("NPC");
    }
}
