package dev.dusk.rankcolors.service;

import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.ColorFormatter;
import dev.dusk.rankcolors.color.PlayerColorSettings;
import net.kyori.adventure.text.Component;

import java.util.Map;

public record RenderedPlayer(PlayerColorSettings settings, String rankText,
                             Component rank, Component plus, Component name, Component full,
                             String rankLegacy, String plusLegacy, String nameLegacy, String fullLegacy,
                             Map<ColorCategory, ColorFormatter.SelectionView> views) {
}
