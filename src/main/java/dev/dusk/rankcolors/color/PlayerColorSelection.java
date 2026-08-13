package dev.dusk.rankcolors.color;

import java.util.Locale;
import java.util.Objects;

public final class PlayerColorSelection {
    private final ColorMode mode;
    private final String presetId;
    private final String rgbHex;
    private final GradientDefinition gradient;

    private PlayerColorSelection(ColorMode mode, String presetId, String rgbHex, GradientDefinition gradient) {
        this.mode = mode;
        this.presetId = presetId;
        this.rgbHex = rgbHex;
        this.gradient = gradient;
    }

    public static PlayerColorSelection preset(String presetId) {
        Objects.requireNonNull(presetId, "presetId");
        String normalized = presetId.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) throw new IllegalArgumentException("Preset id cannot be empty");
        return new PlayerColorSelection(ColorMode.PRESET, normalized, null, null);
    }

    public static PlayerColorSelection rgb(String hex) {
        return new PlayerColorSelection(ColorMode.RGB, null,
            HexColor.normalize(hex).orElseThrow(() -> new IllegalArgumentException("Invalid HEX color: " + hex)), null);
    }

    public static PlayerColorSelection gradient(GradientDefinition gradient) {
        return new PlayerColorSelection(ColorMode.GRADIENT, null, null, Objects.requireNonNull(gradient, "gradient"));
    }

    public ColorMode mode() {
        return mode;
    }

    public String presetId() {
        if (mode != ColorMode.PRESET) throw new IllegalStateException("Selection is not a preset");
        return presetId;
    }

    public String rgbHex() {
        if (mode != ColorMode.RGB) throw new IllegalStateException("Selection is not RGB");
        return rgbHex;
    }

    public GradientDefinition gradient() {
        if (mode != ColorMode.GRADIENT) throw new IllegalStateException("Selection is not a gradient");
        return gradient;
    }

    public String rawValue() {
        return switch (mode) {
            case PRESET -> presetId;
            case RGB -> rgbHex;
            case GRADIENT -> String.join(",", gradient.hexColors());
        };
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PlayerColorSelection that)) return false;
        return mode == that.mode && Objects.equals(presetId, that.presetId)
            && Objects.equals(rgbHex, that.rgbHex) && Objects.equals(gradient, that.gradient);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, presetId, rgbHex, gradient);
    }

    @Override
    public String toString() {
        return mode + ":" + rawValue();
    }
}
