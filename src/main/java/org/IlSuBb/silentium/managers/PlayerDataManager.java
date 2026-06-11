package org.IlSuBb.silentium.managers;

import org.IlSuBb.silentium.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final Map<UUID, PlayerData> dataMap = new HashMap<>();

    public PlayerData get(UUID uuid) {
        return dataMap.get(uuid);
    }

    public PlayerData getOrCreate(Player player) {
        return dataMap.computeIfAbsent(
                player.getUniqueId(),
                k -> new PlayerData(k, player.getName()));
    }

    public void remove(UUID uuid) {
        dataMap.remove(uuid);
    }

    public Collection<PlayerData> getAllData() {
        return dataMap.values();
    }

    public int getTrackedCount() {
        return dataMap.size();
    }
}
