package dev.dusk.rankcolors.input;

import dev.dusk.rankcolors.color.ColorCategory;

import java.time.Instant;

public record PendingColorInput(ColorCategory category, InputType type, int gradientIndex,
                                MenuReturn returnTo, Instant expiresAt) {
    public enum InputType { RGB, GRADIENT_STOP }
    public enum MenuReturn { COLOR_TYPE, GRADIENT_EDITOR }
}
