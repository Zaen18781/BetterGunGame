package dev.zaen.betterGunGame.game;

import dev.zaen.betterGunGame.BetterGunGame;
import dev.zaen.betterGunGame.bossbar.BossbarManager;
import dev.zaen.betterGunGame.event.RandomEvent;
import dev.zaen.betterGunGame.event.RandomEventManager;
import dev.zaen.betterGunGame.glowing.GlowingUtil;
import dev.zaen.betterGunGame.map.GameMap;
import dev.zaen.betterGunGame.util.SoundUtil;
import dev.zaen.betterGunGame.util.TextUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameArena {

    private final BetterGunGame plugin;
    private final GameMap map;
    private final int maxLevel;

    private final Map<UUID, GamePlayer> players = new ConcurrentHashMap<>();
    private final BossbarManager bossbarManager;
    private final GlowingUtil glowingUtil;
    private final RandomEventManager eventManager;

    private int timeRemaining;
    private BukkitTask timerTask;
    private BukkitTask bossbarTask;
    private BukkitTask compassTask;
    private boolean ended = false;

    public GameArena(BetterGunGame plugin, GameMap map, List<Player> initialPlayers) {
        this.plugin = plugin;
        this.map = map;
        this.maxLevel = plugin.getLevelManager().getMaxLevel();
        this.timeRemaining = plugin.getConfigManager().getRoundDurationSeconds();
        this.bossbarManager = new BossbarManager(plugin);
        this.glowingUtil = new GlowingUtil();
        this.eventManager = new RandomEventManager(plugin, this);

        for (Player p : initialPlayers) {
            players.put(p.getUniqueId(), new GamePlayer(p));
        }
    }

    public void start() {
        for (UUID uuid : players.keySet()) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p == null) continue;
            setupPlayer(p);
            teleportToRandomSpawn(p);
        }

        startTimer();
        eventManager.startScheduler();
        startBossbarUpdater();
        startCompassUpdater();
    }

    private void setupPlayer(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(getMaxHealth(player));
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExp(0);
        player.setLevel(0);
        player.getInventory().clear();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setFireTicks(0);
        giveWeapon(player);
        giveCompass(player);
    }

    private void giveWeapon(Player player) {
        GamePlayer gp = players.get(player.getUniqueId());
        if (gp == null) return;
        ItemStack weapon = plugin.getLevelManager().getItemForLevel(gp.getLevel());
        player.getInventory().setItem(0, weapon);
        player.getInventory().setHeldItemSlot(0);
    }

    private void giveCompass(Player player) {
        ItemStack compass = dev.zaen.betterGunGame.util.ItemUtil.builder(org.bukkit.Material.RECOVERY_COMPASS)
                .name("<gradient:#00c6ff:#0072ff><b>ᴘʟᴀʏᴇʀ ᴏᴠᴇʀᴠɪᴇᴡ</b></gradient>")
                .lore(
                        "",
                        "<dark_aqua>⬡ ᴢᴇɪɢᴛ ᴅᴇɴ ɴäᴄʜsᴛᴇɴ ɢᴇɢɴᴇʀ</dark_aqua>",
                        "")
                .unbreakable()
                .build();
        player.getInventory().setItem(8, compass);
        // Don't call updateCompass here — player hasn't been teleported to the map world yet.
        // The compassTask timer will update it after the first teleport.
    }

    /** Updates the recovery compass in slot 8 to point to the nearest enemy. */
    private void updateCompass(Player player) {
        Player nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Player other : getOnlinePlayers()) {
            if (other.equals(player)) continue;
            // Skip players in a different world (e.g. during start before TP)
            if (!other.getWorld().equals(player.getWorld())) continue;
            double dist = player.getLocation().distanceSquared(other.getLocation());
            if (dist < minDist) {
                minDist = dist;
                nearest = other;
            }
        }
        if (nearest == null) return;

        ItemStack item = player.getInventory().getItem(8);
        if (item == null || item.getType() != org.bukkit.Material.RECOVERY_COMPASS) return;

        // Attempt lodestone tracking — RECOVERY_COMPASS may not support CompassMeta, skip safely
        if (!(item.getItemMeta() instanceof org.bukkit.inventory.meta.CompassMeta meta)) return;
        meta.setLodestone(nearest.getLocation());
        meta.setLodestoneTracked(false);
        item.setItemMeta(meta);
        player.getInventory().setItem(8, item);
    }

    /** Respawns a player: instant TP + 5s protection + blue glow. */
    public void respawnPlayer(Player player) {
        GamePlayer gp = players.get(player.getUniqueId());
        if (gp == null) return;

        setupPlayer(player);
        teleportToRandomSpawn(player);

        // Apply respawn protection
        gp.setProtected(true);
        player.setInvulnerable(true);
        glowingUtil.addGlow(player);

        int protSeconds = plugin.getConfigManager().getRespawnProtectionSeconds();
        String protMsg = plugin.getConfigManager().getRawMessage("respawn-protected")
                .replace("<seconds>", String.valueOf(protSeconds));
        TextUtil.send(player, plugin.getConfigManager().getPrefix() + protMsg);

        if (plugin.getConfigManager().areSoundsEnabled()) {
            SoundUtil.play(player, plugin.getConfigManager().getSoundRespawn());
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !players.containsKey(player.getUniqueId())) return;
            gp.setProtected(false);
            player.setInvulnerable(false);
            glowingUtil.removeGlow(player);
            TextUtil.send(player, plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getRawMessage("respawn-protection-end"));
        }, protSeconds * 20L);

        eventManager.applyCurrentEventToPlayer(player);
    }

    /**
     * Called when a player gets a kill. Advances their level.
     */
    public void onKill(Player killer, Player victim) {
        if (ended) return;
        GamePlayer killerGp = players.get(killer.getUniqueId());
        GamePlayer victimGp = players.get(victim.getUniqueId());
        if (killerGp == null || victimGp == null) return;

        // DOUBLE_KILLS event: award 2 kills
        boolean doubleKill = eventManager.getCurrentEvent() == RandomEvent.DOUBLE_KILLS;
        int killsToAdd = doubleKill ? 2 : 1;

        for (int i = 0; i < killsToAdd; i++) killerGp.incrementKills();

        int oldLevel = killerGp.getLevel();
        killerGp.incrementLevel();
        if (doubleKill && killerGp.getLevel() < maxLevel) killerGp.incrementLevel();

        // Send kill messages
        String killMsg = plugin.getConfigManager().getRawMessage("kill-message")
                .replace("<victim>", victim.getName());
        TextUtil.send(killer, plugin.getConfigManager().getPrefix() + killMsg);

        String deathMsg = plugin.getConfigManager().getRawMessage("death-message")
                .replace("<killer>", killer.getName());
        TextUtil.send(victim, plugin.getConfigManager().getPrefix() + deathMsg);

        if (plugin.getConfigManager().areSoundsEnabled()) {
            SoundUtil.play(killer, plugin.getConfigManager().getSoundKill());
        }

        // Level up notification
        if (killerGp.getLevel() > oldLevel) {
            String levelMsg = plugin.getConfigManager().getRawMessage("level-up")
                    .replace("<level>", String.valueOf(killerGp.getLevel()))
                    .replace("<max>", String.valueOf(maxLevel));
            TextUtil.sendTitle(killer, levelMsg, "", 5, 30, 10);
            if (plugin.getConfigManager().areSoundsEnabled()) {
                SoundUtil.play(killer, plugin.getConfigManager().getSoundLevelUp());
            }
            giveWeapon(killer);
        }

        // Heal killer to full HP
        killer.setHealth(getMaxHealth(killer));

        // Check win condition
        if (killerGp.getLevel() > maxLevel) {
            endGame(killer);
            return;
        }

        // Respawn victim
        plugin.getServer().getScheduler().runTask(plugin, () -> respawnPlayer(victim));
    }

    /**
     * Called on void death — last attacker gets the level-up.
     */
    public void onVoidDeath(Player victim) {
        if (ended) return;
        GamePlayer victimGp = players.get(victim.getUniqueId());
        if (victimGp == null) return;

        String attackerName = victimGp.getLastAttackerName();
        if (attackerName != null) {
            Player attacker = plugin.getServer().getPlayerExact(attackerName);
            if (attacker != null && players.containsKey(attacker.getUniqueId())) {
                String voidMsg = plugin.getConfigManager().getRawMessage("void-kill")
                        .replace("<killer>", attackerName);
                TextUtil.send(attacker, plugin.getConfigManager().getPrefix() + voidMsg);
                onKill(attacker, victim);
                return;
            }
        }

        // No last attacker — just respawn
        plugin.getServer().getScheduler().runTask(plugin, () -> respawnPlayer(victim));
    }

    private void endGame(Player winner) {
        if (ended) return;
        ended = true;
        stopTimer();
        eventManager.stop();

        String winMsg = plugin.getConfigManager().getRawMessage("winner")
                .replace("<name>", winner.getName());

        for (Player p : getOnlinePlayers()) {
            TextUtil.sendTitle(p, winMsg, "", 10, 80, 20);
            if (plugin.getConfigManager().areSoundsEnabled()) {
                SoundUtil.play(p, plugin.getConfigManager().getSoundGameEnd());
            }
        }

        // Show top players GUI after 3s, TP to lobby after 8s
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            showTopPlayersGui();
        }, 60L);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            endCleanup();
            plugin.getGameManager().onArenaEnd(this);
        }, 160L);
    }

    private void endGameTimeUp() {
        if (ended) return;
        ended = true;
        stopTimer();
        eventManager.stop();

        // Find winner by highest level, then kills
        GamePlayer topPlayer = players.values().stream()
                .max(java.util.Comparator.comparingInt(GamePlayer::getLevel)
                        .thenComparingInt(GamePlayer::getKills))
                .orElse(null);

        String msg;
        if (topPlayer == null) {
            msg = plugin.getConfigManager().getRawMessage("tie");
        } else {
            // Check for tie
            long topCount = players.values().stream()
                    .filter(gp -> gp.getLevel() == topPlayer.getLevel() && gp.getKills() == topPlayer.getKills())
                    .count();
            if (topCount > 1) {
                msg = plugin.getConfigManager().getRawMessage("tie");
            } else {
                msg = plugin.getConfigManager().getRawMessage("winner")
                        .replace("<name>", topPlayer.getName());
            }
        }

        String timeUpMsg = plugin.getConfigManager().getRawMessage("time-up");
        for (Player p : getOnlinePlayers()) {
            TextUtil.sendTitle(p, timeUpMsg, msg, 10, 80, 20);
            if (plugin.getConfigManager().areSoundsEnabled()) {
                SoundUtil.play(p, plugin.getConfigManager().getSoundGameEnd());
            }
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            showTopPlayersGui();
        }, 60L);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            endCleanup();
            plugin.getGameManager().onArenaEnd(this);
        }, 160L);
    }

    private void showTopPlayersGui() {
        List<GamePlayer> sorted = players.values().stream()
                .sorted(java.util.Comparator.comparingInt(GamePlayer::getLevel)
                        .thenComparingInt(GamePlayer::getKills).reversed())
                .toList();
        for (Player p : getOnlinePlayers()) {
            new dev.zaen.betterGunGame.gui.TopPlayersGui(plugin, sorted).open(p);
        }
    }

    private void endCleanup() {
        glowingUtil.clearAll();
        bossbarManager.hideAll();

        Location lobby = plugin.getConfigManager().getLobbyLocation();
        for (Player p : getOnlinePlayers()) {
            p.getInventory().clear();
            p.setGameMode(GameMode.SURVIVAL);
            p.setInvulnerable(false);
            for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
            if (lobby != null) p.teleport(lobby);
        }
    }

    private void startTimer() {
        timerTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            timeRemaining--;
            if (timeRemaining <= 0) {
                endGameTimeUp();
            }
        }, 20L, 20L);
    }

    private void stopTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (bossbarTask != null) {
            bossbarTask.cancel();
            bossbarTask = null;
        }
        if (compassTask != null) {
            compassTask.cancel();
            compassTask = null;
        }
    }

    private void startBossbarUpdater() {
        bossbarTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            bossbarManager.updateAll(players.values(), getOnlinePlayers(), timeRemaining, maxLevel);
        }, 0L, 20L);
    }

    private void startCompassUpdater() {
        compassTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : getOnlinePlayers()) updateCompass(p);
        }, 0L, 40L);
    }

    private void teleportToRandomSpawn(Player player) {
        Location spawn = map.getRandomSpawn();
        player.teleport(spawn);
    }

    private double getMaxHealth(Player player) {
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }

    /** Sends glow state to a newly joining player (e.g. reconnect). */
    public void sendGlowStateTo(Player player) {
        glowingUtil.sendGlowState(player);
    }

    public List<Player> getOnlinePlayers() {
        List<Player> result = new ArrayList<>();
        for (UUID uuid : players.keySet()) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && p.isOnline()) result.add(p);
        }
        return result;
    }

    public GamePlayer getGamePlayer(UUID uuid) { return players.get(uuid); }
    public Collection<GamePlayer> getGamePlayers() { return players.values(); }

    public List<GamePlayer> getSortedPlayers() {
        return players.values().stream()
                .sorted(java.util.Comparator.comparingInt(GamePlayer::getLevel)
                        .thenComparingInt(GamePlayer::getKills).reversed())
                .toList();
    }
    public GameMap getMap() { return map; }
    public boolean isEnded() { return ended; }
    public boolean containsPlayer(UUID uuid) { return players.containsKey(uuid); }
    public dev.zaen.betterGunGame.event.RandomEventManager getEventManager() { return eventManager; }

    public void forceEnd() {
        ended = true;
        stopTimer();
        eventManager.stop();
        endCleanup();
    }
}
