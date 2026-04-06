package dev.zaen.betterGunGame.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

public final class SkullUtil {

    private SkullUtil() {}

    /**
     * Creates a player-head ItemStack from a base64-encoded texture string.
     * The texture string is the value from Mojang's texture API (already base64).
     */
    @SuppressWarnings("deprecation")
    public static ItemStack fromBase64(String base64) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        try {
            String decoded = new String(Base64.getDecoder().decode(base64));
            // JSON format: {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/..."}}}
            int urlStart = decoded.indexOf("\"url\":\"") + 7;
            int urlEnd   = decoded.indexOf("\"", urlStart);
            String url   = decoded.substring(urlStart, urlEnd);

            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(url));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (MalformedURLException | IllegalArgumentException | StringIndexOutOfBoundsException ignored) {
            // Fallback: plain head
        }

        skull.setItemMeta(meta);
        return skull;
    }
}
