package dev.zaen.betterGunGame.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

public final class TextUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private TextUtil() {}

    /**
     * Parses a MiniMessage string and automatically converts text to small caps.
     * MiniMessage tags (<...>) are preserved and only the visible text is converted.
     * Italic is automatically disabled to prevent unicode small caps from looking italic.
     */
    public static Component parse(String miniMessage) {
        return MM.deserialize("<italic:false>" + toSmallCaps(miniMessage));
    }

    /**
     * Parses a MiniMessage string with tag resolvers and automatically converts text to small caps.
     * MiniMessage tags (<...>) are preserved and only the visible text is converted.
     * Italic is automatically disabled to prevent unicode small caps from looking italic.
     */
    public static Component parse(String miniMessage, TagResolver... resolvers) {
        return MM.deserialize("<italic:false>" + toSmallCaps(miniMessage), resolvers);
    }

    public static void send(Player player, String miniMessage) {
        player.sendMessage(parse(miniMessage));
    }

    public static void send(Player player, String miniMessage, TagResolver... resolvers) {
        player.sendMessage(parse(miniMessage, resolvers));
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.showTitle(net.kyori.adventure.title.Title.title(
                parse(title),
                parse(subtitle),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(fadeIn * 50L),
                        java.time.Duration.ofMillis(stay * 50L),
                        java.time.Duration.ofMillis(fadeOut * 50L)
                )
        ));
    }

    public static void sendActionBar(Player player, String miniMessage) {
        player.sendActionBar(parse(miniMessage));
    }

    public static void broadcast(String miniMessage) {
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            p.sendMessage(parse(miniMessage));
        }
    }

    private static final String NORMAL  = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final String SMALL   = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ0123456789";

    /**
     * Converts a plain ASCII string to Unicode small-caps characters.
     * Non-letter/non-digit characters are passed through unchanged.
     * MiniMessage tags (inside < >) are preserved and not translated.
     */
    public static String toSmallCaps(String input) {
        StringBuilder out = new StringBuilder(input.length());
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);

            // MiniMessage tag erkennen: <...>
            if (c == '<') {
                int closeIndex = input.indexOf('>', i);
                if (closeIndex != -1) {
                    // Komplettes Tag unverändert übernehmen
                    out.append(input, i, closeIndex + 1);
                    i = closeIndex + 1;
                    continue;
                }
            }

            // Normale Zeichen übersetzen
            int idx = NORMAL.indexOf(Character.toLowerCase(c));
            out.append(idx >= 0 ? SMALL.charAt(idx) : c);
            i++;
        }
        return out.toString();
    }

    /**
     * Converts only the text content (outside MiniMessage tags) to small caps.
     * Tag arguments like <gradient:color> are preserved exactly.
     */
    public static String toSmallCapsWithTags(String input) {
        return toSmallCaps(input);
    }
}
