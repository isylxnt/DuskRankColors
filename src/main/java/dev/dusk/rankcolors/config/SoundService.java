package dev.dusk.rankcolors.config;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Locale;

/** Plays optional, config-driven UI feedback sounds with safe cross-version fallbacks. */
public final class SoundService {
    private final ConfigManager configs;

    public SoundService(ConfigManager configs) {
        this.configs = configs;
    }

    public void play(Player player, String type) {
        String root = "sounds." + type;
        if (!configs.menus().getBoolean(root + ".enabled", true)) return;
        String value = configs.menus().getString(root + ".sound");
        if (value == null) return;
        try {
            Sound sound = Sound.valueOf(value.toUpperCase(Locale.ROOT));
            float volume = (float) configs.menus().getDouble(root + ".volume", 0.5);
            float pitch = (float) configs.menus().getDouble(root + ".pitch", 1.0);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ignored) {
            // Invalid or unavailable sounds deliberately fall back to silence.
        }
    }
}
