package dev.zaen.betterGunGame.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import dev.zaen.betterGunGame.BetterGunGame;
import dev.zaen.betterGunGame.map.GameMap;
import dev.zaen.betterGunGame.util.ItemUtil;
import dev.zaen.betterGunGame.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class MapOverviewGui {

    private final BetterGunGame plugin;

    public MapOverviewGui(BetterGunGame plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer) {
        PaginatedGui gui = dev.triumphteam.gui.guis.Gui.paginated()
                .title(TextUtil.parse(
                        "<gradient:#e63278:#fd8ddb:#e63278><b>ᴍᴀᴘ-üʙᴇʀsɪᴄʜᴛ</b></gradient>"))
                .rows(6)
                .pageSize(45)
                .disableAllInteractions()
                .create();

        applyNavigation(gui, viewer);

        List<GameMap> maps = plugin.getMapManager().getMaps();

        if (maps.isEmpty()) {
            gui.setItem(3, 5, ItemBuilder.from(
                    ItemUtil.builder(Material.BARRIER)
                            .name("<#c0392b>ᴋᴇɪɴᴇ ᴍᴀᴘs ɢᴇꜰᴜɴᴅᴇɴ!")
                            .lore(
                                    "",
                                    "<grey>ʟᴇɢᴇ ᴡᴏʀʟᴅ-ᴏʀᴅɴᴇʀ ɪɴ</grey>",
                                    "<grey>plugins/BetterGunGame/maps/</grey>",
                                    "")
                            .build())
                    .asGuiItem());
        } else {
            for (GameMap map : maps) {
                gui.addItem(buildMapItem(map, viewer));
            }
        }

        gui.open(viewer);
    }

    private GuiItem buildMapItem(GameMap map, Player viewer) {
        return ItemBuilder.from(
                ItemUtil.builder(Material.FILLED_MAP)
                        .name("<gradient:#e63278:#fd8ddb><b>" + map.getName() + "</b></gradient>")
                        .lore(
                                "",
                                "<grey><#478ED2>●</#478ED2> sᴘᴀᴡɴs: <white>" + map.getSpawns().size() + "</white></grey>",
                                "<grey><#478ED2>●</#478ED2> ᴡᴇʟᴛ: <white>" + map.getWorld().getName() + "</white></grey>",
                                "",
                                "<#478ED2>ᴋʟɪᴄᴋᴇɴ ᴜᴍ ᴅᴇᴛᴀɪʟs ᴢᴜ öꜰꜰɴᴇɴ</#478ED2>",
                                "")
                        .build())
                .asGuiItem(e -> new MapDetailGui(plugin, map).open(viewer));
    }

    // Slots 45-53 = bottom row of a 6-row GUI (0-indexed)
    static void applyNavigation(PaginatedGui gui, Player viewer) {
        // Bottom row — all glass
        for (int slot = 45; slot <= 53; slot++) {
            gui.setItem(slot, ItemBuilder.from(
                    ItemUtil.filler(Material.GRAY_STAINED_GLASS_PANE)).asGuiItem());
        }

        // Prev — slot 48, info — slot 49, next — slot 50
        gui.setItem(48, buildNavItem(gui, true)
                .asGuiItem(e -> { gui.previous(); refreshNav(gui, viewer); }));
        gui.setItem(49, buildPageInfo(gui));
        gui.setItem(50, buildNavItem(gui, false)
                .asGuiItem(e -> { gui.next(); refreshNav(gui, viewer); }));
    }

    static void refreshNav(PaginatedGui gui, Player viewer) {
        gui.updateItem(48, buildNavItem(gui, true)
                .asGuiItem(e -> { gui.previous(); refreshNav(gui, viewer); }));
        gui.updateItem(49, buildPageInfo(gui));
        gui.updateItem(50, buildNavItem(gui, false)
                .asGuiItem(e -> { gui.next(); refreshNav(gui, viewer); }));
    }

    static ItemBuilder buildNavItem(PaginatedGui gui, boolean prev) {
        boolean hasPrev = gui.getCurrentPageNum() > 1;
        boolean hasNext = gui.getCurrentPageNum() < gui.getPagesNum();
        boolean active  = prev ? hasPrev : hasNext;

        Material mat = active ? Material.LIME_CANDLE : Material.RED_CANDLE;
        String name  = prev
                ? (active ? "<#5ac46e>« ᴠᴏʀʜᴇʀɪɢᴇ sᴇɪᴛᴇ</#5ac46e>"
                          : "<#c0392b>« ᴠᴏʀʜᴇʀɪɢᴇ sᴇɪᴛᴇ</#c0392b>")
                : (active ? "<#5ac46e>ɴäᴄʜsᴛᴇ sᴇɪᴛᴇ »</#5ac46e>"
                          : "<#c0392b>ɴäᴄʜsᴛᴇ sᴇɪᴛᴇ »</#c0392b>");

        return ItemBuilder.from(
                ItemUtil.builder(mat).name(name)
                        .lore("", "<#478ED2>●</#478ED2> <grey>ᴋʟɪᴄᴋᴇ ᴢᴜᴍ ɴᴀᴠɪɢɪᴇʀᴇɴ</grey>", "")
                        .build());
    }

    static GuiItem buildPageInfo(PaginatedGui gui) {
        return ItemBuilder.from(
                ItemUtil.builder(Material.PAPER)
                        .name("<yellow>sᴇɪᴛᴇɴ-ɪɴꜰᴏ</yellow>")
                        .lore(
                                "",
                                "<#478ED2>● <grey>sᴇɪᴛᴇ </grey>"
                                        + gui.getCurrentPageNum()
                                        + "<grey> / </grey>"
                                        + gui.getPagesNum(),
                                "")
                        .build())
                .asGuiItem();
    }
}
