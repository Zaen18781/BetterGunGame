package dev.zaen.betterGunGame.command;

import dev.zaen.betterGunGame.BetterGunGame;
import dev.zaen.betterGunGame.game.GameState;
import dev.zaen.betterGunGame.gui.GameManagementGui;
import dev.zaen.betterGunGame.gui.MapOverviewGui;
import dev.zaen.betterGunGame.map.GameMap;
import dev.zaen.betterGunGame.map.MapManager;
import dev.zaen.betterGunGame.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GunGameCommand implements CommandExecutor, TabCompleter {

    private final BetterGunGame plugin;

    public GunGameCommand(BetterGunGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "start" -> {
                if (!hasPermission(sender)) return true;
                plugin.getGameManager().startGame(sender);
            }

            case "stop" -> {
                if (!hasPermission(sender)) return true;
                plugin.getGameManager().stopGame(sender);
            }

            case "join" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
                    return true;
                }
                if (plugin.getGameManager().getState() == GameState.INGAME) {
                    TextUtil.send(player, plugin.getConfigManager().getMessage("game-already-running"));
                    return true;
                }
                plugin.getGameManager().addPlayer(player);
            }

            case "setlobby" -> {
                if (!hasPermission(sender)) return true;
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
                    return true;
                }
                plugin.getConfigManager().setLobbyLocation(player.getLocation());
                TextUtil.send(player, plugin.getConfigManager().getMessage("lobby-set"));
            }

            case "mapoverview" -> {
                if (!hasPermission(sender)) return true;
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
                    return true;
                }
                new MapOverviewGui(plugin).open(player);
            }

            case "mapselect" -> {
                if (!hasPermission(sender)) return true;
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
                    return true;
                }
                handleMapSelect(player, args);
            }

            case "mapsetup" -> {
                if (!hasPermission(sender)) return true;
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
                    return true;
                }
                handleMapSetup(player, args);
            }

            case "manage", "admin" -> {
                if (!hasPermission(sender)) return true;
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
                    return true;
                }
                new GameManagementGui(plugin).open(player);
            }

            case "reload" -> {
                if (!hasPermission(sender)) return true;
                plugin.getConfigManager().load();
                plugin.getLevelManager().load();
                plugin.getMapManager().reloadSpawns();
                String msg = plugin.getConfigManager().getMessage("reload");
                if (sender instanceof Player p) TextUtil.send(p, msg);
                else sender.sendMessage(msg);
            }

            case "rescanspawns" -> {
                if (!hasPermission(sender)) return true;
                plugin.getMapManager().reloadSpawns();
                String prefix = plugin.getConfigManager().getPrefix();
                if (sender instanceof Player p) {
                    TextUtil.send(p, prefix + "<green>Spawns neu gescannt:</green>");
                    for (dev.zaen.betterGunGame.map.GameMap map : plugin.getMapManager().getMaps()) {
                        TextUtil.send(p, prefix + " <color:#08a8f8>●</color> <white>" + map.getName()
                                + "</white> <gray>→</gray> <white>" + map.getSpawns().size() + "</white> <gray>Spawns</gray>");
                    }
                } else {
                    sender.sendMessage("Spawns neu gescannt:");
                    for (dev.zaen.betterGunGame.map.GameMap map : plugin.getMapManager().getMaps()) {
                        sender.sendMessage("  " + map.getName() + " → " + map.getSpawns().size() + " Spawns");
                    }
                }
            }

            default -> sendHelp(sender);
        }

        return true;
    }

    // ---- /bgg mapselect <mapname> ----

    private void handleMapSelect(Player player, String[] args) {
        if (args.length < 2) {
            TextUtil.send(player, plugin.getConfigManager().getPrefix()
                    + "<red>Verwendung: /bgg mapselect <mapname></red>");
            return;
        }
        String mapName = args[1];
        GameMap map = plugin.getMapManager().getMap(mapName);
        if (map == null) {
            TextUtil.send(player, plugin.getConfigManager().getPrefix()
                    + "<red>Map nicht gefunden: <white>" + mapName + "</white></red>");
            return;
        }
        player.teleport(map.getWorld().getSpawnLocation());
        TextUtil.send(player, plugin.getConfigManager().getPrefix()
                + "<gray>Teleportiert zu <white>" + mapName + "</white></gray>");
    }

    // ---- /bgg mapsetup setspawn <1-16> ----

    private void handleMapSetup(Player player, String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("setspawn")) {
            TextUtil.send(player, plugin.getConfigManager().getPrefix()
                    + "<red>Verwendung: /bgg mapsetup setspawn <1-16></red>");
            return;
        }

        // Player must be in a bgg_ world
        if (!MapManager.isBggWorld(player.getWorld())) {
            TextUtil.send(player, plugin.getConfigManager().getPrefix()
                    + "<red>Du musst dich in einer GunGame-Map befinden!</red>");
            return;
        }

        int index;
        try {
            index = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            TextUtil.send(player, plugin.getConfigManager().getPrefix()
                    + "<red>Ungültige Nummer. Verwende 1–16.</red>");
            return;
        }

        if (index < 1 || index > 16) {
            TextUtil.send(player, plugin.getConfigManager().getPrefix()
                    + "<red>Spawn-Index muss zwischen 1 und 16 liegen.</red>");
            return;
        }

        String mapName = MapManager.mapNameFromWorld(player.getWorld().getName());
        plugin.getMapManager().getSetupManager().setSpawn(mapName, index, player.getLocation());

        // Also update the in-memory map spawn list
        plugin.getMapManager().reloadSpawns();

        // Chat message (BetterCore style with click-to-copy coords)
        String x = String.format("%.2f", player.getLocation().getX());
        String y = String.format("%.2f", player.getLocation().getY());
        String z = String.format("%.2f", player.getLocation().getZ());
        float yaw   = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();

        TextUtil.send(player, plugin.getConfigManager().getPrefix()
                + "<green>✔ Spawn <white>#" + index + "</white> gesetzt!</green>");
        TextUtil.send(player, "<dark_gray>  X: <gray>" + x
                + "</gray>  Y: <gray>" + y
                + "</gray>  Z: <gray>" + z
                + "</gray>  Yaw: <gray>" + String.format("%.1f", yaw)
                + "</gray>  Pitch: <gray>" + String.format("%.1f", pitch) + "</gray></dark_gray>");
        TextUtil.send(player, "<dark_gray>  Map: <gray>" + mapName + "</gray></dark_gray>");
    }

    // ---- Helpers ----

    private boolean hasPermission(CommandSender sender) {
        if (sender.hasPermission("bettergungame.admin")) return true;
        String msg = plugin.getConfigManager().getMessage("no-permission");
        if (sender instanceof Player p) TextUtil.send(p, msg);
        else sender.sendMessage(msg);
        return false;
    }

    private void sendHelp(CommandSender sender) {
        String prefix = plugin.getConfigManager().getPrefix();
        List<String> lines = List.of(
                "<gray>/bgg <white>start</white> — Spiel starten",
                "<gray>/bgg <white>stop</white> — Spiel stoppen",
                "<gray>/bgg <white>join</white> — Spiel beitreten",
                "<gray>/bgg <white>setlobby</white> — Lobby setzen",
                "<gray>/bgg <white>mapoverview</white> — Map-Übersicht",
                "<gray>/bgg <white>mapselect <name></white> — Zur Map teleportieren",
                "<gray>/bgg <white>mapsetup setspawn <1-16></white> — Spawn setzen",
                "<gray>/bgg <white>manage</white> — Verwaltungs-GUI",
                "<gray>/bgg <white>reload</white> — Config neu laden",
                "<gray>/bgg <white>rescanspawns</white> — Maps auf Gold-Blöcke neu scannen"
        );
        if (sender instanceof Player p) {
            lines.forEach(l -> TextUtil.send(p, prefix + l));
        } else {
            lines.forEach(sender::sendMessage);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("bettergungame.admin")) return List.of();

        if (args.length == 1) {
            return Arrays.asList("start", "stop", "join", "setlobby",
                    "mapoverview", "mapselect", "mapsetup", "manage", "reload", "rescanspawns");
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "mapselect" -> plugin.getMapManager().getMaps().stream()
                        .map(GameMap::getName).collect(Collectors.toList());
                case "mapsetup" -> List.of("setspawn");
                default -> List.of();
            };
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("mapsetup")
                && args[1].equalsIgnoreCase("setspawn")) {
            return IntStream.rangeClosed(1, 16)
                    .mapToObj(Integer::toString)
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
