package dev.zaen.betterGunGame.listener;

import dev.zaen.betterGunGame.BetterGunGame;
import dev.zaen.betterGunGame.arena.ArenaManager;
import dev.zaen.betterGunGame.event.RandomEvent;
import dev.zaen.betterGunGame.game.GameArena;
import dev.zaen.betterGunGame.game.GamePlayer;
import dev.zaen.betterGunGame.game.GameState;
import dev.zaen.betterGunGame.gui.PlayerOverviewGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

public class GameListener implements Listener {

    private final BetterGunGame plugin;

    public GameListener(BetterGunGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        ArenaManager am = plugin.getGameManager().getArenaManager();
        GameArena arena = am.getArenaForPlayer(victim.getUniqueId());
        if (arena == null) return;

        // Track last attacker for void kills
        GamePlayer victimGp = arena.getGamePlayer(victim.getUniqueId());
        if (victimGp != null) victimGp.setLastAttackerName(attacker.getName());

        // ONE_SHOT event
        GamePlayer attackerGp = arena.getGamePlayer(attacker.getUniqueId());
        if (attackerGp != null
                && arena.getEventManager().getCurrentEvent() == RandomEvent.ONE_SHOT) {
            event.setDamage(100.0);
        }

        // Block damage on protected players
        if (victimGp != null && victimGp.isProtected()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        ArenaManager am = plugin.getGameManager().getArenaManager();
        GameArena arena = am.getArenaForPlayer(victim.getUniqueId());
        if (arena == null) return;

        event.setCancelled(true); // No death screen
        event.setDroppedExp(0);
        event.getDrops().clear();

        Player killer = victim.getKiller();
        if (killer != null && arena.containsPlayer(killer.getUniqueId())) {
            arena.onKill(killer, victim);
        } else {
            // No killer or killer not in arena — just respawn
            plugin.getServer().getScheduler().runTask(plugin, () -> arena.respawnPlayer(victim));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVoidFall(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) return;

        ArenaManager am = plugin.getGameManager().getArenaManager();
        GameArena arena = am.getArenaForPlayer(player.getUniqueId());
        if (arena == null) return;

        event.setCancelled(true);
        arena.onVoidDeath(player);
    }

    @EventHandler
    public void onCompassClick(PlayerInteractEvent event) {
        // Only main hand, only right-click
        if (event.getHand() != EquipmentSlot.HAND) return;
        var action = event.getAction();
        if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (player.getInventory().getHeldItemSlot() != 8) return;
        var item = player.getInventory().getItem(8);
        if (item == null || item.getType() != Material.RECOVERY_COMPASS) return;

        ArenaManager am = plugin.getGameManager().getArenaManager();
        GameArena arena = am.getArenaForPlayer(player.getUniqueId());
        if (arena == null) return;

        event.setCancelled(true);
        new PlayerOverviewGui(plugin, arena).open(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // If game is running and player was in an arena, send glow state
        if (plugin.getGameManager().getState() == GameState.INGAME) {
            GameArena arena = plugin.getGameManager().getArenaManager()
                    .getArenaForPlayer(event.getPlayer().getUniqueId());
            if (arena != null) {
                arena.sendGlowStateTo(event.getPlayer());
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        arena.respawnPlayer(event.getPlayer()), 5L);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Player leaving mid-game — just remove them from the arena's player list
        // (They'll simply be absent when online player list is fetched)
    }

    /**
     * Tracks thrown tridents so they can be reliably removed even if they land in unloaded chunks.
     * Without tracking, a Loyalty trident in an unloaded chunk is missed by getEntitiesByClass(),
     * causing it to orbit the player indefinitely when that chunk later loads.
     */
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof Player player)) return;

        GameArena arena = plugin.getGameManager().getArenaManager().getArenaForPlayer(player.getUniqueId());
        if (arena == null) return;

        arena.trackTrident(player, trident);
    }

}

