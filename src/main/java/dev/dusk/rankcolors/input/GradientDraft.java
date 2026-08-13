package dev.dusk.rankcolors.input;

import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.GradientDefinition;
import dev.dusk.rankcolors.color.HexColor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class GradientDraft {
    private final ColorCategory category;
    private final List<String> stops;
    private Instant expiresAt;

    public GradientDraft(ColorCategory category, List<String> stops, Instant expiresAt) {
        this.category = category;
        this.stops = new ArrayList<>();
        for (String stop : stops) this.stops.add(HexColor.normalize(stop).orElseThrow());
        this.expiresAt = expiresAt;
    }

    public ColorCategory category() { return category; }
    public List<String> stops() { return List.copyOf(stops); }
    public Instant expiresAt() { return expiresAt; }
    public void touch(Instant expiration) { expiresAt = expiration; }
    public void set(int index, String hex) { stops.set(index, HexColor.normalize(hex).orElseThrow()); }
    public void add(String hex) { stops.add(HexColor.normalize(hex).orElseThrow()); }
    public void remove(int index) { stops.remove(index); }
    public GradientDefinition definition() { return GradientDefinition.fromHex(stops); }
}
