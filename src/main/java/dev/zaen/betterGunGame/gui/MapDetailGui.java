package dev.zaen.betterGunGame.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.zaen.betterGunGame.BetterGunGame;
import dev.zaen.betterGunGame.map.GameMap;
import dev.zaen.betterGunGame.util.ItemUtil;
import dev.zaen.betterGunGame.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class MapDetailGui {

    private final BetterGunGame plugin;
    private final GameMap map;

    public MapDetailGui(BetterGunGame plugin, GameMap map) {
        this.plugin = plugin;
        this.map = map;
    }

    public void open(Player viewer) {
        Gui gui = Gui.gui()
                .title(TextUtil.parse(
                        "<gradient:#e63278:#fd8ddb:#e63278><b>ᴍᴀᴘ-ᴅᴇᴛᴀɪʟs</b></gradient>"
                        + " <dark_grey>|</dark_grey> <#478ED2>" + map.getName() + "</#478ED2>"))
                .rows(3)
                .disableAllInteractions()
                .create();

        // Bottom row glass (slots 18-26)
        for (int slot = 18; slot <= 26; slot++) {
            gui.setItem(slot, ItemBuilder.from(
                    ItemUtil.filler(Material.GRAY_STAINED_GLASS_PANE)).asGuiItem());
        }

        // Map info — row 2, col 5
        gui.setItem(2, 5, ItemBuilder.from(
                ItemUtil.builder(Material.FILLED_MAP)
                        .name("<gradient:#e63278:#fd8ddb><b>" + map.getName() + "</b></gradient>")
                        .lore(
                                "",
                                "<grey><#478ED2>●</#478ED2> ᴡᴇʟᴛ: <white>" + map.getWorld().getName() + "</white></grey>",
                                "<grey><#478ED2>●</#478ED2> sᴘᴀᴡɴs: <white>" + map.getSpawns().size() + "</white></grey>",
                                "<grey><#478ED2>●</#478ED2> ᴍᴀx. sᴘɪᴇʟᴇʀ: <white>" + map.getMaxPlayers() + "</white></grey>",
                                "")
                        .build())
                .asGuiItem());

        // Spawns — row 2, col 3
        gui.setItem(2, 3, ItemBuilder.from(
                ItemUtil.builder(Material.GOLD_BLOCK)
                        .name("<gradient:#ffd700:#ff8c00><b>sᴘᴀᴡɴs ᴀɴᴢᴇɪɢᴇɴ</b></gradient>")
                        .lore(
                                "",
                                "<grey><#478ED2>●</#478ED2> ɢᴇsᴘᴇɪᴄʜᴇʀᴛ: <white>" + map.getSpawns().size() + "</white></grey>",
                                "",
                                "<#478ED2>ᴋʟɪᴄᴋᴇɴ ᴜᴍ ᴢᴜ öꜰꜰɴᴇɴ</#478ED2>",
                                "")
                        .build())
                .asGuiItem(e -> new SpawnListGui(plugin, map).open(viewer)));

        // TP — row 2, col 7
        gui.setItem(2, 7, ItemBuilder.from(
                ItemUtil.builder(Material.ENDER_PEARL)
                        .name("<#478ED2><b>ᴢᴜʀ ᴍᴀᴘ ᴛᴇʟᴇᴘᴏʀᴛɪᴇʀᴇɴ</#478ED2>")
                        .lore(
                                "",
                                "<grey><#478ED2>●</#478ED2> ᴛᴘ ᴢᴜᴍ ᴡᴇʟᴛ-sᴘᴀᴡɴ</grey>",
                                "",
                                "<#478ED2>ᴋʟɪᴄᴋᴇɴ ᴜᴍ ᴢᴜ ᴛᴇʟᴇᴘᴏʀᴛɪᴇʀᴇɴ</#478ED2>",
                                "")
                        .build())
                .asGuiItem(e -> {
                    viewer.closeInventory();
                    Location spawn = map.getWorld().getSpawnLocation();
                    viewer.teleport(spawn);
                    TextUtil.send(viewer, plugin.getConfigManager().getPrefix()
                            + "<grey>ᴛᴇʟᴇᴘᴏʀᴛɪᴇʀᴛ ᴢᴜ <white>" + map.getName() + "</white></grey>");
                }));

        // Back — slot 18 (row 3, col 1)
        gui.setItem(18, ItemBuilder.from(
                ItemUtil.builder(Material.ARROW)
                        .name("<grey>◀ ᴢᴜʀüᴄᴋ</grey>")
                        .lore("", "<grey>ᴢᴜʀüᴄᴋ ᴢᴜʀ ᴍᴀᴘ-üʙᴇʀsɪᴄʜᴛ</grey>", "")
                        .build())
                .asGuiItem(e -> new MapOverviewGui(plugin).open(viewer)));

        gui.open(viewer);
    }
}
