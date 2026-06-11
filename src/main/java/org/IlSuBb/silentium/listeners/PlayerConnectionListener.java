package org.IlSuBb.silentium.listeners;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.ghost.AimAssistCheck;
import org.IlSuBb.silentium.checks.ghost.GhostTargetCheck;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final Silentium plugin;

    public PlayerConnectionListener(Silentium plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        plugin.getPlayerDataManager().getOrCreate(player);

        var ghostMgr = plugin.getGhostTargetManager();
        if (ghostMgr != null) {
            // 1. Hide every existing ghost from the newcomer so they can't see other players' ghosts
            ghostMgr.hideAllFromPlayer(player);
            // 2. Spawn a ghost for the newcomer (hidden from all existing players)
            ghostMgr.spawnGhost(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();
        var uuid   = player.getUniqueId();
        var spec   = plugin.getSpecManager();

        spec.onModDisconnect(uuid);
        spec.onTargetDisconnect(uuid, player.getName());

        var aimAssist = plugin.getCheckManager().get(AimAssistCheck.class);
        if (aimAssist != null) aimAssist.removePlayer(uuid);

        var ghostTarget = plugin.getCheckManager().get(GhostTargetCheck.class);
        if (ghostTarget != null) ghostTarget.clearPlayer(uuid);

        var ghostMgr = plugin.getGhostTargetManager();
        if (ghostMgr != null) ghostMgr.removeGhost(uuid);

        plugin.getPlayerDataManager().remove(uuid);
    }
}
