package dev.zaen.betterGunGame.event;

import dev.zaen.betterGunGame.BetterGunGame;
import dev.zaen.betterGunGame.game.GameArena;
import dev.zaen.betterGunGame.util.SoundUtil;
import dev.zaen.betterGunGame.util.TextUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Random;

public class RandomEventManager {

    private final BetterGunGame plugin;
    private final GameArena arena;
    private final Random random = new Random();

    private RandomEvent currentEvent = null;
    private int eventTaskId = -1;

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
            if (plugin.getConfigManager().areSoundsEnabled()) {
                SoundUtil.play(player, plugin.getConfigManager().getSoundEventStart());
            }
        }

        applyEvent(currentEvent);

        int durationTicks = plugin.getConfigManager().getEventDurationSeconds() * 20;
        plugin.getServer().getScheduler().runTaskLater(plugin, this::endCurrentEvent, durationTicks);
    }

    private void applyEvent(RandomEvent event) {
        List<Player> players = arena.getOnlinePlayers();
        int durationTicks = plugin.getConfigManager().getEventDurationSeconds() * 20;

        switch (event) {
            case SPEED_BOOST -> applyEffect(players, PotionEffectType.SPEED, durationTicks, 1);
            case LOW_GRAVITY -> applyEffect(players, PotionEffectType.SLOW_FALLING, durationTicks, 0);
            case BLINDNESS -> applyEffect(players, PotionEffectType.BLINDNESS, durationTicks, 0);
            case INVISIBILITY -> applyEffect(players, PotionEffectType.INVISIBILITY, durationTicks, 0);
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
        List<Player> players = arena.getOnlinePlayers();

        // Remove effects we applied
        switch (currentEvent) {
            case SPEED_BOOST -> players.forEach(p -> p.removePotionEffect(PotionEffectType.SPEED));
            case LOW_GRAVITY -> players.forEach(p -> p.removePotionEffect(PotionEffectType.SLOW_FALLING));
            case BLINDNESS -> players.forEach(p -> p.removePotionEffect(PotionEffectType.BLINDNESS));
            case INVISIBILITY -> players.forEach(p -> p.removePotionEffect(PotionEffectType.INVISIBILITY));
            default -> {}
        }

        for (Player player : players) {
            TextUtil.send(player, plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getRawMessage("event-end"));
        }

        currentEvent = null;
    }

    /** Apply event to a newly respawned player. */
    public void applyCurrentEventToPlayer(Player player) {
        if (currentEvent == null) return;
        int durationTicks = plugin.getConfigManager().getEventDurationSeconds() * 20;
        switch (currentEvent) {
            case SPEED_BOOST -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, 1, true, false));
            case LOW_GRAVITY -> player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, durationTicks, 0, true, false));
            case BLINDNESS -> player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, durationTicks, 0, true, false));
            case INVISIBILITY -> player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, durationTicks, 0, true, false));
            default -> {}
        }
    }

    public RandomEvent getCurrentEvent() { return currentEvent; }
}
