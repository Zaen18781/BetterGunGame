package dev.zaen.betterGunGame.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
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

        Gui gui = Gui.gui()
                .title(TextUtil.parse("<gradient:#e63278:#fd8ddb><b>ᴘʟᴀʏᴇʀ ᴏᴠᴇʀᴠɪᴇᴡ</b></gradient>"))
                .rows(6)
                .disableAllInteractions()
                .create();

        // Bottom row glass (slots 45-53)
        for (int slot = 45; slot <= 53; slot++) {
            gui.setItem(slot, ItemBuilder.from(
                    ItemUtil.filler(Material.GRAY_STAINED_GLASS_PANE)).asGuiItem());
        }

        // Close — slot 49, uses ❌ like the config
        gui.setItem(49, ItemBuilder.from(
                ItemUtil.builder(Material.BARRIER)
                        .name("<color:#ff0000><b>❌ ꜱᴄʜʟɪᴇꜱꜱᴇɴ</b></color>")
                        .hideAll()
                        .build()
        ).asGuiItem(e -> gui.close(viewer)));

        // Player heads — slots 0–44 (rows 1-5, all columns, no border)
        for (int i = 0; i < Math.min(sorted.size(), 45); i++) {
            GamePlayer gp = sorted.get(i);
            int rank = i + 1;
            ItemStack head = buildHead(gp, rank, maxLevel, canTeleport);
            gui.setItem(i, ItemBuilder.from(head).asGuiItem(e -> {
                if (!canTeleport) return;
                Player target = Bukkit.getPlayer(gp.getUuid());
                if (target == null || !target.isOnline()) return;
                gui.close(viewer);
                viewer.teleportAsync(target.getLocation());
            }));
        }

        gui.open(viewer);
    }

    private ItemStack buildHead(GamePlayer gp, int rank, int maxLevel, boolean canTeleport) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(gp.getUuid());
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(op);

        // Rank color: gold / silver / bronze / default blue
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
