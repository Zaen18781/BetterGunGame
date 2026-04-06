package dev.zaen.betterGunGame.game;

import dev.zaen.betterGunGame.BetterGunGame;
import dev.zaen.betterGunGame.arena.ArenaManager;
import dev.zaen.betterGunGame.util.SoundUtil;
import dev.zaen.betterGunGame.util.TextUtil;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {

    private final BetterGunGame plugin;
    private final ArenaManager arenaManager;

    private GameState state = GameState.IDLE;
    private final Set<UUID> joinedPlayers = ConcurrentHashMap.newKeySet();
    private int countdownTaskId = -1;

    public GameManager(BetterGunGame plugin) {
        this.plugin = plugin;
        this.arenaManager = new ArenaManager(plugin);
    }

    public boolean startGame(org.bukkit.command.CommandSender sender) {
        if (state != GameState.IDLE) {
            TextUtil.send((Player) sender, plugin.getConfigManager().getMessage("game-already-running"));
            return false;
        }

        int minPlayers = plugin.getConfigManager().getMinPlayers();
        if (joinedPlayers.size() < minPlayers) {
            String msg = plugin.getConfigManager().getMessage("not-enough-players")
                    .replace("<min>", String.valueOf(minPlayers));
            if (sender instanceof Player p) TextUtil.send(p, msg);
            else sender.sendMessage(msg);
            return false;
        }

        if (plugin.getMapManager().getMaps().isEmpty()) {
            String msg = plugin.getConfigManager().getMessage("no-maps");
            if (sender instanceof Player p) TextUtil.send(p, msg);
            else sender.sendMessage(msg);
            return false;
        }

        // Auto-add all online players — no /bgg join required
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            joinedPlayers.add(online.getUniqueId());
        }

        state = GameState.STARTING;
        startCountdown();
        return true;
    }

    private void startCountdown() {
        final int[] remaining = {plugin.getConfigManager().getCountdownSeconds()};

        countdownTaskId = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (remaining[0] > 0) {
                String countMsg = plugin.getConfigManager().getRawMessage("countdown")
                        .replace("<seconds>", String.valueOf(remaining[0]));
                broadcastTitle(countMsg, "", 5, 15, 5);
                if (plugin.getConfigManager().areSoundsEnabled()) {
                    SoundUtil.broadcast(plugin.getConfigManager().getSoundCountdownTick(), 1f,
                            0.5f + (float)(plugin.getConfigManager().getCountdownSeconds() - remaining[0]) * 0.1f);
                }
                remaining[0]--;
            } else {
                plugin.getServer().getScheduler().cancelTask(countdownTaskId);
                countdownTaskId = -1;
                launchGame();
            }
        }, 0L, 20L).getTaskId();
    }

    private void launchGame() {
        state = GameState.INGAME;

        List<Player> players = new ArrayList<>();
        for (UUID uuid : joinedPlayers) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && p.isOnline()) players.add(p);
        }

        arenaManager.createArenas(players).forEach(GameArena::start);

        String goMsg = plugin.getConfigManager().getRawMessage("countdown-go");
        broadcastTitle(goMsg, "", 5, 30, 10);
        if (plugin.getConfigManager().areSoundsEnabled()) {
            SoundUtil.broadcast(plugin.getConfigManager().getSoundCountdownGo());
        }
    }

    public boolean stopGame(org.bukkit.command.CommandSender sender) {
        if (state == GameState.IDLE) {
            String msg = plugin.getConfigManager().getMessage("game-not-running");
            if (sender instanceof Player p) TextUtil.send(p, msg);
            else sender.sendMessage(msg);
            return false;
        }

        if (countdownTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(countdownTaskId);
            countdownTaskId = -1;
        }

        arenaManager.forceEndAll();
        state = GameState.IDLE;
        joinedPlayers.clear();

        String msg = plugin.getConfigManager().getMessage("game-stopped");
        if (sender instanceof Player p) TextUtil.send(p, msg);
        else sender.sendMessage(msg);
        return true;
    }

    public boolean addPlayer(Player player) {
        if (state != GameState.IDLE && state != GameState.STARTING) return false;
        if (joinedPlayers.contains(player.getUniqueId())) return false;

        // Check max capacity
        int maxTotal = plugin.getMapManager().getMaps().size()
                * plugin.getConfigManager().getMaxPlayersPerMap();
        if (joinedPlayers.size() >= maxTotal) return false;

        joinedPlayers.add(player.getUniqueId());

        // Notify all
        String joinMsg = plugin.getConfigManager().getMessage("joined-game")
                .replace("<name>", player.getName());
        TextUtil.broadcast(joinMsg);

        TextUtil.send(player, plugin.getConfigManager().getMessage("player-added"));
        return true;
    }

    /** Called by a GameArena when it finishes. */
    public void onArenaEnd(GameArena arena) {
        arenaManager.removeArena(arena);
        if (arenaManager.getArenas().isEmpty()) {
            state = GameState.IDLE;
            joinedPlayers.clear();
        }
    }

    private void broadcastTitle(String title, String subtitle, int fi, int stay, int fo) {
        for (UUID uuid : joinedPlayers) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) TextUtil.sendTitle(p, title, subtitle, fi, stay, fo);
        }
    }

    public GameState getState() { return state; }
    public boolean isInGame() { return state != GameState.IDLE; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public Set<UUID> getJoinedPlayers() { return joinedPlayers; }
}
