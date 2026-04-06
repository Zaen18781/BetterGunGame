package dev.zaen.betterGunGame.config;

import dev.zaen.betterGunGame.BetterGunGame;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private final BetterGunGame plugin;
    private FileConfiguration config;
    private FileConfiguration itemsConfig;
    private FileConfiguration guiConfig;

    public ConfigManager(BetterGunGame plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        File itemsFile = new File(plugin.getDataFolder(), "items.yml");
        if (!itemsFile.exists()) plugin.saveResource("items.yml", false);
        this.itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);

        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) plugin.saveResource("gui.yml", false);
        this.guiConfig = YamlConfiguration.loadConfiguration(guiFile);
    }

    // ---- game settings ----
    public int getRoundDurationSeconds()     { return config.getInt("game.round-duration-seconds", 600); }
    public int getMaxLevels()                { return config.getInt("game.max-levels", 50); }
    public int getRespawnProtectionSeconds() { return config.getInt("game.respawn-protection-seconds", 5); }
    public int getMinPlayers()               { return config.getInt("game.min-players", 2); }
    public int getMaxPlayersPerMap()         { return config.getInt("game.max-players-per-map", 16); }
    public int getCountdownSeconds()         { return config.getInt("game.countdown-seconds", 5); }

    // ---- sounds ----
    public boolean areSoundsEnabled()        { return config.getBoolean("sounds.enabled", true); }
    public String getSoundLevelUp()          { return config.getString("sounds.level-up",        "entity.player.levelup"); }
    public String getSoundKill()             { return config.getString("sounds.kill",             "entity.experience_orb.pickup"); }
    public String getSoundRespawn()          { return config.getString("sounds.respawn",          "block.note_block.pling"); }
    public String getSoundCountdownTick()    { return config.getString("sounds.countdown-tick",   "block.note_block.pling"); }
    public String getSoundCountdownGo()      { return config.getString("sounds.countdown-go",     "entity.ender_dragon.growl"); }
    public String getSoundGameEnd()          { return config.getString("sounds.game-end",         "ui.toast.challenge_complete"); }
    public String getSoundEventStart()       { return config.getString("sounds.event-start",      "block.note_block.chime"); }

    // ---- random events ----
    public boolean areRandomEventsEnabled()  { return config.getBoolean("random-events.enabled", true); }
    public int getEventIntervalSeconds()     { return config.getInt("random-events.interval-seconds", 60); }
    public int getEventDurationSeconds()     { return config.getInt("random-events.duration-seconds", 15); }

    // ---- messages (raw MiniMessage strings — TextUtil.parse() applies SmallCaps at display time) ----
    public String getPrefix()               { return config.getString("messages.prefix", ""); }
    public String getMessage(String key)    { return getPrefix() + config.getString("messages." + key, ""); }
    public String getRawMessage(String key) { return config.getString("messages." + key, ""); }

    // ---- lobby ----
    public Location getLobbyLocation() {
        return config.getLocation("lobby-location");
    }

    public void setLobbyLocation(Location location) {
        config.set("lobby-location", location);
        plugin.saveConfig();
    }

    // ---- sub-configs ----
    public FileConfiguration getItemsConfig() { return itemsConfig; }
    public FileConfiguration getGuiConfig()   { return guiConfig; }
}
