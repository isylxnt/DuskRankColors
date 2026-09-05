package dev.dusk.rankcolors.service;

import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.ColorDefinition;
import dev.dusk.rankcolors.color.ColorMode;
import dev.dusk.rankcolors.color.ColorRegistry;
import dev.dusk.rankcolors.color.PlayerColorSelection;
import dev.dusk.rankcolors.config.PluginConfiguration;
import dev.dusk.rankcolors.rank.RankDefinition;
import dev.dusk.rankcolors.rank.RankRegistry;
import org.bukkit.entity.Player;

public final class SelectionValidator {
    private final PluginConfiguration configuration;
    private final ColorRegistry colors;
    private final RankRegistry ranks;

    public SelectionValidator(PluginConfiguration configuration, ColorRegistry colors, RankRegistry ranks) {
        this.configuration = configuration;
        this.colors = colors;
        this.ranks = ranks;
    }

    public Result validate(Player player, ColorCategory category, PlayerColorSelection selection, boolean bypassPermission) {
        if (!configuration.modeEnabled(category, selection.mode())) return Result.MODE_DISABLED;
        RankDefinition rank = ranks.resolve(player);
        if (!bypassPermission && !rank.allowsMode(category, selection.mode())) return Result.RANK_RESTRICTED;
        if (!bypassPermission && configuration.validatePermissions()
            && !player.hasPermission("duskrankcolors." + category.key())) return Result.PERMISSION;
        if (selection.mode() == ColorMode.PRESET) {
            ColorDefinition color = colors.get(selection.presetId()).orElse(null);
            if (color == null || !color.allows(category)) return Result.INVALID;
            if (!bypassPermission && !rank.allowsColor(category, color.id())) return Result.RANK_RESTRICTED;
            if (!bypassPermission && configuration.validatePermissions() && !player.hasPermission(color.permission(category))) {
                return Result.PERMISSION;
            }
        } else if (selection.mode() == ColorMode.GRADIENT) {
            int size = selection.gradient().colors().size();
            if (size < configuration.minGradientColors() || size > configuration.maxGradientColors()) return Result.INVALID;
        }
        if (!bypassPermission && configuration.validatePermissions()) {
            String modePermission = category.permission(selection.mode().name().toLowerCase(java.util.Locale.ROOT));
            if (selection.mode() != ColorMode.PRESET && !player.hasPermission(modePermission)) return Result.PERMISSION;
        }
        return Result.OK;
    }

    public enum Result {
        OK,
        INVALID,
        MODE_DISABLED,
        PERMISSION,
        RANK_RESTRICTED,
        CANCELLED
    }
}
