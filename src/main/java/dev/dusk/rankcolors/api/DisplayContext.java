package dev.dusk.rankcolors.api;

import java.util.Locale;

public enum DisplayContext {
    CHAT,
    TAB,
    NAMETAG,
    SCOREBOARD;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
