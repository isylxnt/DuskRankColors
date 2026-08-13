package dev.dusk.rankcolors.command;

import dev.dusk.rankcolors.DuskRankColorsPlugin;
import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.ColorDefinition;
import dev.dusk.rankcolors.color.ColorRegistry;
import dev.dusk.rankcolors.color.GradientDefinition;
import dev.dusk.rankcolors.color.PlayerColorSelection;
import dev.dusk.rankcolors.config.MessageService;
import dev.dusk.rankcolors.config.PluginConfiguration;
import dev.dusk.rankcolors.hook.HookManager;
import dev.dusk.rankcolors.menu.MenuManager;
import dev.dusk.rankcolors.scheduler.SchedulerAdapter;
import dev.dusk.rankcolors.service.PlayerColorService;
import dev.dusk.rankcolors.service.RenderedPlayer;
import dev.dusk.rankcolors.service.SelectionValidator;
import dev.dusk.rankcolors.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class RankColorsCommand implements CommandExecutor, TabCompleter {
    private static final List<String> CATEGORIES = List.of("rank", "plus", "name");
    private final DuskRankColorsPlugin plugin;
    private final MessageService messages;
    private final PluginConfiguration configuration;
    private final ColorRegistry registry;
    private final PlayerColorService colors;
    private final MenuManager menus;
    private final HookManager hooks;
    private final SchedulerAdapter scheduler;

    public RankColorsCommand(DuskRankColorsPlugin plugin, MessageService messages, PluginConfiguration configuration,
                             ColorRegistry registry, PlayerColorService colors, MenuManager menus,
                             HookManager hooks, SchedulerAdapter scheduler) {
        this.plugin = plugin;
        this.messages = messages;
        this.configuration = configuration;
        this.registry = registry;
        this.colors = colors;
        this.menus = menus;
        this.hooks = hooks;
        this.scheduler = scheduler;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 0) {
            Player player = player(sender);
            if (player != null && require(sender, "duskrankcolors.gui")) menus.openMain(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> help(sender);
            case "preview" -> preview(sender);
            case "rank", "plus", "name" -> openCategory(sender, sub);
            case "rgb" -> playerRgb(sender, args);
            case "gradient" -> playerGradient(sender, args);
            case "reset" -> reset(sender, args);
            case "reload" -> reload(sender);
            case "info" -> info(sender);
            case "set" -> adminPreset(sender, args);
            case "setrgb" -> adminRgb(sender, args);
            case "setgradient" -> adminGradient(sender, args);
            default -> help(sender);
        }
        return true;
    }

    private void openCategory(CommandSender sender, String value) {
        Player player = player(sender);
        if (player == null) return;
        ColorCategory.parse(value).ifPresent(category -> menus.openType(player, category));
    }

    private void preview(CommandSender sender) {
        if (!require(sender, "duskrankcolors.preview")) return;
        Player player = player(sender);
        if (player == null) return;
        RenderedPlayer rendered = colors.rendered(player.getUniqueId());
        if (rendered == null) return;
        player.sendMessage(Component.text("━━━━━━━━━━ DuskRankColors ━━━━━━━━━━", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Rango: ", NamedTextColor.GRAY).append(rendered.rank()));
        player.sendMessage(Component.text("Plus: ", NamedTextColor.GRAY).append(rendered.plus()));
        player.sendMessage(Component.text("Nombre: ", NamedTextColor.GRAY).append(rendered.name()));
        player.sendMessage(Component.empty());
        player.sendMessage(rendered.full());
    }

    private void playerRgb(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null) return;
        if (args.length != 3) { sender.sendMessage(Text.legacy("&cUso: /rankcolors rgb <rank|plus|name> <#RRGGBB>")); return; }
        Optional<ColorCategory> category = category(sender, args[1]);
        if (category.isEmpty()) return;
        PlayerColorSelection selection;
        try { selection = PlayerColorSelection.rgb(args[2]); }
        catch (IllegalArgumentException exception) { messages.send(sender, "invalid-hex"); return; }
        select(player, category.get(), selection, false);
    }

    private void playerGradient(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null) return;
        if (args.length < 4) { sender.sendMessage(Text.legacy("&cUso: /rankcolors gradient <rank|plus|name> <hex1> <hex2> [hex3...]")); return; }
        Optional<ColorCategory> category = category(sender, args[1]);
        if (category.isEmpty()) return;
        PlayerColorSelection selection = gradient(sender, Arrays.copyOfRange(args, 2, args.length));
        if (selection != null) select(player, category.get(), selection, false);
    }

    private void reset(CommandSender sender, String[] args) {
        if (args.length >= 2 && ColorCategory.parse(args[1]).isEmpty()) {
            if (!require(sender, "duskrankcolors.admin.reset")) return;
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { messages.send(sender, "player-not-found"); return; }
            if (args.length < 3 || args[2].equalsIgnoreCase("all")) {
                colors.resetAll(target, ignored -> reply(sender, () -> messages.send(sender, "reset.all")));
            } else {
                Optional<ColorCategory> category = category(sender, args[2]);
                category.ifPresent(value -> colors.reset(target, value,
                    ignored -> reply(sender, () -> messages.send(sender, "reset." + value.key()))));
            }
            return;
        }
        if (!require(sender, "duskrankcolors.reset")) return;
        Player player = player(sender);
        if (player == null) return;
        if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
            colors.resetAll(player, ignored -> messages.send(player, "reset.all"));
        } else {
            Optional<ColorCategory> category = category(sender, args[1]);
            category.ifPresent(value -> colors.reset(player, value, ignored -> messages.send(player, "reset." + value.key())));
        }
    }

    private void reload(CommandSender sender) {
        if (!require(sender, "duskrankcolors.admin.reload")) return;
        plugin.reloadPluginConfiguration();
        messages.send(sender, "reloaded");
    }

    private void info(CommandSender sender) {
        if (!require(sender, "duskrankcolors.admin.info")) return;
        sender.sendMessage(Text.legacy("&6DuskRankColors &fv" + plugin.getPluginMeta().getVersion()));
        sender.sendMessage(Text.legacy("&7Platform: &f" + scheduler.platformName()));
        sender.sendMessage(Text.legacy("&7Server: &f" + Bukkit.getVersion()));
        sender.sendMessage(Text.legacy("&7PlaceholderAPI: &f" + (hooks.placeholderHooked() ? "hooked" : "not installed")));
        sender.sendMessage(Text.legacy("&7LuckPerms: &f" + (hooks.luckPermsHooked() ? "hooked" : "not installed")));
        sender.sendMessage(Text.legacy("&7Loaded colors: &f" + registry.size()));
        sender.sendMessage(Text.legacy("&7Cached players: &f" + colors.cachedPlayers()));
    }

    private void adminPreset(CommandSender sender, String[] args) {
        if (!require(sender, "duskrankcolors.admin.set")) return;
        if (args.length != 4) { sender.sendMessage(Text.legacy("&cUso: /rankcolors set <player> <rank|plus|name> <preset>")); return; }
        Player target = target(sender, args[1]);
        Optional<ColorCategory> category = category(sender, args[2]);
        if (target == null || category.isEmpty()) return;
        ColorDefinition definition = registry.get(args[3]).orElse(null);
        if (definition == null || !definition.allows(category.get())) { messages.send(sender, "invalid-color"); return; }
        selectAdmin(sender, target, category.get(), PlayerColorSelection.preset(definition.id()));
    }

    private void adminRgb(CommandSender sender, String[] args) {
        if (!require(sender, "duskrankcolors.admin.rgb")) return;
        if (args.length != 4) { sender.sendMessage(Text.legacy("&cUso: /rankcolors setrgb <player> <rank|plus|name> <#RRGGBB>")); return; }
        Player target = target(sender, args[1]);
        Optional<ColorCategory> category = category(sender, args[2]);
        if (target == null || category.isEmpty()) return;
        try { selectAdmin(sender, target, category.get(), PlayerColorSelection.rgb(args[3])); }
        catch (IllegalArgumentException exception) { messages.send(sender, "invalid-hex"); }
    }

    private void adminGradient(CommandSender sender, String[] args) {
        if (!require(sender, "duskrankcolors.admin.gradient")) return;
        if (args.length < 5) { sender.sendMessage(Text.legacy("&cUso: /rankcolors setgradient <player> <rank|plus|name> <hex1> <hex2> [hex3...]")); return; }
        Player target = target(sender, args[1]);
        Optional<ColorCategory> category = category(sender, args[2]);
        if (target == null || category.isEmpty()) return;
        PlayerColorSelection selection = gradient(sender, Arrays.copyOfRange(args, 3, args.length));
        if (selection != null) selectAdmin(sender, target, category.get(), selection);
    }

    private void selectAdmin(CommandSender sender, Player target, ColorCategory category, PlayerColorSelection selection) {
        colors.setSelection(target, category, selection, true, result -> {
            reply(sender, () -> {
                if (result == SelectionValidator.Result.OK) messages.send(sender, "selected." + category.key());
                else messages.send(sender, result == SelectionValidator.Result.MODE_DISABLED ? "mode-locked" : "invalid-color");
            });
        });
    }

    private void reply(CommandSender sender, Runnable message) {
        if (sender instanceof Player player) scheduler.runForPlayer(player, message);
        else scheduler.runGlobal(message);
    }

    private PlayerColorSelection gradient(CommandSender sender, String[] values) {
        if (values.length < configuration.minGradientColors() || values.length > configuration.maxGradientColors()) {
            messages.send(sender, "invalid-gradient");
            return null;
        }
        try { return PlayerColorSelection.gradient(GradientDefinition.fromHex(List.of(values))); }
        catch (IllegalArgumentException exception) { messages.send(sender, "invalid-gradient"); return null; }
    }

    private void select(Player player, ColorCategory category, PlayerColorSelection selection, boolean bypass) {
        colors.setSelection(player, category, selection, bypass, result -> {
            if (result == SelectionValidator.Result.OK) messages.send(player, "selected." + category.key());
            else if (result == SelectionValidator.Result.PERMISSION) messages.send(player, selection.mode() == dev.dusk.rankcolors.color.ColorMode.PRESET ? "color-locked" : "mode-locked");
            else messages.send(player, selection.mode() == dev.dusk.rankcolors.color.ColorMode.GRADIENT ? "invalid-gradient" : "invalid-color");
        });
    }

    private void help(CommandSender sender) {
        sender.sendMessage(Text.legacy("&6&lDuskRankColors &7- comandos"));
        sender.sendMessage(Text.legacy("&e/rankcolors &7- abrir menú"));
        sender.sendMessage(Text.legacy("&e/rankcolors <rank|plus|name>"));
        sender.sendMessage(Text.legacy("&e/rankcolors preview"));
        sender.sendMessage(Text.legacy("&e/rankcolors reset [rank|plus|name]"));
        sender.sendMessage(Text.legacy("&e/rankcolors rgb <categoría> <#RRGGBB>"));
        sender.sendMessage(Text.legacy("&e/rankcolors gradient <categoría> <hex1> <hex2> [hex...]"));
        if (sender.hasPermission("duskrankcolors.admin")) sender.sendMessage(Text.legacy("&cAdmin: &7reload, info, set, setrgb, setgradient, reset <player>"));
    }

    private Optional<ColorCategory> category(CommandSender sender, String value) {
        Optional<ColorCategory> category = ColorCategory.parse(value);
        if (category.isEmpty()) messages.send(sender, "invalid-category");
        return category;
    }

    private Player target(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) messages.send(sender, "player-not-found");
        return target;
    }

    private Player player(CommandSender sender) {
        if (sender instanceof Player player) return player;
        messages.send(sender, "player-only");
        return null;
    }

    private boolean require(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;
        messages.send(sender, "no-permission");
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return filter(args[0], List.of("rank", "plus", "name", "preview", "reset", "rgb", "gradient", "help", "reload", "info", "set", "setrgb", "setgradient"));
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ((sub.equals("rgb") || sub.equals("gradient")) && args.length == 2) return filter(args[1], CATEGORIES);
        if (sub.equals("rgb") && args.length == 3) return filter(args[2], List.of("#FFB224", "#55FFFF"));
        if (sub.equals("gradient") && args.length >= 3) return filter(args[args.length - 1], List.of("#FF55FF", "#55FFFF"));
        if ((sub.equals("set") || sub.equals("setrgb") || sub.equals("setgradient")) && args.length == 2) {
            return filter(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        if ((sub.equals("set") || sub.equals("setrgb") || sub.equals("setgradient")) && args.length == 3) return filter(args[2], CATEGORIES);
        if (sub.equals("set") && args.length == 4) {
            Optional<ColorCategory> category = ColorCategory.parse(args[2]);
            if (category.isPresent()) return filter(args[3], registry.forCategory(category.get()).stream().map(ColorDefinition::id).toList());
        }
        if (sub.equals("reset") && args.length == 2) {
            List<String> values = new ArrayList<>(CATEGORIES);
            values.add("all");
            if (sender.hasPermission("duskrankcolors.admin.reset")) values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return filter(args[1], values);
        }
        if (sub.equals("reset") && args.length == 3) return filter(args[2], List.of("rank", "plus", "name", "all"));
        return List.of();
    }

    private List<String> filter(String prefix, List<String> values) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized)).sorted().toList();
    }
}
