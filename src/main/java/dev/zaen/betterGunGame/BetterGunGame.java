package dev.zaen.betterGunGame;

import dev.zaen.betterGunGame.command.GunGameCommand;
import dev.zaen.betterGunGame.config.ConfigManager;
import dev.zaen.betterGunGame.game.GameManager;
import dev.zaen.betterGunGame.game.LevelManager;
import dev.zaen.betterGunGame.listener.GameListener;
import dev.zaen.betterGunGame.listener.ProtectionListener;
import dev.zaen.betterGunGame.map.MapManager;
import dev.zaen.betterGunGame.worldguard.RegionUtils;
import org.bukkit.plugin.java.JavaPlugin;

public final class BetterGunGame extends JavaPlugin {

    private ConfigManager configManager;
    private MapManager mapManager;
    private LevelManager levelManager;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        // 1. Config
        configManager = new ConfigManager(this);
        configManager.load();

        // 2. Maps
        mapManager = new MapManager(this);
        mapManager.loadMaps();

        // 3. Levels
        levelManager = new LevelManager(this);
        levelManager.load();

        // 4. WorldGuard regions
        for (dev.zaen.betterGunGame.map.GameMap map : mapManager.getMaps()) {
            try {
                RegionUtils.createMapRegion(map.getWorld(), getLogger());
            } catch (Exception e) {
                getLogger().warning("WorldGuard-Region für " + map.getName() + " fehlgeschlagen: " + e.getMessage());
            }
        }

        // 5. Game manager
        gameManager = new GameManager(this);

        // 6. Listeners
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);

        // 7. Commands
        GunGameCommand cmd = new GunGameCommand(this);
        var cmdObj = getCommand("bettergungame");
        if (cmdObj != null) {
            cmdObj.setExecutor(cmd);
            cmdObj.setTabCompleter(cmd);
        }

        getLogger().info("BetterGunGame v" + getPluginMeta().getVersion() + " aktiviert!");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.getArenaManager().forceEndAll();
        }
        getLogger().info("BetterGunGame deaktiviert.");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public MapManager getMapManager() { return mapManager; }
    public LevelManager getLevelManager() { return levelManager; }
    public GameManager getGameManager() { return gameManager; }
}
