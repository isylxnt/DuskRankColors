package dev.dusk.rankcolors.hook;

import dev.dusk.rankcolors.api.DisplayContext;
import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.ColorFormatter;
import dev.dusk.rankcolors.color.PlayerColorSelection;
import dev.dusk.rankcolors.service.PlayerColorService;
import dev.dusk.rankcolors.service.RenderedPlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class DuskPlaceholderExpansion extends PlaceholderExpansion {
    private final String version;
    private final PlayerColorService colors;

    public DuskPlaceholderExpansion(String version, PlayerColorService colors) {
        this.version = version;
        this.colors = colors;
    }

    @Override public @NotNull String getIdentifier() { return "duskrankcolors"; }
    @Override public @NotNull String getAuthor() { return "isylxnt"; }
    @Override public @NotNull String getVersion() { return version; }
    @Override public boolean persist() { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String parameters) {
        if (player == null) return "";
        RenderedPlayer rendered = colors.rendered(player.getUniqueId());
        if (rendered == null) return "";
        String key = parameters.toLowerCase(Locale.ROOT);
        if (key.equals("rank")) return rendered.rankLegacy();
        if (key.equals("plus")) return rendered.plusLegacy();
        if (key.equals("name")) return rendered.nameLegacy();
        if (key.equals("full") || key.equals("preview")) return rendered.fullLegacy();
        if (key.equals("rank_id")) return rendered.rankId();
        if (key.equals("rank_display")) return rendered.rankText();
        if (key.equals("rank_priority")) return rendered.rankId().isBlank() ? "" : Integer.toString(rendered.rankPriority());
        if (key.equals("rank_has_plus")) return Boolean.toString(rendered.rankHasPlus());
        if (key.equals("rank_bold")) return Boolean.toString(rendered.rankBold());
        for (DisplayContext context : DisplayContext.values()) {
            if (key.equals(context.key())) return rendered.contextLegacy().getOrDefault(context, "");
        }
        for (ColorCategory category : ColorCategory.values()) {
            String prefix = category.key() + "_";
            if (!key.startsWith(prefix)) continue;
            String suffix = key.substring(prefix.length());
            PlayerColorSelection selection = rendered.settings().selection(category);
            ColorFormatter.SelectionView view = rendered.views().get(category);
            return switch (suffix) {
                case "color" -> view.color();
                case "hex" -> view.hex();
                case "legacy" -> view.legacy();
                case "mode" -> selection.mode().name();
                case "raw" -> selection.rawValue();
                case "gradient" -> view.gradient();
                case "minimessage" -> view.miniMessage();
                default -> null;
            };
        }
        return null;
    }
}
