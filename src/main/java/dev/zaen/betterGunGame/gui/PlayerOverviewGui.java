package dev.zaen.betterGunGame.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.PaginatedGui;
import dev.zaen.betterGunGame.BetterGunGame;
import dev.zaen.betterGunGame.game.GameArena;
import dev.zaen.betterGunGame.game.GamePlayer;
import dev.zaen.betterGunGame.util.ItemUtil;
import dev.zaen.betterGunGame.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class PlayerOverviewGui {

    private final BetterGunGame plugin;
    private final GameArena arena;

    public PlayerOverviewGui(BetterGunGame plugin, GameArena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    public void open(Player viewer) {
        List<GamePlayer> sorted = arena.getSortedPlayers();
        int maxLevel = plugin.getLevelManager().getMaxLevel();
        boolean canTeleport = viewer.isOp() || viewer.hasPermission("bgg.admin");

        PaginatedGui gui = dev.triumphteam.gui.guis.Gui.paginated()
                .title(TextUtil.parse("<gradient:#e63278:#fd8ddb><b>ᴘʟᴀʏᴇʀ ᴏᴠᴇʀᴠɪᴇᴡ</b></gradient>"))
                .rows(6)
                .pageSize(45)
                .disableAllInteractions()
                .create();

        applyNavigation(gui, viewer);

        for (int i = 0; i < sorted.size(); i++) {
            GamePlayer gp = sorted.get(i);
            int rank = i + 1;
            gui.addItem(ItemBuilder.from(buildHead(gp, rank, maxLevel, canTeleport))
                    .asGuiItem(e -> {
                        if (!canTeleport) return;
                        Player target = Bukkit.getPlayer(gp.getUuid());
                        if (target == null || !target.isOnline()) return;
                        viewer.closeInventory();
                        viewer.teleportAsync(target.getLocation());
                    }));
        }

        gui.open(viewer);
    }

    private void applyNavigation(PaginatedGui gui, Player viewer) {
        for (int slot = 45; slot <= 53; slot++) {
            gui.setItem(slot, ItemBuilder.from(
                    ItemUtil.filler(Material.GRAY_STAINED_GLASS_PANE)).asGuiItem());
        }

        gui.setItem(48, MapOverviewGui.buildNavItem(gui, true)
                .asGuiItem(e -> { gui.previous(); refreshNav(gui, viewer); }));
        gui.setItem(49, MapOverviewGui.buildPageInfo(gui));
        gui.setItem(50, MapOverviewGui.buildNavItem(gui, false)
                .asGuiItem(e -> { gui.next(); refreshNav(gui, viewer); }));

        // Close button
        gui.setItem(53, ItemBuilder.from(
                ItemUtil.builder(Material.BARRIER)
                        .name("<color:#ff0000><b>❌ ꜱᴄʜʟɪᴇꜱꜱᴇɴ</b></color>")
                        .hideAll()
                        .build())
                .asGuiItem(e -> viewer.closeInventory()));
    }

    private void refreshNav(PaginatedGui gui, Player viewer) {
        gui.updateItem(48, MapOverviewGui.buildNavItem(gui, true)
                .asGuiItem(e -> { gui.previous(); refreshNav(gui, viewer); }));
        gui.updateItem(49, MapOverviewGui.buildPageInfo(gui));
        gui.updateItem(50, MapOverviewGui.buildNavItem(gui, false)
                .asGuiItem(e -> { gui.next(); refreshNav(gui, viewer); }));
    }

    private ItemStack buildHead(GamePlayer gp, int rank, int maxLevel, boolean canTeleport) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(gp.getUuid());
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(op);

        String rankColor = switch (rank) {
            case 1 -> "<gradient:#ffd700:#ff8c00>";
            case 2 -> "<gradient:#c0c0c0:#9e9e9e>";
            case 3 -> "<gradient:#cd7f32:#a0522d>";
            default -> "<color:#08a8f8>";
        };
        String rankClose = rank <= 3 ? "</gradient>" : "</color>";
        meta.displayName(TextUtil.parse(rankColor + "<b>#" + rank + " — " + gp.getName() + "</b>" + rankClose));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(TextUtil.parse(""));
        lore.add(TextUtil.parse("<grey><color:#08a8f8>●</color> ʟᴇᴠᴇʟ: <white>" + gp.getLevel() + "</white><dark_gray>/" + maxLevel + "</dark_gray></grey>"));
        lore.add(TextUtil.parse("<grey><color:#08a8f8>●</color> ᴋɪʟʟs: <white>" + gp.getKills() + "</white></grey>"));
        lore.add(TextUtil.parse("<grey><color:#08a8f8>●</color> ʀᴀɴɢ: <white>#" + rank + "</white></grey>"));
        if (canTeleport) {
            lore.add(TextUtil.parse(""));
            lore.add(TextUtil.parse("<grey>ᴋʟɪᴄᴋᴇɴ ᴜᴍ ᴢᴜ <color:#08a8f8>" + gp.getName() + "</color> ᴢᴜ ᴛᴇʟᴇᴘᴏʀᴛɪᴇʀᴇɴ</grey>"));
        }
        lore.add(TextUtil.parse(""));
        meta.lore(lore);
        skull.setItemMeta(meta);
        return skull;
    }
}
