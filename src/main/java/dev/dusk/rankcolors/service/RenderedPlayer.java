package dev.dusk.rankcolors.service;

import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.ColorFormatter;
import dev.dusk.rankcolors.color.PlayerColorSettings;
import dev.dusk.rankcolors.api.DisplayContext;
import net.kyori.adventure.text.Component;

import java.util.Map;

public record RenderedPlayer(PlayerColorSettings settings, String rankId, String rankText, String rankPermission,
                             int rankPriority,
                             boolean rankHasPlus, boolean rankBold,
                             Component rank, Component plus, Component name, Component full,
                             String rankLegacy, String plusLegacy, String nameLegacy, String fullLegacy,
                             Map<ColorCategory, ColorFormatter.SelectionView> views,
                             Map<DisplayContext, Component> contexts,
                             Map<DisplayContext, String> contextLegacy) {
}
