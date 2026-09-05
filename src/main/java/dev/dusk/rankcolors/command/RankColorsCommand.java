package dev.dusk.rankcolors.command;

import dev.dusk.rankcolors.DuskRankColorsPlugin;
import dev.dusk.rankcolors.color.ColorRegistry;
import dev.dusk.rankcolors.config.ConfigValidator;
import dev.dusk.rankcolors.config.MessageService;
import dev.dusk.rankcolors.config.PluginConfiguration;
import dev.dusk.rankcolors.config.SoundService;
import dev.dusk.rankcolors.hook.HookManager;
import dev.dusk.rankcolors.menu.MenuManager;
import dev.dusk.rankcolors.rank.RankRegistry;
import dev.dusk.rankcolors.scheduler.SchedulerAdapter;
import dev.dusk.rankcolors.service.PlayerColorService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public final class RankColorsCommand implements CommandExecutor, TabCompleter {
    private final MessageService messages;
    private final SoundService sounds;
    private final MenuManager menus;
    private final AdminCommandHandler admin;

    public RankColorsCommand(DuskRankColorsPlugin plugin, MessageService messages, SoundService sounds,
                             PluginConfiguration configuration, ColorRegistry registry, RankRegistry ranks,
                             PlayerColorService colors, MenuManager menus, HookManager hooks,
                             SchedulerAdapter scheduler, ConfigValidator configValidator) {
        this.messages = messages;
        this.sounds = sounds;
        this.menus = menus;
        this.admin = new AdminCommandHandler(plugin, messages, sounds, configuration, registry, ranks,
            colors, hooks, scheduler, configValidator);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) messages.send(sender, "player-only");
            else if (requireUse(player)) menus.openMain(player);
            return true;
        }
        if (!args[0].equalsIgnoreCase("admin")) {
            messages.send(sender, "usage.player");
            return true;
        }
        if (!label.equalsIgnoreCase("drc")) {
            messages.send(sender, "usage.admin-namespace");
            return true;
        }
        admin.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return alias.equalsIgnoreCase("drc") && admin.hasAnyPermission(sender)
                && "admin".startsWith(args[0].toLowerCase()) ? List.of("admin") : List.of();
        }
        if (!alias.equalsIgnoreCase("drc") || !args[0].equalsIgnoreCase("admin")) return List.of();
        return admin.complete(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    private boolean requireUse(Player player) {
        if (player.hasPermission("duskrankcolors.use")) return true;
        messages.send(player, "no-permission");
        sounds.play(player, "denied");
        return false;
    }
}
