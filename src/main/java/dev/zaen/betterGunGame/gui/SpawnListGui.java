package dev.zaen.betterGunGame.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.PaginatedGui;
import dev.zaen.betterGunGame.BetterGunGame;
import dev.zaen.betterGunGame.map.GameMap;
import dev.zaen.betterGunGame.util.ItemUtil;
import dev.zaen.betterGunGame.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class SpawnListGui {

    private final BetterGunGame plugin;
    private final GameMap map;

    public SpawnListGui(BetterGunGame plugin, GameMap map) {
        this.plugin = plugin;
        this.map = map;
    }

    public void open(Player viewer) {
        List<Location> spawns = map.getSpawns();

        PaginatedGui gui = dev.triumphteam.gui.guis.Gui.paginated()
                .title(TextUtil.parse(
                        "<gradient:#e63278:#fd8ddb:#e63278><b>sᴘᴀᴡɴ-üʙᴇʀsɪᴄʜᴛ</b></gradient>"
                        + " <dark_grey>|</dark_grey> <#478ED2>" + map.getName() + "</#478ED2>"))
                .rows(6)
                .pageSize(45)
                .disableAllInteractions()
                .create();

        applyNavigation(gui, viewer);

        if (spawns.isEmpty()) {
            gui.setItem(3, 5, ItemBuilder.from(
                    ItemUtil.builder(Material.BARRIER)
                            .name("<#c0392b>ᴋᴇɪɴᴇ sᴘᴀᴡɴs ɢᴇꜰᴜɴᴅᴇɴ!")
                            .lore(
                                    "",
                                    "<grey>sᴇᴛᴢᴇ sᴘᴀᴡɴs ᴍɪᴛ:</grey>",
                                    "<#478ED2>/bgg mapsetup setspawn <1-16></#478ED2>",
                                    "<grey>ᴏᴅᴇʀ ᴘʟᴀᴛᴢɪᴇʀᴇ ɢᴏʟᴅ-Blöᴄᴋᴇ</grey>",
                                    "")
                            .build())
                    .asGuiItem());
        } else {
            for (int i = 0; i < spawns.size(); i++) {
                Location loc = spawns.get(i);
                int idx = i + 1;
                gui.addItem(ItemBuilder.from(
                        ItemUtil.builder(Material.GOLD_BLOCK)
                                .name("<gradient:#ffd700:#ff8c00><b>sᴘᴀᴡɴ #" + idx + "</b></gradient>")
                                .lore(
                                        "",
                                        String.format("<grey><#478ED2>●</#478ED2> X: <white>%.1f</white>", loc.getX()),
                                        String.format("<grey><#478ED2>●</#478ED2> Y: <white>%.1f</white>", loc.getY()),
                                        String.format("<grey><#478ED2>●</#478ED2> Z: <white>%.1f</white>", loc.getZ()),
                                        String.format("<grey><#478ED2>●</#478ED2> ʏᴀᴡ: <white>%.1f</white>"
                                                + "  ᴘɪᴛᴄʜ: <white>%.1f</white>",
                                                loc.getYaw(), loc.getPitch()),
                                        "",
                                        "<#478ED2>ᴋʟɪᴄᴋᴇɴ ᴜᴍ ᴢᴜ ᴛᴇʟᴇᴘᴏʀᴛɪᴇʀᴇɴ</#478ED2>",
                                        "")
                                .build())
                        .asGuiItem(e -> {
                            viewer.closeInventory();
                            viewer.teleport(loc);
                            TextUtil.send(viewer, plugin.getConfigManager().getPrefix()
                                    + "<grey>ᴛᴇʟᴇᴘᴏʀᴛɪᴇʀᴛ ᴢᴜ sᴘᴀᴡɴ <white>#" + idx + "</white></grey>");
                        }));
            }
        }

        gui.open(viewer);
    }

    private void applyNavigation(PaginatedGui gui, Player viewer) {
        // Bottom row — all glass
        for (int slot = 45; slot <= 53; slot++) {
            gui.setItem(slot, ItemBuilder.from(
                    ItemUtil.filler(Material.GRAY_STAINED_GLASS_PANE)).asGuiItem());
        }

        // Prev — slot 48, info — slot 49, next — slot 50
        gui.setItem(48, MapOverviewGui.buildNavItem(gui, true)
                .asGuiItem(e -> { gui.previous(); refreshNav(gui, viewer); }));
        gui.setItem(49, MapOverviewGui.buildPageInfo(gui));
        gui.setItem(50, MapOverviewGui.buildNavItem(gui, false)
                .asGuiItem(e -> { gui.next(); refreshNav(gui, viewer); }));

        // Back — slot 53 (right end of bottom row)
        gui.setItem(53, ItemBuilder.from(
                ItemUtil.builder(Material.ARROW)
                        .name("<grey>◀ ᴢᴜʀüᴄᴋ</grey>")
                        .lore("", "<grey>ᴢᴜʀüᴄᴋ ᴢᴜʀ ᴍᴀᴘ-üʙᴇʀsɪᴄʜᴛ</grey>", "")
                        .build())
                .asGuiItem(e -> new MapDetailGui(plugin, map).open(viewer)));
    }

    private void refreshNav(PaginatedGui gui, Player viewer) {
        gui.updateItem(48, MapOverviewGui.buildNavItem(gui, true)
                .asGuiItem(e -> { gui.previous(); refreshNav(gui, viewer); }));
        gui.updateItem(49, MapOverviewGui.buildPageInfo(gui));
        gui.updateItem(50, MapOverviewGui.buildNavItem(gui, false)
                .asGuiItem(e -> { gui.next(); refreshNav(gui, viewer); }));
    }
}
