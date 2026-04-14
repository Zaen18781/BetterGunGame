package dev.zaen.betterGunGame.glowing;

import de.ZaenCotti.bettercore.BetterCore;
import de.ZaenCotti.bettercore.modules.glowing.GlowingModule;
import de.ZaenCotti.bettercore.modules.glowing.service.GlowingService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Respawn-protection glow (AQUA) using BetterCore's GlowingService when available.
 * Falls back to plain setGlowing(true) if BetterCore is not loaded.
 *
 * Saves each player's pre-existing glow color before applying the protection glow
 * and restores it when protection ends, so personal /glow settings aren't lost.
 */
public class GlowingUtil {

    /** Players currently under respawn-protection glow. */
    private final Set<UUID> glowingPlayers = ConcurrentHashMap.newKeySet();

    /**
     * The glow color each player had BEFORE we applied the protection glow.
     * null value = player had no glow active before respawn protection.
     */
    private final Map<UUID, GlowingService.GlowColor> savedColors = new ConcurrentHashMap<>();

    public void addGlow(Player player) {
        glowingPlayers.add(player.getUniqueId());

        GlowingService service = getService();
        if (service != null) {
            // Only save if they actually have a color — ConcurrentHashMap disallows null values
            GlowingService.GlowColor current = service.getColor(player);
            if (current != null) savedColors.put(player.getUniqueId(), current);
            service.setGlow(player, GlowingService.GlowColor.GREEN);
        } else {
            player.setGlowing(true);
        }
    }

    public void removeGlow(Player player) {
        if (!glowingPlayers.remove(player.getUniqueId())) return;

        GlowingService service = getService();
        if (service != null) {
            GlowingService.GlowColor previous = savedColors.remove(player.getUniqueId());
            if (previous != null) {
                // Restore their original glow color
                service.setGlow(player, previous);
            } else {
                // They had no glow before — remove it entirely
                service.removeGlow(player);
            }
        } else {
            player.setGlowing(false);
        }
    }

    public boolean isGlowing(Player player) {
        return glowingPlayers.contains(player.getUniqueId());
    }

    public void clearAll() {
        for (UUID uuid : glowingPlayers.toArray(new UUID[0])) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) removeGlow(p);
        }
        glowingPlayers.clear();
        savedColors.clear();
    }

    /** No-op — BetterCore handles glow sync automatically. */
    public void sendGlowState(Player viewer) {}

    private static GlowingService getService() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("BetterCore");
        if (!(plugin instanceof BetterCore bc)) return null;
        GlowingModule module = bc.getModuleRegistry().getModule("glowing");
        return module != null ? module.getService() : null;
    }
}
