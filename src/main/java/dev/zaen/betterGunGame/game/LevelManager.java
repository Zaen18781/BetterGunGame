package dev.zaen.betterGunGame.game;

import dev.zaen.betterGunGame.BetterGunGame;
import dev.zaen.betterGunGame.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LevelManager {

    private final BetterGunGame plugin;
    private final Map<Integer, ItemStack> levelItems = new HashMap<>();
    private final Map<Integer, List<ItemStack>> levelExtraItems = new HashMap<>();
    private final Map<Integer, ItemStack> levelOffhandItems = new HashMap<>();

    /** Custom overrides loaded from custom_items.yml — take priority over items.yml. */
    private final Map<Integer, ItemStack> customWeapons = new HashMap<>();
    private final Map<Integer, ItemStack> customOffhands = new HashMap<>();
    private final Map<Integer, List<ItemStack>> customExtras = new HashMap<>();

    public LevelManager(BetterGunGame plugin) {
        this.plugin = plugin;
    }

    public void load() {
        levelItems.clear();
        levelExtraItems.clear();
        levelOffhandItems.clear();
        customWeapons.clear();
        customOffhands.clear();
        customExtras.clear();
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

            // Parse offhand item (e.g. shield)
            String offhandName = section.getString("offhand");
            if (offhandName != null) {
                try {
                    Material offhandMat = Material.valueOf(offhandName.toUpperCase());
                    ItemStack offhand = new ItemStack(offhandMat);
                    ItemMeta offhandMeta = offhand.getItemMeta();
                    if (offhandMeta != null) {
                        offhandMeta.setUnbreakable(true);
                        offhand.setItemMeta(offhandMeta);
                    }
                    levelOffhandItems.put(level, offhand);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Unbekanntes Offhand-Item in Level " + level + ": " + offhandName);
                }
            }

            // Parse extra-items (e.g. arrows for bow, wind charges for mace)
            ConfigurationSection extras = section.getConfigurationSection("extra-items");
            if (extras != null) {
                List<ItemStack> extraList = new ArrayList<>();
                for (String matKey : extras.getKeys(false)) {
                    try {
                        Material mat = Material.valueOf(matKey.toUpperCase());
                        int amount = extras.getInt(matKey, 1);
                        extraList.add(new ItemStack(mat, amount));
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("Unbekanntes Extra-Item in Level " + level + ": " + matKey);
                    }
                }
                // Apply tipped-arrow potion effect if configured
                String tippedEffect = section.getString("tipped-arrow-effect");
                if (tippedEffect != null) {
                    int amplifier = section.getInt("tipped-arrow-amplifier", 0);
                    PotionEffectType pet = Registry.EFFECT.get(NamespacedKey.minecraft(tippedEffect.toLowerCase()));
                    if (pet != null) {
                        for (ItemStack extra : extraList) {
                            if (extra.getType() == Material.TIPPED_ARROW
                                    && extra.getItemMeta() instanceof PotionMeta pm) {
                                pm.addCustomEffect(new PotionEffect(pet, 1, amplifier), true);
                                extra.setItemMeta(pm);
                            }
                        }
                    } else {
                        plugin.getLogger().warning("Unbekannter Potion-Effekt in Level " + level + ": " + tippedEffect);
                    }
                }
                if (!extraList.isEmpty()) levelExtraItems.put(level, extraList);
            }
        }

        plugin.getLogger().info("Level-Items geladen: " + levelItems.size() + " Levels.");
        loadCustomItems();
    }

    private void loadCustomItems() {
        java.io.File file = new java.io.File(plugin.getDataFolder(), "custom_items.yml");
        if (!file.exists()) return;

        org.bukkit.configuration.file.YamlConfiguration cfg =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        ConfigurationSection levels = cfg.getConfigurationSection("levels");
        if (levels == null) return;

        int loaded = 0;
        for (String key : levels.getKeys(false)) {
            int level;
            try { level = Integer.parseInt(key); } catch (NumberFormatException e) { continue; }
            ConfigurationSection section = levels.getConfigurationSection(key);
            if (section == null) continue;

            String weaponB64 = section.getString("weapon");
            if (weaponB64 != null && !weaponB64.isEmpty()) {
                try {
                    customWeapons.put(level, ItemStack.deserializeBytes(Base64.getDecoder().decode(weaponB64)));
                    loaded++;
                } catch (Exception e) {
                    plugin.getLogger().warning("Fehler beim Laden des Custom-Weapons für Level " + level);
                }
            }

            String offhandB64 = section.getString("offhand");
            if (offhandB64 != null && !offhandB64.isEmpty()) {
                try {
                    customOffhands.put(level, ItemStack.deserializeBytes(Base64.getDecoder().decode(offhandB64)));
                } catch (Exception e) {
                    plugin.getLogger().warning("Fehler beim Laden des Custom-Offhands für Level " + level);
                }
            }

            List<String> extraB64List = section.getStringList("extras");
            List<ItemStack> extraItems = new ArrayList<>();
            for (String b64 : extraB64List) {
                try { extraItems.add(ItemStack.deserializeBytes(Base64.getDecoder().decode(b64))); }
                catch (Exception e) { plugin.getLogger().warning("Fehler beim Laden der Custom-Extras für Level " + level); }
            }
            if (!extraItems.isEmpty()) customExtras.put(level, extraItems);
        }

        if (loaded > 0) plugin.getLogger().info("Custom Level-Items geladen: " + loaded + " Levels überschrieben.");
    }

    /** Saves a custom level kit to custom_items.yml and updates in-memory maps. */
    public void saveCustomLevel(int level, ItemStack weapon, ItemStack offhand, List<ItemStack> extras) {
        java.io.File file = new java.io.File(plugin.getDataFolder(), "custom_items.yml");
        org.bukkit.configuration.file.YamlConfiguration cfg = file.exists()
                ? org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file)
                : new org.bukkit.configuration.file.YamlConfiguration();

        String path = "levels." + level;

        if (weapon != null && weapon.getType() != Material.AIR) {
            cfg.set(path + ".weapon", Base64.getEncoder().encodeToString(weapon.serializeAsBytes()));
            customWeapons.put(level, weapon.clone());
        }

        if (offhand != null && offhand.getType() != Material.AIR) {
            cfg.set(path + ".offhand", Base64.getEncoder().encodeToString(offhand.serializeAsBytes()));
            customOffhands.put(level, offhand.clone());
        } else {
            cfg.set(path + ".offhand", null);
            customOffhands.remove(level);
        }

        List<String> extraB64 = new ArrayList<>();
        List<ItemStack> extraClones = new ArrayList<>();
        for (ItemStack extra : extras) {
            if (extra != null && extra.getType() != Material.AIR) {
                extraB64.add(Base64.getEncoder().encodeToString(extra.serializeAsBytes()));
                extraClones.add(extra.clone());
            }
        }
        cfg.set(path + ".extras", extraB64.isEmpty() ? null : extraB64);
        if (!extraClones.isEmpty()) customExtras.put(level, extraClones);
        else customExtras.remove(level);

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Fehler beim Speichern von custom_items.yml: " + e.getMessage());
        }
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

    /** Returns the weapon item for the given level (1-based). Custom overrides items.yml. */
    public ItemStack getItemForLevel(int level) {
        ItemStack source = customWeapons.containsKey(level) ? customWeapons.get(level) : levelItems.get(level);
        ItemStack result = source != null ? source.clone() : new ItemStack(Material.STICK);
        ItemMeta meta = result.getItemMeta();
        if (meta != null) { meta.setUnbreakable(true); result.setItemMeta(meta); }
        return result;
    }

    public List<ItemStack> getExtraItemsForLevel(int level) {
        if (customWeapons.containsKey(level)) return customExtras.getOrDefault(level, Collections.emptyList());
        return levelExtraItems.getOrDefault(level, Collections.emptyList());
    }

    /** Returns the offhand item for the given level, or null if none. Custom overrides items.yml. */
    public ItemStack getOffhandForLevel(int level) {
        if (customWeapons.containsKey(level)) {
            ItemStack item = customOffhands.get(level);
            return item != null ? item.clone() : null;
        }
        ItemStack item = levelOffhandItems.get(level);
        return item != null ? item.clone() : null;
    }

    /** Returns the raw (non-gameplay) weapon for display in GUIs — no forced unbreakable flag. */
    public ItemStack getRawItemForLevel(int level) {
        ItemStack source = customWeapons.containsKey(level) ? customWeapons.get(level) : levelItems.get(level);
        return source != null ? source.clone() : new ItemStack(Material.STICK);
    }

    public int getMaxLevel() {
        return levelItems.isEmpty() ? plugin.getConfigManager().getMaxLevels()
                : levelItems.keySet().stream().mapToInt(Integer::intValue).max().orElse(50);
    }
}
