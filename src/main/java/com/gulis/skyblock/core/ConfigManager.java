package com.gulis.skyblock.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads and caches all YAML configuration files used by the plugin.
 *
 * <p>Each config file is loaded into a {@link FileConfiguration} and cached in
 * a map keyed by file name. Default versions are copied from the jar resources
 * when the file does not yet exist on disk.</p>
 */
public class ConfigManager {

    private final Skyblock plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> files = new HashMap<>();

    public ConfigManager(Skyblock plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads (or reloads) every config file the plugin uses.
     */
    public void loadAll() {
        load("config.yml");
        load("shops.yml");
        load("messages.yml");
    }

    /**
     * Loads a single config file. If it does not exist on disk, the default
     * version bundled in the jar is saved first.
     *
     * @param name the file name relative to the plugin data folder
     */
    public void load(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Merge defaults from the jar so new keys appear automatically
        InputStream defaults = plugin.getResource(name);
        if (defaults != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaults, StandardCharsets.UTF_8)));
        }

        configs.put(name, config);
        files.put(name, file);
    }

    /**
     * Saves a config file back to disk.
     *
     * @param name the file name relative to the plugin data folder
     */
    public void save(String name) {
        FileConfiguration config = configs.get(name);
        File file = files.get(name);
        if (config == null || file == null) {
            return;
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + name + ": " + e.getMessage());
        }
    }

    /**
     * Reloads a single config file from disk.
     *
     * @param name the file name relative to the plugin data folder
     */
    public void reload(String name) {
        load(name);
    }

    /**
     * Reloads every loaded config file.
     */
    public void reloadAll() {
        for (String name : configs.keySet()) {
            load(name);
        }
    }

    public FileConfiguration getConfig() {
        return get("config.yml");
    }

    public FileConfiguration get(String name) {
        return configs.get(name);
    }

    public File getFile(String name) {
        return files.get(name);
    }
}
