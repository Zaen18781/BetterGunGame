package dev.zaen.betterGunGame.map;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class GameMap {

    private final String name;
    private final World world;
    private final List<Location> spawns;

    public GameMap(String name, World world, List<Location> spawns) {
        this.name = name;
        this.world = world;
        this.spawns = new ArrayList<>(spawns);
    }

    public String getName() { return name; }
    public World getWorld() { return world; }
    public List<Location> getSpawns() { return spawns; }

    public int getMaxPlayers() { return spawns.size(); }

    /** Returns a random spawn location from the list. */
    public Location getRandomSpawn() {
        if (spawns.isEmpty()) return world.getSpawnLocation();
        return spawns.get((int) (Math.random() * spawns.size()));
    }
}
