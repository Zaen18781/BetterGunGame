package dev.zaen.betterGunGame.event;

import dev.zaen.betterGunGame.BetterGunGame;
import dev.zaen.betterGunGame.game.GameArena;
import dev.zaen.betterGunGame.util.SoundUtil;
import dev.zaen.betterGunGame.util.TextUtil;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Random;

public class RandomEventManager {

    private final BetterGunGame plugin;
    private final GameArena arena;
    private final Random random = new Random();

    private RandomEvent currentEvent = null;
    private int eventTaskId = -1;

    /** Hotbar countdown state */
    private BukkitTask countdownTask = null;
    private int eventSecondsRemaining = 0;

    public RandomEventManager(BetterGunGame plugin, GameArena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    public void startScheduler() {
        int intervalTicks = plugin.getConfigManager().getEventIntervalSeconds() * 20;
        eventTaskId = plugin.getServer().getScheduler().runTaskTimer(
                plugin, this::triggerRandomEvent, intervalTicks, intervalTicks
        ).getTaskId();
    }

    public void stop() {
        if (eventTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(eventTaskId);
            eventTaskId = -1;
        }
        endCurrentEvent();
    }

    private void triggerRandomEvent() {
        if (!plugin.getConfigManager().areRandomEventsEnabled()) return;
        endCurrentEvent();

        RandomEvent[] events = RandomEvent.values();
        currentEvent = events[random.nextInt(events.length)];

        String msg = plugin.getConfigManager().getRawMessage("event-announce")
                .replace("<event>", currentEvent.getDisplayName());

        for (Player player : arena.getOnlinePlayers()) {
            TextUtil.send(player, plugin.getConfigManager().getPrefix() + msg);
            TextUtil.sendTitle(player,
                    "<gradient:#ff8800:#ffff00><b>" + currentEvent.getDisplayName() + "</b></gradient>",
                    "<gray>" + currentEvent.getDescription() + "</gray>",
                    5, 40, 10);
            if (plugin.getConfigManager().areSoundsEnabled()) {
                SoundUtil.play(player, plugin.getConfigManager().getSoundEventStart());
            }
        }

        applyEvent(currentEvent);

        int durationSeconds = plugin.getConfigManager().getEventDurationSeconds();
        eventSecondsRemaining = durationSeconds;

        // Schedule end
        plugin.getServer().getScheduler().runTaskLater(plugin, this::endCurrentEvent,
                (long) durationSeconds * 20);

        // Start hotbar countdown (fires every second, starts immediately)
        startCountdown();
    }

    private void startCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }

        countdownTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (currentEvent == null || eventSecondsRemaining <= 0) {
                stopCountdown(true);
                return;
            }
            String bar = "<gradient:#ff8800:#ffff00>⚡ "
                    + currentEvent.getDisplayName()
                    + "</gradient> <dark_gray>—</dark_gray> <yellow><b>"
                    + eventSecondsRemaining
                    + "s</b></yellow>";
            for (Player p : arena.getOnlinePlayers()) {
                TextUtil.sendActionBar(p, bar);
            }
            eventSecondsRemaining--;
        }, 0L, 20L);
    }

    private void stopCountdown(boolean clearBar) {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        eventSecondsRemaining = 0;
        if (clearBar) {
            // Clear the action bar for all players
            for (Player p : arena.getOnlinePlayers()) {
                TextUtil.sendActionBar(p, "");
            }
        }
    }

    private void applyEvent(RandomEvent event) {
        List<Player> players = arena.getOnlinePlayers();
        int durationTicks = plugin.getConfigManager().getEventDurationSeconds() * 20;

        switch (event) {
            case SPEED_BOOST  -> applyEffect(players, PotionEffectType.SPEED, durationTicks, 1);
            case LOW_GRAVITY  -> applyEffect(players, PotionEffectType.SLOW_FALLING, durationTicks, 0);
            case BLINDNESS    -> applyEffect(players, PotionEffectType.BLINDNESS, durationTicks, 0);
            case INVISIBILITY -> applyEffect(players, PotionEffectType.INVISIBILITY, durationTicks, 0);
            case STRENGTH     -> applyEffect(players, PotionEffectType.STRENGTH, durationTicks, 1);
            case JUMP_BOOST   -> applyEffect(players, PotionEffectType.JUMP_BOOST, durationTicks, 3);
            case SLOWNESS     -> applyEffect(players, PotionEffectType.SLOWNESS, durationTicks, 2);
            case DOUBLE_KILLS, ONE_SHOT -> { /* Handled in kill logic via getCurrentEvent() */ }
        }
    }

    private void applyEffect(List<Player> players, PotionEffectType type, int durationTicks, int amplifier) {
        for (Player player : players) {
            player.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, true, false));
        }
    }

    private void endCurrentEvent() {
        if (currentEvent == null) return;
        stopCountdown(true);

        List<Player> players = arena.getOnlinePlayers();

        // Remove effects we applied
        switch (currentEvent) {
            case SPEED_BOOST  -> players.forEach(p -> p.removePotionEffect(PotionEffectType.SPEED));
            case LOW_GRAVITY  -> players.forEach(p -> p.removePotionEffect(PotionEffectType.SLOW_FALLING));
            case BLINDNESS    -> players.forEach(p -> p.removePotionEffect(PotionEffectType.BLINDNESS));
            case INVISIBILITY -> players.forEach(p -> p.removePotionEffect(PotionEffectType.INVISIBILITY));
            case STRENGTH     -> players.forEach(p -> p.removePotionEffect(PotionEffectType.STRENGTH));
            case JUMP_BOOST   -> players.forEach(p -> p.removePotionEffect(PotionEffectType.JUMP_BOOST));
            case SLOWNESS     -> players.forEach(p -> p.removePotionEffect(PotionEffectType.SLOWNESS));
            default -> {}
        }

        for (Player player : players) {
            TextUtil.send(player, plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getRawMessage("event-end"));
        }

        currentEvent = null;
    }

    /** Apply the current event's effect to a newly respawned player, with remaining duration. */
    public void applyCurrentEventToPlayer(Player player) {
        if (currentEvent == null) return;
        // Use remaining seconds so the effect matches what's left of the event
        int remainingTicks = Math.max(1, eventSecondsRemaining * 20);
        switch (currentEvent) {
            case SPEED_BOOST  -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,       remainingTicks, 1, true, false));
            case LOW_GRAVITY  -> player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, remainingTicks, 0, true, false));
            case BLINDNESS    -> player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,    remainingTicks, 0, true, false));
            case INVISIBILITY -> player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, remainingTicks, 0, true, false));
            case STRENGTH     -> player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,     remainingTicks, 1, true, false));
            case JUMP_BOOST   -> player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,   remainingTicks, 3, true, false));
            case SLOWNESS     -> player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,     remainingTicks, 2, true, false));
            default -> {}
        }
    }

    public RandomEvent getCurrentEvent() { return currentEvent; }
}
