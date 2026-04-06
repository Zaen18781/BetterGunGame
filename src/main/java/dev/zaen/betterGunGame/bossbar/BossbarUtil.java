package dev.zaen.betterGunGame.bossbar;

import dev.zaen.betterGunGame.util.TextUtil;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player BossBars, ensuring each player has exactly one
 * BossBar instance that gets updated rather than recreated every tick.
 */
public class BossbarUtil {

    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<>();

    /**
     * Shows or updates a BossBar for the player.
     */
    public void show(Player player, String miniMessage, float progress,
                     BossBar.Color color, BossBar.Overlay overlay) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        BossBar existing = bars.get(player.getUniqueId());
        if (existing != null) {
            existing.name(TextUtil.parse(miniMessage));
            existing.progress(clamped);
            existing.color(color);
            existing.overlay(overlay);
        } else {
            BossBar bar = BossBar.bossBar(TextUtil.parse(miniMessage), clamped, color, overlay);
            bars.put(player.getUniqueId(), bar);
            player.showBossBar(bar);
        }
    }

    /**
     * Shows or updates a BossBar with default NOTCHED_20 overlay.
     */
    public void show(Player player, String miniMessage, float progress, BossBar.Color color) {
        show(player, miniMessage, progress, color, BossBar.Overlay.NOTCHED_20);
    }

    /**
     * Hides and removes the BossBar for the player.
     */
    public void hide(Player player) {
        BossBar bar = bars.remove(player.getUniqueId());
        if (bar != null) player.hideBossBar(bar);
    }

    /**
     * Hides all tracked BossBars (e.g., on plugin disable or game end).
     */
    public void hideAll() {
        for (Map.Entry<UUID, BossBar> entry : bars.entrySet()) {
            Player p = org.bukkit.Bukkit.getPlayer(entry.getKey());
            if (p != null) p.hideBossBar(entry.getValue());
        }
        bars.clear();
    }
}
