package dev.zaen.betterGunGame.game;

import org.bukkit.entity.Player;

import java.util.UUID;

public class GamePlayer {

    private final UUID uuid;
    private final String name;

    private int level = 1;
    private int kills = 0;
    private String lastAttackerName = null;

    // Respawn protection state
    private boolean protected_ = false;

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
}
