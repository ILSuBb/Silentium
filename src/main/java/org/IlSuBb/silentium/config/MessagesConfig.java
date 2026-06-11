package org.IlSuBb.silentium.config;

import net.kyori.adventure.text.Component;
import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.utils.ColorUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MessagesConfig {

    private final Silentium plugin;
    private FileConfiguration config;

    public MessagesConfig(Silentium plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Returns a parsed Component for the given message key.
     * Substitutes {prefix} and any additional {key} → value pairs.
     *
     * @param key     message key in messages.yml
     * @param entries alternating placeholder key/value pairs
     */
    public Component get(String key, String... entries) {
        String prefix = config.getString("prefix", plugin.getConfigManager().getPrefix());
        String raw = config.getString(key, key);
        raw = raw.replace("{prefix}", prefix);
        return ColorUtils.parse(raw, entries);
    }

    public String getRaw(String key) {
        return config.getString(key, key);
    }
}
