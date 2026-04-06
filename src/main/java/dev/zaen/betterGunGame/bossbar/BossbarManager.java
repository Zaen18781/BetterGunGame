package dev.zaen.betterGunGame.bossbar;

import dev.zaen.betterGunGame.BetterGunGame;
import dev.zaen.betterGunGame.game.GamePlayer;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/**
 * Manages GunGame-specific bossbar content.
 * Format: GunGame · Level: X/50 · Kills: Y · ⌚ MM:SS · #1: Name (Lvl Z)
 */
public class BossbarManager {

    private final BetterGunGame plugin;
    private final BossbarUtil util = new BossbarUtil();

    public BossbarManager(BetterGunGame plugin) {
        this.plugin = plugin;
    }

    /**
     * Updates the bossbar for all arena players.
     *
     * @param players     All players in the arena (with their GamePlayer state)
     * @param timeSeconds Remaining round time in seconds
     * @param maxLevel    Maximum level (usually 50)
     */
    public void updateAll(Collection<GamePlayer> gamePlayers,
                          Collection<Player> onlinePlayers,
                          int timeSeconds, int maxLevel) {

        int minutes = timeSeconds / 60;
        int seconds = timeSeconds % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);

        // Find top player
        Optional<GamePlayer> top = gamePlayers.stream()
                .max(Comparator.comparingInt(GamePlayer::getLevel)
                        .thenComparingInt(GamePlayer::getKills));

        String topStr = top.map(gp -> " <dark_gray>·</dark_gray> <gray>#1: <white>"
                + gp.getName() + "</white> (Lvl " + gp.getLevel() + ")</gray>").orElse("");

        for (Player player : onlinePlayers) {
            GamePlayer gp = gamePlayers.stream()
                    .filter(p -> p.getUuid().equals(player.getUniqueId()))
                    .findFirst().orElse(null);

            if (gp == null) continue;

            float progress = (float) gp.getLevel() / maxLevel;
            String text = "<gradient:#e63278:#fd8ddb><b>GunGame</b></gradient>"
                    + " <dark_gray>·</dark_gray> <gray>Level: <white>" + gp.getLevel() + "/" + maxLevel + "</white></gray>"
                    + " <dark_gray>·</dark_gray> <gray>Kills: <white>" + gp.getKills() + "</white></gray>"
                    + " <dark_gray>·</dark_gray> <gray>⌚ <white>" + timeStr + "</white></gray>"
                    + topStr;

            util.show(player, text, progress, BossBar.Color.PINK);
        }
    }

    public void hide(Player player) {
        util.hide(player);
    }

    public void hideAll() {
        util.hideAll();
    }
}
