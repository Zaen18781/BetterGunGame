package dev.zaen.betterGunGame.arena;

import dev.zaen.betterGunGame.BetterGunGame;
import dev.zaen.betterGunGame.game.GameArena;
import dev.zaen.betterGunGame.game.GamePlayer;
import dev.zaen.betterGunGame.map.GameMap;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages multiple active arenas and player-to-arena lookups.
 */
public class ArenaManager {

    private final BetterGunGame plugin;
    private final SpawnDistributor distributor;
    private final List<GameArena> arenas = new ArrayList<>();

    public ArenaManager(BetterGunGame plugin) {
        this.plugin = plugin;
        this.distributor = new SpawnDistributor();
    }

    /**
     * Creates arenas: randomly selects the right number of maps, then distributes players.
     */
    public List<GameArena> createArenas(List<Player> players) {
        List<GameMap> maps = plugin.getMapManager().selectMapsForGame(players.size());
        Map<GameMap, List<Player>> distribution = distributor.distribute(players, maps);

        arenas.clear();
        for (Map.Entry<GameMap, List<Player>> entry : distribution.entrySet()) {
            GameArena arena = new GameArena(plugin, entry.getKey(), entry.getValue());
            arenas.add(arena);
        }

        return arenas;
    }

    /** Returns the arena containing the given player, or null. */
    public GameArena getArenaForPlayer(UUID uuid) {
        for (GameArena arena : arenas) {
            if (arena.containsPlayer(uuid)) return arena;
        }
        return null;
    }

    /** Returns the GamePlayer object for the given player, or null. */
    public GamePlayer getGamePlayer(UUID uuid) {
        GameArena arena = getArenaForPlayer(uuid);
        return arena != null ? arena.getGamePlayer(uuid) : null;
    }

    public boolean isInGame(UUID uuid) {
        return getArenaForPlayer(uuid) != null;
    }

    public void removeArena(GameArena arena) {
        arenas.remove(arena);
    }

    public void forceEndAll() {
        for (GameArena arena : new ArrayList<>(arenas)) {
            arena.forceEnd();
        }
        arenas.clear();
    }

    public List<GameArena> getArenas() { return arenas; }
}
