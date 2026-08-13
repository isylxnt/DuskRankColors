package dev.dusk.rankcolors;

import dev.dusk.rankcolors.api.DuskRankColorsAPI;
import dev.dusk.rankcolors.api.DuskRankColorsApiImpl;
import dev.dusk.rankcolors.color.ColorFormatter;
import dev.dusk.rankcolors.color.ColorRegistry;
import dev.dusk.rankcolors.color.GradientFormatter;
import dev.dusk.rankcolors.command.RankColorsCommand;
import dev.dusk.rankcolors.config.ConfigManager;
import dev.dusk.rankcolors.config.MessageService;
import dev.dusk.rankcolors.config.PluginConfiguration;
import dev.dusk.rankcolors.hook.HookManager;
import dev.dusk.rankcolors.hook.RankProvider;
import dev.dusk.rankcolors.input.InputManager;
import dev.dusk.rankcolors.listener.InventoryListener;
import dev.dusk.rankcolors.listener.PlayerListener;
import dev.dusk.rankcolors.menu.MenuManager;
import dev.dusk.rankcolors.scheduler.SchedulerAdapter;
import dev.dusk.rankcolors.scheduler.SchedulerFactory;
import dev.dusk.rankcolors.service.PlayerColorService;
import dev.dusk.rankcolors.service.SelectionValidator;
import dev.dusk.rankcolors.storage.GradientSerializer;
import dev.dusk.rankcolors.storage.PlayerColorRepository;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class DuskRankColorsPlugin extends JavaPlugin {
    private ConfigManager configs;
    private PluginConfiguration configuration;
    private ColorRegistry registry;
    private SchedulerAdapter scheduler;
    private HookManager hooks;
    private PlayerColorService colors;
    private InputManager inputs;
    private MenuManager menus;
    private DuskRankColorsAPI api;

    @Override
    public void onEnable() {
        getLogger().info("Enabling DuskRankColors v" + getPluginMeta().getVersion() + "...");
        configs = new ConfigManager(this);
        configs.load();
        GradientSerializer gradientSerializer = new GradientSerializer();
        configuration = new PluginConfiguration(configs, gradientSerializer);
        configuration.reload();
        registry = new ColorRegistry(configs, getLogger());
        registry.reload();
        scheduler = SchedulerFactory.create(this);
        hooks = new HookManager(this, configuration);
        RankProvider rankProvider = hooks.discoverRankProvider();
        ColorFormatter formatter = new ColorFormatter(registry, new GradientFormatter());
        PlayerColorRepository repository = new PlayerColorRepository(this, configuration, gradientSerializer);
        SelectionValidator validator = new SelectionValidator(configuration, registry);
        colors = new PlayerColorService(this, configuration, repository, formatter, validator, scheduler, rankProvider);
        MessageService messages = new MessageService(configs);
        inputs = new InputManager(configuration, messages, scheduler, colors);
        menus = new MenuManager(configs, configuration, messages, registry, formatter, colors, inputs);
        inputs.bindMenus(menus);

        Bukkit.getPluginManager().registerEvents(new PlayerListener(colors, menus), this);
        Bukkit.getPluginManager().registerEvents(new InventoryListener(menus, inputs, scheduler), this);
        Bukkit.getPluginManager().registerEvents(inputs, this);
        PluginCommand command = getCommand("rankcolors");
        if (command == null) throw new IllegalStateException("rankcolors command is missing from plugin.yml");
        RankColorsCommand handler = new RankColorsCommand(this, messages, configuration, registry, colors, menus, hooks, scheduler);
        command.setExecutor(handler);
        command.setTabCompleter(handler);

        api = new DuskRankColorsApiImpl(colors);
        Bukkit.getServicesManager().register(DuskRankColorsAPI.class, api, this, ServicePriority.Normal);
        hooks.hookPlaceholderApi(colors);
        Bukkit.getOnlinePlayers().forEach(colors::load);

        getLogger().info("Loaded " + registry.size() + " preset colors.");
        getLogger().info("PlaceholderAPI: " + (hooks.placeholderHooked() ? "hooked." : "not installed/disabled."));
        getLogger().info("LuckPerms: " + (hooks.luckPermsHooked() ? "hooked." : "not installed/disabled."));
        getLogger().info("Platform: " + scheduler.platformName() + ".");
        getLogger().info("Enabled successfully.");
    }

    public void reloadPluginConfiguration() {
        configs.load();
        configuration.reload();
        registry.reload();
        Bukkit.getOnlinePlayers().forEach(colors::reload);
        getLogger().info("Reloaded configuration and " + registry.size() + " preset colors.");
    }

    public DuskRankColorsAPI api() {
        return api;
    }

    @Override
    public void onDisable() {
        if (hooks != null) hooks.shutdown();
        Bukkit.getServicesManager().unregisterAll(this);
        if (inputs != null) inputs.clear();
        if (menus != null) menus.clear();
        if (colors != null) colors.clear();
        if (scheduler != null) scheduler.shutdown();
    }
}
