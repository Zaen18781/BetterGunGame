package dev.zaen.betterGunGame.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class SoundUtil {

    private SoundUtil() {}

    public static void play(Player player, String soundKey, float volume, float pitch) {
        // Use string-based playSound — works for all valid Minecraft sound keys
        String key = soundKey.contains(":") ? soundKey : "minecraft:" + soundKey;
        player.playSound(player.getLocation(), key, volume, pitch);
    }

    public static void play(Player player, String soundKey) {
        play(player, soundKey, 1.0f, 1.0f);
    }

    public static void playAtLocation(Location location, String soundKey, float volume, float pitch) {
        String key = soundKey.contains(":") ? soundKey : "minecraft:" + soundKey;
        location.getWorld().playSound(location, key, volume, pitch);
    }

    public static void broadcast(String soundKey, float volume, float pitch) {
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            play(p, soundKey, volume, pitch);
        }
    }

    public static void broadcast(String soundKey) {
        broadcast(soundKey, 1.0f, 1.0f);
    }
}
