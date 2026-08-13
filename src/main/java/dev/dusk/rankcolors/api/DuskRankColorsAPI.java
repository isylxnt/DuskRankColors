package dev.dusk.rankcolors.api;

import dev.dusk.rankcolors.color.PlayerColorSelection;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface DuskRankColorsAPI {
    PlayerColorSelection getRankSelection(UUID uuid);
    PlayerColorSelection getPlusSelection(UUID uuid);
    PlayerColorSelection getNameSelection(UUID uuid);
    void setRankSelection(Player player, PlayerColorSelection selection);
    void setPlusSelection(Player player, PlayerColorSelection selection);
    void setNameSelection(Player player, PlayerColorSelection selection);
    Component formatRank(Player player);
    Component formatPlus(Player player);
    Component formatName(Player player);
    Component formatFull(Player player);
}
