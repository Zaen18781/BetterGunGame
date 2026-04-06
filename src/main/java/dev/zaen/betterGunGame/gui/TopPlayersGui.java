package dev.zaen.betterGunGame.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.zaen.betterGunGame.BetterGunGame;
import dev.zaen.betterGunGame.game.GamePlayer;
import dev.zaen.betterGunGame.util.ItemUtil;
import dev.zaen.betterGunGame.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class TopPlayersGui {

    // Podium layout: #1 center (slot 13), #2 left (slot 11), #3 right (slot 15)
    private static final int[] SLOTS = {13, 11, 15};

    private static final String[] RANK_GRADIENT = {
            "<gradient:#ffd700:#ff8c00>",
            "<gradient:#c0c0c0:#9e9e9e>",
            "<gradient:#cd7f32:#a0522d>"
    };
    private static final String[] MEDALS = {"🥇", "🥈", "🥉"};

    private final BetterGunGame plugin;
    private final List<GamePlayer> sorted;

    public TopPlayersGui(BetterGunGame plugin, List<GamePlayer> sorted) {
        this.plugin = plugin;
        this.sorted = sorted;
    }

    public void open(Player viewer) {
        int maxLevel = plugin.getLevelManager().getMaxLevel();

        Gui gui = Gui.gui()
                .title(TextUtil.parse("<gradient:#ffd700:#ff8c00><b>ʀᴜɴᴅᴇɴ-ᴇʀɢᴇʙɴɪs</b></gradient>"))
                .rows(6)
                .disableAllInteractions()
                .create();

        // Fill rows 1-5 with dark glass for atmosphere
        for (int slot = 0; slot < 45; slot++) {
            gui.setItem(slot, ItemBuilder.from(
                    ItemUtil.filler(Material.BLACK_STAINED_GLASS_PANE)).asGuiItem());
        }

        // Bottom row (slots 45-53)
        for (int slot = 45; slot <= 53; slot++) {
            gui.setItem(slot, ItemBuilder.from(
                    ItemUtil.filler(Material.GRAY_STAINED_GLASS_PANE)).asGuiItem());
        }

        // Close button slot 49
        gui.setItem(49, ItemBuilder.from(
                ItemUtil.builder(Material.BARRIER)
                        .name("<color:#ff0000><b>❌ ꜱᴄʜʟɪᴇꜱꜱᴇɴ</b></color>")
                        .hideAll()
                        .build()
        ).asGuiItem(e -> gui.close(viewer)));

        // Decorative gold blocks above each podium spot
        gui.setItem(4,  ItemBuilder.from(ItemUtil.filler(Material.GOLD_BLOCK)).asGuiItem());
        gui.setItem(10, ItemBuilder.from(ItemUtil.filler(Material.IRON_BLOCK)).asGuiItem());
        gui.setItem(16, ItemBuilder.from(ItemUtil.filler(Material.COPPER_BLOCK)).asGuiItem());

        // Player heads on podium
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            GamePlayer gp = sorted.get(i);
            gui.setItem(SLOTS[i], ItemBuilder.from(buildHead(gp, i, maxLevel)).asGuiItem());
        }

        // Filler item: show "no player" for missing podium spots
        for (int i = sorted.size(); i < 3; i++) {
            gui.setItem(SLOTS[i], ItemBuilder.from(
                    ItemUtil.builder(Material.BARRIER)
                            .name("<gray><i>ᴋᴇɪɴ ꜱᴘɪᴇʟᴇʀ</i></gray>")
                            .hideAll()
                            .build()
            ).asGuiItem());
        }

        gui.open(viewer);
    }

    private ItemStack buildHead(GamePlayer gp, int rank, int maxLevel) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(gp.getUuid());
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(op);

        String grad = RANK_GRADIENT[rank];
        meta.displayName(TextUtil.parse(
                grad + "<b>" + MEDALS[rank] + " " + gp.getName() + "</b></gradient>"));

        List<Component> lore = new ArrayList<>();
        lore.add(TextUtil.parse(""));
        lore.add(TextUtil.parse(grad + "<b>ᴘʟᴀᴛᴢ #" + (rank + 1) + "</b></gradient>"));
        lore.add(TextUtil.parse(""));
        lore.add(TextUtil.parse("<gray><color:#ffd700>●</color> ʟᴇᴠᴇʟ: <white>" + gp.getLevel() + "</white><dark_gray>/" + maxLevel + "</dark_gray></gray>"));
        lore.add(TextUtil.parse("<gray><color:#ffd700>●</color> ᴋɪʟʟs: <white>" + gp.getKills() + "</white></gray>"));
        lore.add(TextUtil.parse(""));

        meta.lore(lore);
        skull.setItemMeta(meta);
        return skull;
    }
}
