package dev.zaen.betterGunGame.game;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;

public class GamePlayer {

    private final UUID uuid;
    private final String name;

    private int level = 1;
    private int kills = 0;
    private String lastAttackerName = null;

    // Respawn protection state
    private boolean protected_ = false;
    private BukkitTask protectionTask = null;

    /** Saved hotbar layout: material → slot (0-7). Null = use default positions. */
    private Map<Material, Integer> layoutSlots = null;

    public GamePlayer(Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public void incrementLevel() { level++; }

    public int getKills() { return kills; }
    public void incrementKills() { kills++; }

    public String getLastAttackerName() { return lastAttackerName; }
    public void setLastAttackerName(String name) { this.lastAttackerName = name; }

    public boolean isProtected() { return protected_; }
    public void setProtected(boolean value) { this.protected_ = value; }

    public BukkitTask getProtectionTask() { return protectionTask; }
    public void setProtectionTask(BukkitTask task) { this.protectionTask = task; }

    public Map<Material, Integer> getLayoutSlots() { return layoutSlots; }
    public void setLayoutSlots(Map<Material, Integer> slots) { this.layoutSlots = slots; }
    public void clearLayoutSlots() { this.layoutSlots = null; }
}
