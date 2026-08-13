package dev.dusk.rankcolors.color;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class PlayerColorSettings {
    private final Map<ColorCategory, PlayerColorSelection> selections;

    public PlayerColorSettings(PlayerColorSelection rank, PlayerColorSelection plus, PlayerColorSelection name) {
        EnumMap<ColorCategory, PlayerColorSelection> values = new EnumMap<>(ColorCategory.class);
        values.put(ColorCategory.RANK, Objects.requireNonNull(rank, "rank"));
        values.put(ColorCategory.PLUS, Objects.requireNonNull(plus, "plus"));
        values.put(ColorCategory.NAME, Objects.requireNonNull(name, "name"));
        this.selections = Map.copyOf(values);
    }

    public PlayerColorSelection selection(ColorCategory category) {
        return selections.get(Objects.requireNonNull(category, "category"));
    }

    public PlayerColorSettings with(ColorCategory category, PlayerColorSelection selection) {
        EnumMap<ColorCategory, PlayerColorSelection> copy = new EnumMap<>(ColorCategory.class);
        copy.putAll(selections);
        copy.put(category, Objects.requireNonNull(selection, "selection"));
        return new PlayerColorSettings(copy.get(ColorCategory.RANK), copy.get(ColorCategory.PLUS), copy.get(ColorCategory.NAME));
    }
}
