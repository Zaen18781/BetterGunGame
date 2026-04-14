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
import java.util.stream.Collectors;

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
        return createArenas(players, plugin.getMapManager().selectMapsForGame(players.size()));
    }

    /**
     * Creates arenas using a specific ordered map list (first N maps are picked based on player count).
     */
    public List<GameArena> createArenas(List<Player> players, List<GameMap> orderedMaps) {
        int mapsNeeded = (int) Math.ceil(players.size() / 16.0);
        mapsNeeded = Math.max(1, Math.min(mapsNeeded, Math.min(4, orderedMaps.size())));
        List<GameMap> maps = orderedMaps.stream().limit(mapsNeeded).collect(Collectors.toList());

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
