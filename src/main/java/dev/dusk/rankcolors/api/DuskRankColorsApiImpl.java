package dev.dusk.rankcolors.api;

import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.PlayerColorSelection;
import dev.dusk.rankcolors.service.PlayerColorService;
import dev.dusk.rankcolors.service.RenderedPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class DuskRankColorsApiImpl implements DuskRankColorsAPI {
    private final PlayerColorService colors;

    public DuskRankColorsApiImpl(PlayerColorService colors) {
        this.colors = colors;
    }

    @Override public PlayerColorSelection getRankSelection(UUID uuid) { return colors.settings(uuid).selection(ColorCategory.RANK); }
    @Override public PlayerColorSelection getPlusSelection(UUID uuid) { return colors.settings(uuid).selection(ColorCategory.PLUS); }
    @Override public PlayerColorSelection getNameSelection(UUID uuid) { return colors.settings(uuid).selection(ColorCategory.NAME); }
    @Override public void setRankSelection(Player player, PlayerColorSelection selection) { set(player, ColorCategory.RANK, selection); }
    @Override public void setPlusSelection(Player player, PlayerColorSelection selection) { set(player, ColorCategory.PLUS, selection); }
    @Override public void setNameSelection(Player player, PlayerColorSelection selection) { set(player, ColorCategory.NAME, selection); }

    private void set(Player player, ColorCategory category, PlayerColorSelection selection) {
        colors.setSelection(player, category, selection, false, ignored -> { });
    }

    @Override public Component formatRank(Player player) { return rendered(player).rank(); }
    @Override public Component formatPlus(Player player) { return rendered(player).plus(); }
    @Override public Component formatName(Player player) { return rendered(player).name(); }
    @Override public Component formatFull(Player player) { return rendered(player).full(); }

    private RenderedPlayer rendered(Player player) {
        RenderedPlayer rendered = colors.rendered(player.getUniqueId());
        if (rendered == null) throw new IllegalStateException("Player is not loaded in DuskRankColors");
        return rendered;
    }
}
