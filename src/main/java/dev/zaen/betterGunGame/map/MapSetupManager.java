package dev.zaen.betterGunGame.map;

import dev.zaen.betterGunGame.BetterGunGame;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Saves and loads manually placed spawn points per map.
 * File: plugins/BetterGunGame/spawns/<mapName>.yml
 *
 * Manual spawns take priority over gold-block scan.
 */
public class MapSetupManager {

    private final BetterGunGame plugin;
    private final File spawnsFolder;

    public MapSetupManager(BetterGunGame plugin) {
        this.plugin = plugin;
        this.spawnsFolder = new File(plugin.getDataFolder(), "spawns");
        if (!spawnsFolder.exists()) spawnsFolder.mkdirs();
    }

    /**
     * Sets (or overwrites) a spawn point for the given map.
     *
     * @param mapName  the map's folder name (without bgg_ prefix)
     * @param index    1-based spawn index (1–16)
     * @param location the exact location incl. yaw/pitch
     */
    public void setSpawn(String mapName, int index, Location location) {
        File file = getFile(mapName);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        String path = "spawns." + index;
        cfg.set(path + ".x",     location.getX());
        cfg.set(path + ".y",     location.getY());
        cfg.set(path + ".z",     location.getZ());
        cfg.set(path + ".yaw",   (double) location.getYaw());
        cfg.set(path + ".pitch", (double) location.getPitch());

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Fehler beim Speichern von spawns/" + mapName + ".yml", e);
        }
    }

    /**
     * Loads all saved spawn points for the given map and world.
     * Returns an empty list if no file exists.
     */
    public List<Location> loadSpawns(String mapName, World world) {
        File file = getFile(mapName);
        if (!file.exists()) return List.of();

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("spawns");
        if (section == null) return List.of();

        List<Location> result = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;

            double x     = s.getDouble("x");
            double y     = s.getDouble("y");
            double z     = s.getDouble("z");
            float  yaw   = (float) s.getDouble("yaw");
            float  pitch = (float) s.getDouble("pitch");
            result.add(new Location(world, x, y, z, yaw, pitch));
        }
        return result;
    }

    /**
     * Returns how many spawns are saved for a map (0 if file doesn't exist).
     */
    public int getSpawnCount(String mapName) {
        File file = getFile(mapName);
        if (!file.exists()) return 0;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection s = cfg.getConfigurationSection("spawns");
        return s == null ? 0 : s.getKeys(false).size();
    }

    private File getFile(String mapName) {
        return new File(spawnsFolder, mapName + ".yml");
    }
}
