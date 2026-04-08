package dev.zaen.betterGunGame.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public final class ItemUtil {

    private ItemUtil() {}

    public static Builder builder(Material material) {
        return new Builder(material);
    }

    public static ItemStack filler(Material material) {
        return builder(material).name(" ").hideAll().build();
    }

    /**
     * Applies a name and lore to an existing ItemStack (e.g. a skull from SkullUtil).
     * Returns the same ItemStack with updated meta.
     */
    public static ItemStack applyMeta(ItemStack item, String name, String... loreLines) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(TextUtil.parse(name));
        if (loreLines.length > 0) {
            meta.lore(Arrays.stream(loreLines).map(TextUtil::parse).toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    public static class Builder {
        private final ItemStack item;
        private final ItemMeta meta;

        Builder(Material material) {
            this.item = new ItemStack(material);
            this.meta = item.getItemMeta();
        }

        public Builder name(String miniMessage) {
            meta.displayName(TextUtil.parse(miniMessage));
            return this;
        }

        public Builder name(Component component) {
            meta.displayName(component);
            return this;
        }

        public Builder lore(String... lines) {
            meta.lore(Arrays.stream(lines).map(TextUtil::parse).toList());
            return this;
        }

        public Builder lore(List<Component> lore) {
            meta.lore(lore);
            return this;
        }

        public Builder enchant(Enchantment enchantment, int level) {
            meta.addEnchant(enchantment, level, true);
            return this;
        }

        public Builder glowing() {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            return this;
        }

        public Builder hideAll() {
            meta.addItemFlags(ItemFlag.values());
            return this;
        }

        public Builder unbreakable() {
            meta.setUnbreakable(true);
            return this;
        }

        public Builder amount(int amount) {
            item.setAmount(amount);
            return this;
        }

        public ItemStack build() {
            item.setItemMeta(meta);
            return item;
        }
    }
}
