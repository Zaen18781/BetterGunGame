package dev.zaen.betterGunGame.worldguard;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * Fluent builder for applying WorldGuard flags to a region.
 */
public class RegionFlagBuilder {

    private final ProtectedRegion region;

    private RegionFlagBuilder(ProtectedRegion region) {
        this.region = region;
    }

    public static RegionFlagBuilder of(ProtectedRegion region) {
        return new RegionFlagBuilder(region);
    }

    public RegionFlagBuilder allow(StateFlag flag) {
        region.setFlag(flag, StateFlag.State.ALLOW);
        return this;
    }

    public RegionFlagBuilder deny(StateFlag flag) {
        region.setFlag(flag, StateFlag.State.DENY);
        return this;
    }

    public <T> RegionFlagBuilder set(Flag<T> flag, T value) {
        region.setFlag(flag, value);
        return this;
    }

    public ProtectedRegion build() {
        return region;
    }
}
