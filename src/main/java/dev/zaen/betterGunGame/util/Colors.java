package dev.zaen.betterGunGame.util;

public enum Colors {
    GREEN("<color:#4bfb00>"),
    SMOOTH_GREEN("<color:#2ecc71>"),
    PASTEL_GREEN("<color:#77dd77>"),
    MINT_GREEN_DARK("<color:#08FB95>"),
    MINT_GREEN_LIGHT("<color:#B5FFE0>"),

    RED("<color:#ff0000>"),
    PASTEL_RED("<color:#ff6961>"),
    DARKER_RED("<color:#dc2626>"),

    BLUE("<color:#08a8f8>"),

    GREY("<color:#AAAAAA>"),
    GREY_CUSTOM("<color:#96a7b2>"),
    DARK_GREY("<color:#181a1f>"),
    PASTEL_GREY("<color:#d3d3d3>"),

    YELLOW("<color:#fede00>"),
    PASTEL_YELLOW("<color:#fdfd96>"),

    ORANGE("<color:#ffa500>"),
    PASTEL_ORANGE("<color:#ffb347>"),

    PURPLE("<color:#7F00FF>"),
    PASTEL_PURPLE("<color:#cba6f7>"),

    DISCORD_BLURPLE("<color:#7289DA>"),
    PASTEL_BLURPLE("<color:#a3b9f7>"),

    GEM_PURPLE("<color:#e43a96>"),

    GOLD_GRADIENT("<gradient:#fff200:#ff9900:#fff200>");

    private final String hex;

    Colors(String hex) {
        this.hex = hex;
    }

    public String getHex() {
        return hex;
    }

    /**
     * Replaces color placeholders in a template string with actual MiniMessage color tags.
     * Supports: {green}, {red}, {blue}, {grey}, {yellow}, {orange}, {purple}, {gold}
     */
    public static String colorize(String template) {
        if (template == null) return null;
        String result = template;
        for (Colors color : values()) {
            String key = "{" + color.name().toLowerCase().replace("_", "") + "}";
            result = result.replace(key, color.hex);
        }
        return result;
    }
}
