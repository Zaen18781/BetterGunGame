package dev.zaen.betterGunGame.map;

import dev.zaen.betterGunGame.BetterGunGame;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MapManager {

    private final BetterGunGame plugin;
    private final MapSetupManager setupManager;
    private final List<GameMap> maps = new ArrayList<>();
    /** Saved map order (map names). Empty = no saved order, use insertion order. */
    private List<String> savedMapOrder = new ArrayList<>();

    /** World names that belong to this plugin (prefix bgg_). */
    public static final String WORLD_PREFIX = "bgg_";

    public MapManager(BetterGunGame plugin) {
        this.plugin = plugin;
        this.setupManager = new MapSetupManager(plugin);
    }

    // ---- Loading ----

    public void loadMaps() {
        maps.clear();

        // Extract maps bundled inside the JAR to the plugin data folder
        extractMapsFromJar();

        File mapsFolder = new File(plugin.getDataFolder(), "maps");
        if (!mapsFolder.exists()) {
            mapsFolder.mkdirs();
            plugin.getLogger().info("Maps-Ordner erstellt: " + mapsFolder.getPath());
            return;
        }

        File[] entries = mapsFolder.listFiles(File::isDirectory);
        if (entries == null || entries.length == 0) {
            plugin.getLogger().info("Keine Maps in " + mapsFolder.getPath() + " gefunden.");
            return;
        }

        for (File mapFolder : entries) {
            loadMap(mapFolder);
        }

        plugin.getLogger().info("Maps geladen: " + maps.size());
        loadMapOrder();
    }

    /**
     * Extracts all map folders bundled inside the JAR's /maps/ resource directory
     * to plugins/BetterGunGame/maps/ on the server filesystem.
     * Files that already exist are skipped so server-side edits are never overwritten.
     * session.lock is always skipped — Bukkit manages it when loading worlds.
     */
    private void extractMapsFromJar() {
        File jarFile = plugin.getPluginFile();

        // Safety: only proceed if this is an actual JAR file (not an IDE class directory)
        if (jarFile == null || !jarFile.isFile()) return;

        try (ZipFile zip = new ZipFile(jarFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            int extracted = 0;

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                // Only process entries inside the maps/ directory
                if (!name.startsWith("maps/") || name.equals("maps/")) continue;

                // Skip session.lock — Bukkit creates and manages it when loading worlds
                if (name.endsWith("session.lock")) continue;

                File target = new File(plugin.getDataFolder(), name);

                if (entry.isDirectory()) {
                    target.mkdirs();
                } else if (!target.exists()) {
                    target.getParentFile().mkdirs();
                    try (InputStream in = zip.getInputStream(entry);
                         FileOutputStream out = new FileOutputStream(target)) {
                        in.transferTo(out);
                        extracted++;
                    }
                }
            }

            if (extracted > 0) {
                plugin.getLogger().info(extracted + " Map-Dateien aus der JAR extrahiert.");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Fehler beim Extrahieren der Maps aus der JAR", e);
        }
    }

    /**
     * Sanitizes a map folder name into a valid Minecraft world name.
     * NamespacedKey only allows [a-z0-9_-./] — umlauts and special chars are replaced.
     */
    private static String sanitizeWorldName(String folderName) {
        String s = folderName.toLowerCase();
        // Replace German umlauts with ASCII equivalents
        s = s.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
        // Replace any remaining invalid characters with underscore
        s = s.replaceAll("[^a-z0-9_\\-./]", "_");
        return s;
    }

    private void loadMap(File mapFolder) {
        String mapName   = mapFolder.getName();
        String worldName = WORLD_PREFIX + sanitizeWorldName(mapName);

        // Copy world folder to server root if not already there
        File worldTarget = new File(Bukkit.getWorldContainer(), worldName);
        if (!worldTarget.exists()) {
            copyFolder(mapFolder, worldTarget);
        }

        // Load (or find already loaded) world
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = Bukkit.createWorld(new WorldCreator(worldName));
        }
        if (world == null) {
            plugin.getLogger().warning("Welt konnte nicht geladen werden: " + worldName);
            return;
        }

        // Load spawns: manual spawns first, gold-block scan as fallback
        List<Location> spawns = setupManager.loadSpawns(mapName, world);
        if (spawns.isEmpty()) {
            plugin.getLogger().info(mapName + ": Keine manuellen Spawns — scanne Gold-Blöcke...");
            spawns = scanGoldBlocks(world);
        }

        if (spawns.isEmpty()) {
            plugin.getLogger().warning(mapName + ": Keine Spawns gefunden! (Gold-Blöcke setzen oder /bgg mapsetup verwenden)");
        }

        maps.add(new GameMap(mapName, world, spawns));
        plugin.getLogger().info("Map geladen: " + mapName + " | " + spawns.size() + " Spawns");
    }

    // ---- Random selection ----

    /**
     * Randomly selects the minimum number of maps needed for the given player count.
     * Formula: mapsNeeded = ceil(playerCount / 16), capped at min(4, maps.size()).
     */
    public List<GameMap> selectMapsForGame(int playerCount) {
        if (maps.isEmpty()) return Collections.emptyList();

        int mapsNeeded = (int) Math.ceil(playerCount / 16.0);
        mapsNeeded = Math.max(1, Math.min(mapsNeeded, Math.min(4, maps.size())));

        List<GameMap> shuffled = new ArrayList<>(maps);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, mapsNeeded);
    }

    // ---- Gold-block scan (fallback) ----

    /**
     * Chunk radius around spawn to scan.
     * 12 chunks = 192 blocks in each direction → covers most game maps.
     */
    private static final int SCAN_CHUNK_RADIUS = 12;

    /**
     * Scans all chunks within SCAN_CHUNK_RADIUS of the world spawn for gold blocks.
     * Chunks are force-loaded if not already in memory (fine at startup for small maps).
     */
    private List<Location> scanGoldBlocks(World world) {
        List<Location> result = new ArrayList<>();
        Location center = world.getSpawnLocation();
        int centerChunkX = center.getBlockX() >> 4;
        int centerChunkZ = center.getBlockZ() >> 4;

        for (int cx = centerChunkX - SCAN_CHUNK_RADIUS; cx <= centerChunkX + SCAN_CHUNK_RADIUS; cx++) {
            for (int cz = centerChunkZ - SCAN_CHUNK_RADIUS; cz <= centerChunkZ + SCAN_CHUNK_RADIUS; cz++) {
                Chunk chunk = world.getChunkAt(cx, cz); // loads chunk if not loaded
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                            Block block = chunk.getBlock(x, y, z);
                            if (block.getType() == Material.GOLD_BLOCK) {
                                result.add(new Location(world,
                                        block.getX() + 0.5,
                                        block.getY() + 1.0,
                                        block.getZ() + 0.5,
                                        0f, 0f));
                            }
                        }
                    }
                }
            }
        }

        if (!result.isEmpty()) {
            plugin.getLogger().info(world.getName() + ": " + result.size() + " Gold-Blöcke gefunden.");
        }
        return result;
    }

    // ---- Helpers ----

    private void copyFolder(File src, File dest) {
        try {
            if (src.isDirectory()) {
                if (!dest.exists()) dest.mkdirs();
                for (String child : src.list()) {
                    copyFolder(new File(src, child), new File(dest, child));
                }
            } else {
                java.nio.file.Files.copy(src.toPath(), dest.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Fehler beim Kopieren von " + src.getName(), e);
        }
    }

    // ---- Map order persistence ----

    private void loadMapOrder() {
        File orderFile = new File(plugin.getDataFolder(), "map_order.yml");
        if (!orderFile.exists()) return;
        savedMapOrder = YamlConfiguration.loadConfiguration(orderFile).getStringList("order");
    }

    /** Saves the given map order to map_order.yml and caches it for future rounds. */
    public void saveMapOrder(List<GameMap> order) {
        savedMapOrder = order.stream().map(GameMap::getName).collect(Collectors.toList());
        File orderFile = new File(plugin.getDataFolder(), "map_order.yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("order", savedMapOrder);
        try {
            cfg.save(orderFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Map-Reihenfolge konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    /**
     * Returns maps sorted by the saved order.
     * Maps not present in the saved order are appended at the end.
     */
    public List<GameMap> getOrderedMaps() {
        if (savedMapOrder.isEmpty()) return new ArrayList<>(maps);
        List<GameMap> result = new ArrayList<>();
        for (String name : savedMapOrder) {
            GameMap map = getMap(name);
            if (map != null) result.add(map);
        }
        for (GameMap map : maps) {
            if (!result.contains(map)) result.add(map);
        }
        return result;
    }

    /** Derives map name from a bgg_ world name. */
    public static String mapNameFromWorld(String worldName) {
        return worldName.startsWith(WORLD_PREFIX) ? worldName.substring(WORLD_PREFIX.length()) : worldName;
    }

    /** Returns true if the world belongs to BetterGunGame. */
    public static boolean isBggWorld(World world) {
        return world != null && world.getName().startsWith(WORLD_PREFIX);
    }

    /** Reloads spawns for all loaded maps (e.g. after /bgg mapsetup). */
    public void reloadSpawns() {
        for (GameMap map : maps) {
            List<Location> spawns = setupManager.loadSpawns(map.getName(), map.getWorld());
            if (spawns.isEmpty()) spawns = scanGoldBlocks(map.getWorld());
            map.getSpawns().clear();
            map.getSpawns().addAll(spawns);
        }
    }

    public List<GameMap> getMaps()                          { return maps; }
    public MapSetupManager getSetupManager()                { return setupManager; }

    public GameMap getMap(String name) {
        return maps.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public GameMap getMapByWorld(World world) {
        return maps.stream()
                .filter(m -> m.getWorld().equals(world))
                .findFirst().orElse(null);
    }
}
