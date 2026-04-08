package dev.zaen.betterGunGame.game;

import dev.zaen.betterGunGame.BetterGunGame;
import dev.zaen.betterGunGame.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class LevelManager {

    private final BetterGunGame plugin;
    private final Map<Integer, ItemStack> levelItems = new HashMap<>();

    public LevelManager(BetterGunGame plugin) {
        this.plugin = plugin;
    }

    public void load() {
        levelItems.clear();
        FileConfiguration cfg = plugin.getConfigManager().getItemsConfig();
        ConfigurationSection levels = cfg.getConfigurationSection("levels");
        if (levels == null) {
            plugin.getLogger().warning("items.yml hat keinen 'levels'-Abschnitt!");
            return;
        }

        for (String key : levels.getKeys(false)) {
            int level;
            try {
                level = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Ungültiger Level-Key in items.yml: " + key);
                continue;
            }

            ConfigurationSection section = levels.getConfigurationSection(key);
            if (section == null) continue;

            String materialName = section.getString("material", "STICK");
            Material material;
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unbekanntes Material in Level " + level + ": " + materialName);
                material = Material.STICK;
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            String name = section.getString("name");
            if (name != null) {
                meta.displayName(TextUtil.parse(name));
            }

            ConfigurationSection enchants = section.getConfigurationSection("enchantments");
            if (enchants != null) {
                for (String enchKey : enchants.getKeys(false)) {
                    Enchantment ench = resolveEnchantment(enchKey);
                    if (ench != null) {
                        meta.addEnchant(ench, enchants.getInt(enchKey), true);
                    }
                }
            }

            item.setItemMeta(meta);
            levelItems.put(level, item);
        }

        plugin.getLogger().info("Level-Items geladen: " + levelItems.size() + " Levels.");
    }

    private Enchantment resolveEnchantment(String key) {
        org.bukkit.NamespacedKey nk = org.bukkit.NamespacedKey.minecraft(key.toLowerCase());
        Enchantment ench = io.papermc.paper.registry.RegistryAccess.registryAccess()
                .getRegistry(io.papermc.paper.registry.RegistryKey.ENCHANTMENT)
                .get(nk);
        if (ench != null) return ench;

        plugin.getLogger().warning("Unbekannte Verzauberung: " + key);
        return null;
    }

    /** Returns the weapon item for the given level (1-based). Returns stick if not found. */
    public ItemStack getItemForLevel(int level) {
        ItemStack item = levelItems.get(level);
        ItemStack result = item != null ? item.clone() : new ItemStack(Material.STICK);
        ItemMeta meta = result.getItemMeta();
        meta.setUnbreakable(true);
        result.setItemMeta(meta);
        return result;
    }

    public int getMaxLevel() {
        return levelItems.isEmpty() ? plugin.getConfigManager().getMaxLevels()
                : levelItems.keySet().stream().mapToInt(Integer::intValue).max().orElse(50);
    }
}
