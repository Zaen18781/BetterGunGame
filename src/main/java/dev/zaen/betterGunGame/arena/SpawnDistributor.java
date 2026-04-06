package dev.zaen.betterGunGame.arena;

import dev.zaen.betterGunGame.map.GameMap;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Distributes players evenly across the given maps.
 *
 * Rule: spread players as evenly as possible so no map gets more than 16.
 * mapsNeeded = ceil(players / 16)
 *
 * Examples:
 *   17 players, 2 maps → [9, 8]
 *   26 players, 2 maps → [13, 13]
 *   32 players, 2 maps → [16, 16]
 *   48 players, 3 maps → [16, 16, 16]
 *   56 players, 4 maps → [14, 14, 14, 14]
 */
public class SpawnDistributor {

    /**
     * Assigns players to the provided maps (already randomly pre-selected by MapManager).
     *
     * @param players shuffled player list
     * @param maps    pre-selected maps (size == mapsNeeded)
     * @return ordered map: GameMap → assigned players
     */
    public Map<GameMap, List<Player>> distribute(List<Player> players, List<GameMap> maps) {
        if (players.isEmpty() || maps.isEmpty()) return Collections.emptyMap();

        List<Player> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);

        int total      = shuffled.size();
        int mapCount   = maps.size();
        int base       = total / mapCount;   // minimum per map
        int extra      = total % mapCount;   // first `extra` maps get base+1

        Map<GameMap, List<Player>> result = new LinkedHashMap<>();
        int idx = 0;
        for (int i = 0; i < mapCount; i++) {
            int count = base + (i < extra ? 1 : 0);
            result.put(maps.get(i), new ArrayList<>(shuffled.subList(idx, idx + count)));
            idx += count;
        }
        return result;
    }
}
