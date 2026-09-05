package dev.dusk.rankcolors.api;

import dev.dusk.rankcolors.color.PlayerColorSelection;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface DuskRankColorsAPI {
    PlayerColorSelection getRankSelection(UUID uuid);
    PlayerColorSelection getPlusSelection(UUID uuid);
    PlayerColorSelection getNameSelection(UUID uuid);
    boolean isLoaded(UUID uuid);
    void setRankSelection(Player player, PlayerColorSelection selection);
    void setPlusSelection(Player player, PlayerColorSelection selection);
    void setNameSelection(Player player, PlayerColorSelection selection);
    CompletionStage<SelectionResult> setRankSelectionAsync(Player player, PlayerColorSelection selection);
    CompletionStage<SelectionResult> setPlusSelectionAsync(Player player, PlayerColorSelection selection);
    CompletionStage<SelectionResult> setNameSelectionAsync(Player player, PlayerColorSelection selection);
    CompletionStage<Boolean> resetRankAsync(Player player);
    CompletionStage<Boolean> resetPlusAsync(Player player);
    CompletionStage<Boolean> resetNameAsync(Player player);
    CompletionStage<Boolean> resetAllAsync(Player player);
    Component formatRank(Player player);
    Component formatPlus(Player player);
    Component formatName(Player player);
    Component formatFull(Player player);
    Optional<RankView> getRank(Player player);
    Component formatPart(Player player, DisplayPart part);
    String formatPartLegacy(Player player, DisplayPart part);
    String formatPartMiniMessage(Player player, DisplayPart part);
    Component formatContext(Player player, DisplayContext context);
    String formatContextLegacy(Player player, DisplayContext context);
    String formatContextMiniMessage(Player player, DisplayContext context);
}
