package dev.dusk.rankcolors.storage;

import dev.dusk.rankcolors.color.GradientDefinition;
import dev.dusk.rankcolors.color.HexColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GradientSerializer {
    public String serialize(GradientDefinition gradient) {
        return String.join(",", gradient.hexColors());
    }

    public Optional<GradientDefinition> deserialize(String value, int minimum, int maximum) {
        if (value == null || value.isBlank()) return Optional.empty();
        String[] parts = value.split(",", -1);
        if (parts.length < minimum || parts.length > maximum) return Optional.empty();
        List<String> colors = new ArrayList<>(parts.length);
        for (String part : parts) {
            Optional<String> normalized = HexColor.normalize(part);
            if (normalized.isEmpty()) return Optional.empty();
            colors.add(normalized.get());
        }
        try {
            return Optional.of(GradientDefinition.fromHex(colors));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
