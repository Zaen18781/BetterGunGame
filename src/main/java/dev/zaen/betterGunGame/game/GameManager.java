package dev.zaen.betterGunGame.game;

import dev.zaen.betterGunGame.BetterGunGame;
import dev.zaen.betterGunGame.arena.ArenaManager;
import dev.zaen.betterGunGame.map.GameMap;
import dev.zaen.betterGunGame.util.SoundUtil;
import dev.zaen.betterGunGame.util.TextUtil;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GameManager {

    private final BetterGunGame plugin;
    private final ArenaManager arenaManager;

    private GameState state = GameState.IDLE;
    private int countdownTaskId = -1;
    /** Set when /bgg start <map> or startgui is used; null = random selection. */
    private List<GameMap> pendingMapOrder = null;

    public GameManager(BetterGunGame plugin) {
        this.plugin = plugin;
        this.arenaManager = new ArenaManager(plugin);
    }

    public boolean startGame(org.bukkit.command.CommandSender sender) {
        return startGame(sender, null);
    }

    /** Starts the game. If forcedMaps is non-null, those maps (in order) override random selection. */
    public boolean startGame(org.bukkit.command.CommandSender sender, List<GameMap> forcedMaps) {
        if (state != GameState.IDLE) {
            if (sender instanceof Player p)
                TextUtil.send(p, plugin.getConfigManager().getMessage("game-already-running"));
            else sender.sendMessage(plugin.getConfigManager().getMessage("game-already-running"));
            return false;
        }

        if (plugin.getMapManager().getMaps().isEmpty()) {
            String msg = plugin.getConfigManager().getMessage("no-maps");
            if (sender instanceof Player p) TextUtil.send(p, msg);
            else sender.sendMessage(msg);
            return false;
        }

        pendingMapOrder = forcedMaps;
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

        // All online players at launch time — includes anyone who joined during countdown
        List<Player> players = new ArrayList<>(plugin.getServer().getOnlinePlayers());

        List<GameArena> arenas;
        if (pendingMapOrder != null) {
            arenas = arenaManager.createArenas(players, pendingMapOrder);
            pendingMapOrder = null;
        } else {
            arenas = arenaManager.createArenas(players);
        }
        arenas.forEach(GameArena::start);

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

        String msg = plugin.getConfigManager().getMessage("game-stopped");
        if (sender instanceof Player p) TextUtil.send(p, msg);
        else sender.sendMessage(msg);
        return true;
    }

    /** Called by a GameArena when it finishes. */
    public void onArenaEnd(GameArena arena) {
        arenaManager.removeArena(arena);
        if (arenaManager.getArenas().isEmpty()) {
            state = GameState.IDLE;
        }
    }

    private void broadcastTitle(String title, String subtitle, int fi, int stay, int fo) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            TextUtil.sendTitle(p, title, subtitle, fi, stay, fo);
        }
    }

    public GameState getState() { return state; }
    public boolean isInGame() { return state != GameState.IDLE; }
    public ArenaManager getArenaManager() { return arenaManager; }
}
