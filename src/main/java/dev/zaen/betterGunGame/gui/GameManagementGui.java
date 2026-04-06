package dev.zaen.betterGunGame.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.zaen.betterGunGame.BetterGunGame;
import dev.zaen.betterGunGame.util.ItemUtil;
import dev.zaen.betterGunGame.util.SkullUtil;
import dev.zaen.betterGunGame.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class GameManagementGui {

    private static final String TEXTURE_START =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0" +
            "L3RleHR1cmUvYTkyZTMxZmZiNTljOTBhYjA4ZmM5ZGMxZmUyNjgwMjAzNWEzYTQ3YzQyZmVlNjM0" +
            "MjNiY2RiNDI2MmVjYjliNiJ9fX0=";

    private static final String TEXTURE_RESET =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0" +
            "L3RleHR1cmUvYmViNTg4YjIxYTZmOThhZDFmZjRlMDg1YzU1MmRjYjA1MGVmYzljYWI0MjdmNDYw" +
            "NDhmMThmYzgwMzQ3NWY3In19fQ==";

    private final BetterGunGame plugin;

    public GameManagementGui(BetterGunGame plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer) {
        Gui gui = Gui.gui()
                .title(TextUtil.parse(
                        "<gradient:#e63278:#fd8ddb:#e63278><b>ɢᴀᴍᴇ ᴍᴀɴᴀɢᴇᴍᴇɴᴛ</b></gradient>"))
                .rows(3)
                .disableAllInteractions()
                .create();

        // Bottom row glass (slots 18-26)
        for (int slot = 18; slot <= 26; slot++) {
            gui.setItem(slot, ItemBuilder.from(
                    ItemUtil.filler(Material.GRAY_STAINED_GLASS_PANE)).asGuiItem());
        }

        // Start — row 2, col 3
        gui.setItem(2, 3, ItemBuilder.from(
                ItemUtil.applyMeta(SkullUtil.fromBase64(TEXTURE_START),
                        "<#5ac46e><b>ʀᴜɴᴅᴇ sᴛᴀʀᴛᴇɴ</#5ac46e>",
                        "",
                        "<grey><#478ED2>●</#478ED2> ᴋʟɪᴄᴋᴇ ᴜᴍ ᴅɪᴇ ʀᴜɴᴅᴇ ᴢᴜ sᴛᴀʀᴛᴇɴ</grey>",
                        ""))
                .asGuiItem(e -> {
                    viewer.closeInventory();
                    plugin.getGameManager().startGame(viewer);
                }));

        // Map overview — row 2, col 5
        gui.setItem(2, 5, ItemBuilder.from(
                ItemUtil.builder(Material.COMPASS)
                        .name("<#478ED2><b>ᴍᴀᴘ-üʙᴇʀsɪᴄʜᴛ</#478ED2>")
                        .lore(
                                "",
                                "<grey><#478ED2>●</#478ED2> ᴋʟɪᴄᴋᴇ ᴜᴍ ᴅɪᴇ ᴍᴀᴘ-üʙᴇʀsɪᴄʜᴛ ᴢᴜ öꜰꜰɴᴇɴ</grey>",
                                "")
                        .build())
                .asGuiItem(e -> new MapOverviewGui(plugin).open(viewer)));

        // Stop/Reset — row 2, col 7
        gui.setItem(2, 7, ItemBuilder.from(
                ItemUtil.applyMeta(SkullUtil.fromBase64(TEXTURE_RESET),
                        "<#c0392b><b>sᴘɪᴇʟ ᴢᴜʀüᴄᴋsᴇᴛᴢᴇɴ</#c0392b>",
                        "",
                        "<grey><#c0392b>●</#c0392b> ᴋʟɪᴄᴋᴇ ᴜᴍ ᴅᴀs sᴘɪᴇʟ ᴢᴜʀüᴄᴋᴢᴜsᴇᴛᴢᴇɴ</grey>",
                        ""))
                .asGuiItem(e -> {
                    viewer.closeInventory();
                    plugin.getGameManager().stopGame(viewer);
                }));

        gui.open(viewer);
    }
}
