package dev.zaen.betterGunGame.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.World;

import java.util.logging.Logger;

public final class RegionUtils {

    private RegionUtils() {}

    /**
     * Creates a GunGame protection region for a map world.
     * Region covers the entire world column (-4096 to 4096 XZ, full Y range).
     */
    public static void createMapRegion(World world, Logger logger) {
        RegionManager rm = getRegionManager(world);
        if (rm == null) {
            logger.warning("WorldGuard RegionManager nicht verfügbar für " + world.getName());
            return;
        }

        String regionName = "bgg_" + world.getName();

        // If already exists, just update flags
        ProtectedRegion region = rm.getRegion(regionName);
        if (region == null) {
            BlockVector3 min = BlockVector3.at(-4096, world.getMinHeight(), -4096);
            BlockVector3 max = BlockVector3.at(4096, world.getMaxHeight() - 1, 4096);
            region = new ProtectedCuboidRegion(regionName, min, max);
            rm.addRegion(region);
        }

        RegionFlagBuilder.of(region)
                .allow(Flags.PVP)
                .deny(Flags.BLOCK_BREAK)
                .deny(Flags.BLOCK_PLACE)
                .deny(Flags.MOB_SPAWNING)
                .deny(Flags.ITEM_DROP)
                .deny(Flags.ITEM_PICKUP)
                .deny(Flags.FALL_DAMAGE);

        try {
            rm.saveChanges();
        } catch (Exception e) {
            logger.warning("Fehler beim Speichern der WorldGuard-Region: " + e.getMessage());
        }

        logger.info("WorldGuard-Region erstellt: " + regionName);
    }

    /**
     * Removes the GunGame region for a world.
     */
    public static void removeMapRegion(World world) {
        RegionManager rm = getRegionManager(world);
        if (rm == null) return;

        String regionName = "bgg_" + world.getName();
        rm.removeRegion(regionName);
        try {
            rm.saveChanges();
        } catch (Exception ignored) {}
    }

    private static RegionManager getRegionManager(World world) {
        try {
            return WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(world));
        } catch (Exception e) {
            return null;
        }
    }
}
