package dev.dusk.rankcolors.api;

import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.PlayerColorSelection;
import dev.dusk.rankcolors.service.PlayerColorService;
import dev.dusk.rankcolors.service.RenderedPlayer;
import dev.dusk.rankcolors.service.SelectionValidator;
import dev.dusk.rankcolors.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class DuskRankColorsApiImpl implements DuskRankColorsAPI {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final PlayerColorService colors;

    public DuskRankColorsApiImpl(PlayerColorService colors) {
        this.colors = colors;
    }

    @Override public PlayerColorSelection getRankSelection(UUID uuid) { return colors.settings(uuid).selection(ColorCategory.RANK); }
    @Override public PlayerColorSelection getPlusSelection(UUID uuid) { return colors.settings(uuid).selection(ColorCategory.PLUS); }
    @Override public PlayerColorSelection getNameSelection(UUID uuid) { return colors.settings(uuid).selection(ColorCategory.NAME); }
    @Override public boolean isLoaded(UUID uuid) { return colors.rendered(uuid) != null; }
    @Override public void setRankSelection(Player player, PlayerColorSelection selection) { set(player, ColorCategory.RANK, selection); }
    @Override public void setPlusSelection(Player player, PlayerColorSelection selection) { set(player, ColorCategory.PLUS, selection); }
    @Override public void setNameSelection(Player player, PlayerColorSelection selection) { set(player, ColorCategory.NAME, selection); }

    @Override public CompletionStage<SelectionResult> setRankSelectionAsync(Player player, PlayerColorSelection selection) {
        return setAsync(player, ColorCategory.RANK, selection);
    }

    @Override public CompletionStage<SelectionResult> setPlusSelectionAsync(Player player, PlayerColorSelection selection) {
        return setAsync(player, ColorCategory.PLUS, selection);
    }

    @Override public CompletionStage<SelectionResult> setNameSelectionAsync(Player player, PlayerColorSelection selection) {
        return setAsync(player, ColorCategory.NAME, selection);
    }

    @Override public CompletionStage<Boolean> resetRankAsync(Player player) { return resetAsync(player, ColorCategory.RANK); }
    @Override public CompletionStage<Boolean> resetPlusAsync(Player player) { return resetAsync(player, ColorCategory.PLUS); }
    @Override public CompletionStage<Boolean> resetNameAsync(Player player) { return resetAsync(player, ColorCategory.NAME); }

    @Override
    public CompletionStage<Boolean> resetAllAsync(Player player) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        colors.resetAll(player, future::complete);
        return future;
    }

    private void set(Player player, ColorCategory category, PlayerColorSelection selection) {
        setAsync(player, category, selection);
    }

    private CompletionStage<SelectionResult> setAsync(Player player, ColorCategory category,
                                                       PlayerColorSelection selection) {
        CompletableFuture<SelectionResult> future = new CompletableFuture<>();
        colors.setSelection(player, category, selection, false, result -> future.complete(map(result)));
        return future;
    }

    private CompletionStage<Boolean> resetAsync(Player player, ColorCategory category) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        colors.reset(player, category, future::complete);
        return future;
    }

    private SelectionResult map(SelectionValidator.Result result) {
        return switch (result) {
            case OK -> SelectionResult.SUCCESS;
            case INVALID -> SelectionResult.INVALID_SELECTION;
            case MODE_DISABLED -> SelectionResult.MODE_DISABLED;
            case PERMISSION -> SelectionResult.NO_PERMISSION;
            case RANK_RESTRICTED -> SelectionResult.RANK_RESTRICTED;
            case CANCELLED -> SelectionResult.CANCELLED;
        };
    }

    @Override public Component formatRank(Player player) { return rendered(player).rank(); }
    @Override public Component formatPlus(Player player) { return rendered(player).plus(); }
    @Override public Component formatName(Player player) { return rendered(player).name(); }
    @Override public Component formatFull(Player player) { return rendered(player).full(); }

    @Override
    public Optional<RankView> getRank(Player player) {
        RenderedPlayer value = rendered(player);
        if (value.rankId().isBlank()) return Optional.empty();
        return Optional.of(new RankView(value.rankId(), value.rankText(), value.rankPermission(),
            value.rankPriority(), value.rankHasPlus(), value.rankBold()));
    }

    @Override
    public Component formatPart(Player player, DisplayPart part) {
        RenderedPlayer value = rendered(player);
        return switch (part) {
            case RANK -> value.rank();
            case PLUS -> value.plus();
            case NAME -> value.name();
            case FULL -> value.full();
        };
    }

    @Override public String formatPartLegacy(Player player, DisplayPart part) {
        return Text.section(formatPart(player, part));
    }

    @Override public String formatPartMiniMessage(Player player, DisplayPart part) {
        return MINI_MESSAGE.serialize(formatPart(player, part));
    }

    @Override public Component formatContext(Player player, DisplayContext context) {
        return rendered(player).contexts().getOrDefault(context, Component.empty());
    }

    @Override public String formatContextLegacy(Player player, DisplayContext context) {
        return rendered(player).contextLegacy().getOrDefault(context, "");
    }

    @Override public String formatContextMiniMessage(Player player, DisplayContext context) {
        return MINI_MESSAGE.serialize(formatContext(player, context));
    }

    private RenderedPlayer rendered(Player player) {
        RenderedPlayer rendered = colors.rendered(player.getUniqueId());
        if (rendered == null) throw new IllegalStateException("Player is not loaded in DuskRankColors");
        return rendered;
    }
}
