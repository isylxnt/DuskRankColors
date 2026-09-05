package dev.dusk.rankcolors.command;

import dev.dusk.rankcolors.DuskRankColorsPlugin;
import dev.dusk.rankcolors.api.DisplayContext;
import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.ColorDefinition;
import dev.dusk.rankcolors.color.ColorRegistry;
import dev.dusk.rankcolors.color.GradientDefinition;
import dev.dusk.rankcolors.color.PlayerColorSelection;
import dev.dusk.rankcolors.config.ConfigValidator;
import dev.dusk.rankcolors.config.MessageService;
import dev.dusk.rankcolors.config.PluginConfiguration;
import dev.dusk.rankcolors.config.SoundService;
import dev.dusk.rankcolors.hook.HookManager;
import dev.dusk.rankcolors.rank.RankDefinition;
import dev.dusk.rankcolors.rank.RankRegistry;
import dev.dusk.rankcolors.scheduler.SchedulerAdapter;
import dev.dusk.rankcolors.service.PlayerColorService;
import dev.dusk.rankcolors.service.RenderedPlayer;
import dev.dusk.rankcolors.service.SelectionValidator;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class AdminCommandHandler {
    private static final List<String> CATEGORIES = List.of("rank", "plus", "name");
    private final DuskRankColorsPlugin plugin;
    private final MessageService messages;
    private final SoundService sounds;
    private final PluginConfiguration configuration;
    private final ColorRegistry registry;
    private final RankRegistry ranks;
    private final PlayerColorService colors;
    private final HookManager hooks;
    private final SchedulerAdapter scheduler;
    private final ConfigValidator configValidator;

    AdminCommandHandler(DuskRankColorsPlugin plugin, MessageService messages, SoundService sounds,
                        PluginConfiguration configuration, ColorRegistry registry, RankRegistry ranks,
                        PlayerColorService colors, HookManager hooks, SchedulerAdapter scheduler,
                        ConfigValidator configValidator) {
        this.plugin = plugin;
        this.messages = messages;
        this.sounds = sounds;
        this.configuration = configuration;
        this.registry = registry;
        this.ranks = ranks;
        this.colors = colors;
        this.hooks = hooks;
        this.scheduler = scheduler;
        this.configValidator = configValidator;
    }

    void execute(CommandSender sender, String[] args) {
        if (args.length == 0) { usage(sender); return; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "info" -> info(sender);
            case "inspect" -> inspect(sender, args);
            case "validate" -> validate(sender, args);
            case "set" -> setPreset(sender, args);
            case "setrgb" -> setRgb(sender, args);
            case "setgradient" -> setGradient(sender, args);
            case "reset" -> reset(sender, args);
            default -> usage(sender);
        }
    }

    List<String> complete(CommandSender sender, String[] args) {
        if (!hasAnyPermission(sender)) return List.of();
        if (args.length == 1) return filter(args[0], subcommands(sender));
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ((sub.equals("set") || sub.equals("setrgb") || sub.equals("setgradient") || sub.equals("reset")
            || sub.equals("inspect")) && args.length == 2) {
            return filter(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        if ((sub.equals("set") || sub.equals("setrgb") || sub.equals("setgradient")) && args.length == 3) {
            return filter(args[2], CATEGORIES);
        }
        if (sub.equals("set") && args.length == 4) {
            Optional<ColorCategory> category = ColorCategory.parse(args[2]);
            if (category.isPresent()) {
                return filter(args[3], registry.forCategory(category.get()).stream().map(ColorDefinition::id).toList());
            }
        }
        if (sub.equals("setrgb") && args.length == 4) return filter(args[3], List.of("#FFB224", "#55FFFF"));
        if (sub.equals("setgradient") && args.length >= 4) {
            return filter(args[args.length - 1], List.of("#FF55FF", "#55FFFF"));
        }
        if (sub.equals("reset") && args.length == 3) return filter(args[2], List.of("rank", "plus", "name", "all"));
        return List.of();
    }

    boolean hasAnyPermission(CommandSender sender) {
        return sender.hasPermission("duskrankcolors.admin") || sender.hasPermission("duskrankcolors.admin.reload")
            || sender.hasPermission("duskrankcolors.admin.info") || sender.hasPermission("duskrankcolors.admin.set")
            || sender.hasPermission("duskrankcolors.admin.rgb") || sender.hasPermission("duskrankcolors.admin.gradient")
            || sender.hasPermission("duskrankcolors.admin.reset") || sender.hasPermission("duskrankcolors.admin.inspect")
            || sender.hasPermission("duskrankcolors.admin.validate");
    }

    private void reload(CommandSender sender) {
        if (!require(sender, "duskrankcolors.admin.reload")) return;
        scheduler.runGlobal(() -> {
            plugin.reloadPluginConfiguration();
            reply(sender, () -> messages.send(sender, "reloaded"));
        });
    }

    private void info(CommandSender sender) {
        if (!require(sender, "duskrankcolors.admin.info")) return;
        String active = messages.raw("admin.info.status.active");
        String inactive = messages.raw("admin.info.status.inactive");
        messages.send(sender, "admin.info.title", Map.of("version", plugin.getPluginMeta().getVersion()));
        messages.send(sender, "admin.info.platform", Map.of("platform", scheduler.platformName()));
        messages.send(sender, "admin.info.server", Map.of("server", Bukkit.getVersion()));
        messages.send(sender, "admin.info.placeholderapi", Map.of("status", hooks.placeholderHooked() ? active : inactive));
        messages.send(sender, "admin.info.colors", Map.of("count", Integer.toString(registry.size())));
        messages.send(sender, "admin.info.ranks", Map.of("count", Integer.toString(ranks.size())));
        messages.send(sender, "admin.info.cache", Map.of("count", Integer.toString(colors.cachedPlayers())));
    }

    private void inspect(CommandSender sender, String[] args) {
        if (!require(sender, "duskrankcolors.admin.inspect")) return;
        if (args.length != 2) { messages.send(sender, "usage.inspect"); return; }
        Player target = target(sender, args[1]);
        if (target != null) colors.synchronizeRank(target, () -> reply(sender, () -> sendInspection(sender, target)));
    }

    private void sendInspection(CommandSender sender, Player target) {
        RenderedPlayer rendered = colors.rendered(target.getUniqueId());
        if (rendered == null) { messages.send(sender, "player-not-found"); return; }
        RankDefinition rank = ranks.resolve(target);
        String none = messages.raw("admin.inspect.none");
        messages.send(sender, "admin.inspect.title", Map.of("player", target.getName()));
        messages.send(sender, "admin.inspect.rank", Map.of(
            "id", rank.id().isBlank() ? none : rank.id(),
            "display", rank.display().isBlank() ? none : rank.display(),
            "priority", rank.id().isBlank() ? none : Integer.toString(rank.priority())));
        messages.send(sender, "admin.inspect.permission", Map.of(
            "permission", rank.permission().isBlank() ? none : rank.permission()));
        messages.send(sender, "admin.inspect.flags", Map.of(
            "plus", Boolean.toString(rendered.rankHasPlus()), "bold", Boolean.toString(rendered.rankBold())));
        for (ColorCategory category : ColorCategory.values()) {
            PlayerColorSelection selection = rendered.settings().selection(category);
            messages.send(sender, "admin.inspect.selection", Map.of(
                "category", category.name(), "mode", selection.mode().name(), "value", selection.rawValue()));
        }
        for (DisplayContext context : DisplayContext.values()) {
            sender.sendMessage(messages.component("admin.inspect.context", Map.of("context", context.name()))
                .append(rendered.contexts().get(context)));
        }
    }

    private void validate(CommandSender sender, String[] args) {
        if (!require(sender, "duskrankcolors.admin.validate")) return;
        if (args.length != 1) { messages.send(sender, "usage.validate"); return; }
        ConfigValidator.Report report = configValidator.validate();
        messages.send(sender, "admin.validate.title", Map.of(
            "errors", Long.toString(report.errors()), "warnings", Long.toString(report.warnings())));
        if (report.issues().isEmpty()) { messages.send(sender, "admin.validate.clean"); return; }
        for (ConfigValidator.Issue issue : report.issues()) {
            String level = messages.raw("admin.validate.level." + issue.severity().name().toLowerCase(Locale.ROOT));
            messages.send(sender, "admin.validate.issue", Map.of(
                "level", level, "path", issue.path(), "message", issue.message()));
        }
    }

    private void reset(CommandSender sender, String[] args) {
        if (!require(sender, "duskrankcolors.admin.reset")) return;
        if (args.length < 2 || args.length > 3) { messages.send(sender, "usage.reset"); return; }
        Player target = target(sender, args[1]);
        if (target == null) return;
        if (args.length == 2 || args[2].equalsIgnoreCase("all")) {
            colors.resetAll(target, changed -> { if (changed) reply(sender, () -> messages.send(sender, "reset.all")); });
            return;
        }
        category(sender, args[2]).ifPresent(value -> colors.reset(target, value, changed -> {
            if (changed) reply(sender, () -> messages.send(sender, "reset." + value.key()));
        }));
    }

    private void setPreset(CommandSender sender, String[] args) {
        if (!require(sender, "duskrankcolors.admin.set")) return;
        if (args.length != 4) { messages.send(sender, "usage.set"); return; }
        Player target = target(sender, args[1]);
        Optional<ColorCategory> category = category(sender, args[2]);
        if (target == null || category.isEmpty()) return;
        ColorDefinition definition = registry.get(args[3]).orElse(null);
        if (definition == null || !definition.allows(category.get())) { messages.send(sender, "invalid-color"); return; }
        select(sender, target, category.get(), PlayerColorSelection.preset(definition.id()));
    }

    private void setRgb(CommandSender sender, String[] args) {
        if (!require(sender, "duskrankcolors.admin.rgb")) return;
        if (args.length != 4) { messages.send(sender, "usage.setrgb"); return; }
        Player target = target(sender, args[1]);
        Optional<ColorCategory> category = category(sender, args[2]);
        if (target == null || category.isEmpty()) return;
        try { select(sender, target, category.get(), PlayerColorSelection.rgb(args[3])); }
        catch (IllegalArgumentException exception) { messages.send(sender, "invalid-hex"); }
    }

    private void setGradient(CommandSender sender, String[] args) {
        if (!require(sender, "duskrankcolors.admin.gradient")) return;
        if (args.length < 5) { messages.send(sender, "usage.setgradient"); return; }
        Player target = target(sender, args[1]);
        Optional<ColorCategory> category = category(sender, args[2]);
        if (target == null || category.isEmpty()) return;
        PlayerColorSelection selection = gradient(sender, Arrays.copyOfRange(args, 3, args.length));
        if (selection != null) select(sender, target, category.get(), selection);
    }

    private void select(CommandSender sender, Player target, ColorCategory category, PlayerColorSelection selection) {
        colors.setSelection(target, category, selection, true, result -> reply(sender, () -> {
            if (result == SelectionValidator.Result.OK) messages.send(sender, "selected." + category.key());
            else messages.send(sender, result == SelectionValidator.Result.MODE_DISABLED ? "mode-locked" : "invalid-color");
        }));
    }

    private PlayerColorSelection gradient(CommandSender sender, String[] values) {
        if (values.length < configuration.minGradientColors() || values.length > configuration.maxGradientColors()) {
            messages.send(sender, "invalid-gradient");
            return null;
        }
        try { return PlayerColorSelection.gradient(GradientDefinition.fromHex(List.of(values))); }
        catch (IllegalArgumentException exception) { messages.send(sender, "invalid-gradient"); return null; }
    }

    private void usage(CommandSender sender) {
        if (!hasAnyPermission(sender)) { messages.send(sender, "no-permission"); return; }
        messages.send(sender, "admin.usage.title");
        messages.send(sender, "admin.usage.commands");
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

    private boolean require(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;
        messages.send(sender, "no-permission");
        if (sender instanceof Player player) sounds.play(player, "denied");
        return false;
    }

    private void reply(CommandSender sender, Runnable message) {
        if (sender instanceof Player player) scheduler.runForPlayer(player, message);
        else scheduler.runGlobal(message);
    }

    private List<String> subcommands(CommandSender sender) {
        ArrayList<String> commands = new ArrayList<>();
        if (sender.hasPermission("duskrankcolors.admin.reload")) commands.add("reload");
        if (sender.hasPermission("duskrankcolors.admin.info")) commands.add("info");
        if (sender.hasPermission("duskrankcolors.admin.inspect")) commands.add("inspect");
        if (sender.hasPermission("duskrankcolors.admin.validate")) commands.add("validate");
        if (sender.hasPermission("duskrankcolors.admin.set")) commands.add("set");
        if (sender.hasPermission("duskrankcolors.admin.rgb")) commands.add("setrgb");
        if (sender.hasPermission("duskrankcolors.admin.gradient")) commands.add("setgradient");
        if (sender.hasPermission("duskrankcolors.admin.reset")) commands.add("reset");
        return commands;
    }

    private List<String> filter(String prefix, List<String> values) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized)).sorted().toList();
    }
}
