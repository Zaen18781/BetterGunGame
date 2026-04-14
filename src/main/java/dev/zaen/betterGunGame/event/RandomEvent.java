package dev.zaen.betterGunGame.event;

public enum RandomEvent {

    DOUBLE_KILLS("⚡ Doppel-Kills",    "Jeder Kill zählt doppelt!"),
    LOW_GRAVITY ("🪂 Niedrige Schwerkraft", "Die Schwerkraft ist reduziert!"),
    SPEED_BOOST ("💨 Speedboost",      "Alle Spieler bewegen sich schneller!"),
    BLINDNESS   ("🌑 Blindheit",       "Alle Spieler sind geblendet!"),
    INVISIBILITY("👻 Unsichtbarkeit",  "Alle Spieler sind unsichtbar!"),
    ONE_SHOT    ("💀 One-Shot",        "Ein Treffer — sofortiger Kill!"),
    STRENGTH    ("💪 Stärke",          "Alle Spieler schlagen härter zu!"),
    JUMP_BOOST  ("🚀 Sprung-Boost",   "Alle Spieler springen extrem hoch!"),
    SLOWNESS    ("🐌 Zeitlupe",        "Alle Spieler bewegen sich in Zeitlupe!");

    private final String displayName;
    private final String description;

    RandomEvent(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
