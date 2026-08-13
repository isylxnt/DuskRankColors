package dev.dusk.rankcolors.menu;

import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.ColorDefinition;
import dev.dusk.rankcolors.color.ColorFormatter;
import dev.dusk.rankcolors.color.ColorMode;
import dev.dusk.rankcolors.color.ColorRegistry;
import dev.dusk.rankcolors.color.PlayerColorSelection;
import dev.dusk.rankcolors.config.ConfigManager;
import dev.dusk.rankcolors.config.MessageService;
import dev.dusk.rankcolors.config.PluginConfiguration;
import dev.dusk.rankcolors.input.GradientDraft;
import dev.dusk.rankcolors.input.InputManager;
import dev.dusk.rankcolors.service.PlayerColorService;
import dev.dusk.rankcolors.service.RenderedPlayer;
import dev.dusk.rankcolors.service.SelectionValidator;
import dev.dusk.rankcolors.util.ItemBuilder;
import dev.dusk.rankcolors.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuManager {
    private final ConfigManager configs;
    private final PluginConfiguration configuration;
    private final MessageService messages;
    private final ColorRegistry registry;
    private final ColorFormatter formatter;
    private final PlayerColorService colors;
    private final InputManager inputs;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public MenuManager(ConfigManager configs, PluginConfiguration configuration, MessageService messages,
                       ColorRegistry registry, ColorFormatter formatter, PlayerColorService colors, InputManager inputs) {
        this.configs = configs;
        this.configuration = configuration;
        this.messages = messages;
        this.registry = registry;
        this.formatter = formatter;
        this.colors = colors;
        this.inputs = inputs;
    }

    public void openMain(Player player) {
        long now = System.currentTimeMillis();
        Long previous = cooldowns.put(player.getUniqueId(), now);
        if (previous != null && now - previous < configuration.guiCooldownMillis()) return;
        openMainInternal(player);
    }

    private void openMainInternal(Player player) {
        RenderedPlayer rendered = colors.rendered(player.getUniqueId());
        if (rendered == null) {
            colors.load(player);
            return;
        }
        Inventory inventory = inventory("main", new MenuHolder(MenuHolder.MenuType.MAIN, null, 0, null));
        decorate(inventory, "main");
        for (ColorCategory category : ColorCategory.values()) {
            ConfigurationSection button = configs.menus().getConfigurationSection("main." + category.key());
            int slot = safeSlot(button, switch (category) { case RANK -> 20; case PLUS -> 22; case NAME -> 24; }, inventory);
            String current = formatter.displayValue(rendered.settings().selection(category));
            ItemStack item = ItemBuilder.configured(button, category == ColorCategory.NAME ? Material.PLAYER_HEAD : Material.PAPER,
                Map.of("current", current));
            if (category == ColorCategory.NAME && item.getType() == Material.PLAYER_HEAD) {
                item = ItemBuilder.playerHead(player, item.getItemMeta().displayName(), item.getItemMeta().lore());
            }
            inventory.setItem(slot, item);
        }
        ConfigurationSection preview = configs.menus().getConfigurationSection("main.preview");
        inventory.setItem(safeSlot(preview, 31, inventory), ItemBuilder.configured(preview, Material.PAPER,
            Map.of("preview", rendered.fullLegacy())));
        player.openInventory(inventory);
        sound(player, "open");
    }

    public void openType(Player player, ColorCategory category) {
        if (!player.hasPermission("duskrankcolors." + category.key())) {
            deny(player, "no-permission");
            return;
        }
        Inventory inventory = inventory("selection-type", new MenuHolder(MenuHolder.MenuType.TYPE, category, 0, null));
        decorate(inventory, "selection-type");
        PlayerColorSelection current = colors.settings(player.getUniqueId()).selection(category);
        addModeButton(inventory, player, category, ColorMode.PRESET, "preset", 20, current);
        addModeButton(inventory, player, category, ColorMode.RGB, "rgb", 22, current);
        addModeButton(inventory, player, category, ColorMode.GRADIENT, "gradient", 24, current);
        ConfigurationSection preview = configs.menus().getConfigurationSection("selection-type.preview");
        inventory.setItem(safeSlot(preview, 31, inventory), ItemBuilder.configured(preview, Material.PAPER,
            Map.of("current", formatter.displayValue(current), "mode", current.mode().name(),
                "preview", Text.section(formatText(player, category, current)))));
        ConfigurationSection back = configs.menus().getConfigurationSection("selection-type.back");
        inventory.setItem(safeSlot(back, 40, inventory), ItemBuilder.configured(back, Material.ARROW, Map.of()));
        player.openInventory(inventory);
    }

    private void addModeButton(Inventory inventory, Player player, ColorCategory category, ColorMode mode,
                               String path, int fallbackSlot, PlayerColorSelection current) {
        if (!configuration.modeEnabled(category, mode)) return;
        boolean permitted = mode == ColorMode.PRESET || !configuration.validatePermissions()
            || player.hasPermission(category.permission(mode.name().toLowerCase(Locale.ROOT)));
        if (!permitted && !configuration.showLockedMode(mode)) return;
        ConfigurationSection section = configs.menus().getConfigurationSection("selection-type." + path);
        String status = permitted
            ? (current.mode() == mode ? "&a✔ Actualmente seleccionado" : "&eClick para configurar.")
            : "&c✘ Bloqueado";
        ItemStack base = ItemBuilder.configured(section, Material.PAPER, Map.of(
            "current", formatter.displayValue(current), "mode", current.mode().name(), "status", status));
        List<Component> lore = base.getItemMeta().lore();
        if (lore == null || lore.isEmpty()) {
            lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Modo actual: ", NamedTextColor.GRAY).append(Component.text(current.mode().name(), NamedTextColor.WHITE)));
            lore.add(Component.text("Valor: ", NamedTextColor.GRAY).append(Component.text(formatter.displayValue(current), NamedTextColor.WHITE)));
            lore.add(Component.empty());
            lore.add(Component.text(Text.plain(Text.legacy(status)), permitted ? NamedTextColor.GREEN : NamedTextColor.RED));
        }
        inventory.setItem(safeSlot(section, fallbackSlot, inventory), ItemBuilder.create(base.getType(), base.getItemMeta().displayName(), lore));
    }

    public void openPresets(Player player, ColorCategory category, int requestedPage) {
        List<ColorDefinition> available = new ArrayList<>();
        boolean showLocked = configs.menus().getBoolean("preset-menu.locked-colors.show", true);
        for (ColorDefinition definition : registry.forCategory(category)) {
            if (showLocked || canUse(player, category, definition)) available.add(definition);
        }
        List<Integer> slots = configs.menus().getIntegerList("preset-menu.color-slots");
        if (slots.isEmpty()) slots = List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34);
        int pages = Math.max(1, (available.size() + slots.size() - 1) / slots.size());
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        Inventory inventory = inventory("preset-menu", new MenuHolder(MenuHolder.MenuType.PRESETS, category, page, null));
        decorate(inventory, "preset-menu");
        PlayerColorSelection selected = colors.settings(player.getUniqueId()).selection(category);
        int start = page * slots.size();
        for (int index = 0; index < slots.size() && start + index < available.size(); index++) {
            int slot = slots.get(index);
            if (slot < 0 || slot >= inventory.getSize()) continue;
            ColorDefinition definition = available.get(start + index);
            boolean permitted = canUse(player, category, definition);
            boolean current = selected.mode() == ColorMode.PRESET && selected.presetId().equals(definition.id());
            Material lockedMaterial = material("preset-menu.locked-colors.material", Material.BARRIER);
            List<Component> lore = List.of(Component.empty(), Component.text("Vista previa: ", NamedTextColor.GRAY)
                    .append(Component.text(sample(player, category), definition.textColor())), Component.empty(),
                Component.text(permitted ? (current ? "✔ SELECCIONADO" : "Click para seleccionar.") : "✘ BLOQUEADO",
                    permitted ? NamedTextColor.GREEN : NamedTextColor.RED));
            inventory.setItem(slot, ItemBuilder.create(permitted ? definition.material() : lockedMaterial, definition.displayName(), lore));
        }
        addNav(inventory, "preset-menu.previous", page > 0);
        addNav(inventory, "preset-menu.back", true);
        addNav(inventory, "preset-menu.next", page + 1 < pages);
        player.openInventory(inventory);
    }

    public void openRgbConfirmation(Player player, ColorCategory category, String hex) {
        Inventory inventory = inventory("confirmation", new MenuHolder(MenuHolder.MenuType.RGB_CONFIRMATION, category, 0, hex));
        decorate(inventory, "confirmation");
        putConfigured(inventory, "confirmation.preview", 13, Material.FIREWORK_STAR, Map.of("hex", hex));
        putConfigured(inventory, "confirmation.confirm", 11, Material.LIME_CONCRETE, Map.of("hex", hex));
        putConfigured(inventory, "confirmation.cancel", 15, Material.RED_CONCRETE, Map.of("hex", hex));
        player.openInventory(inventory);
    }

    public void openGradient(Player player, ColorCategory category) {
        GradientDraft draft = inputs.draftFor(player, category);
        Inventory inventory = inventory("gradient-menu", new MenuHolder(MenuHolder.MenuType.GRADIENT, category, 0, null));
        decorate(inventory, "gradient-menu");
        List<Integer> slots = configs.menus().getIntegerList("gradient-menu.color-slots");
        if (slots.isEmpty()) slots = List.of(19, 21, 23, 25, 31);
        for (int index = 0; index < draft.stops().size() && index < slots.size(); index++) {
            if (slots.get(index) < 0 || slots.get(index) >= inventory.getSize()) continue;
            String hex = draft.stops().get(index);
            List<Component> lore = List.of(Component.empty(), Component.text("Actual: ", NamedTextColor.GRAY)
                    .append(Component.text(hex, dev.dusk.rankcolors.color.HexColor.toTextColor(hex))), Component.empty(),
                Component.text("Click izquierdo para cambiar.", NamedTextColor.YELLOW),
                Component.text("Click derecho para eliminar.", NamedTextColor.RED));
            inventory.setItem(slots.get(index), ItemBuilder.create(Material.FIREWORK_STAR,
                Component.text("COLOR #" + (index + 1), dev.dusk.rankcolors.color.HexColor.toTextColor(hex)), lore));
        }
        if (draft.stops().size() < configuration.maxGradientColors()) putConfigured(inventory, "gradient-menu.add", 33, Material.LIME_DYE, Map.of());
        ConfigurationSection preview = configs.menus().getConfigurationSection("gradient-menu.preview");
        inventory.setItem(safeSlot(preview, 40, inventory), ItemBuilder.create(material(preview, Material.PAPER),
            Text.legacy(preview == null ? "&fVista previa" : preview.getString("name", "&fVista previa")),
            List.of(Component.empty(), formatter.format(sample(player, category), PlayerColorSelection.gradient(draft.definition())))));
        putConfigured(inventory, "gradient-menu.confirm", 48, Material.LIME_CONCRETE, Map.of());
        putConfigured(inventory, "gradient-menu.cancel", 50, Material.RED_CONCRETE, Map.of());
        player.openInventory(inventory);
    }

    public void click(Player player, MenuHolder holder, int slot, ClickType click) {
        switch (holder.type()) {
            case MAIN -> clickMain(player, slot);
            case TYPE -> clickType(player, holder.category(), slot);
            case PRESETS -> clickPreset(player, holder.category(), holder.page(), slot);
            case RGB_CONFIRMATION -> clickConfirmation(player, holder, slot);
            case GRADIENT -> clickGradient(player, holder.category(), slot, click);
        }
    }

    private void clickMain(Player player, int slot) {
        for (ColorCategory category : ColorCategory.values()) {
            ConfigurationSection section = configs.menus().getConfigurationSection("main." + category.key());
            if (slot == (section == null ? -1 : section.getInt("slot"))) { openType(player, category); return; }
        }
    }

    private void clickType(Player player, ColorCategory category, int slot) {
        if (slot("selection-type.back", 40) == slot) { openMainInternal(player); return; }
        if (slot("selection-type.preset", 20) == slot) { openPresets(player, category, 0); return; }
        if (slot("selection-type.rgb", 22) == slot) {
            if (canUseMode(player, category, ColorMode.RGB)) inputs.requestRgb(player, category); else deny(player, "mode-locked");
            return;
        }
        if (slot("selection-type.gradient", 24) == slot) {
            if (canUseMode(player, category, ColorMode.GRADIENT)) openGradient(player, category); else deny(player, "mode-locked");
        }
    }

    private void clickPreset(Player player, ColorCategory category, int page, int slot) {
        if (slot("preset-menu.back", 49) == slot) { openType(player, category); return; }
        if (slot("preset-menu.previous", 45) == slot) { openPresets(player, category, page - 1); return; }
        if (slot("preset-menu.next", 53) == slot) { openPresets(player, category, page + 1); return; }
        List<Integer> slots = configs.menus().getIntegerList("preset-menu.color-slots");
        if (slots.isEmpty()) slots = List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34);
        int local = slots.indexOf(slot);
        if (local < 0) return;
        List<ColorDefinition> shown = new ArrayList<>();
        boolean showLocked = configs.menus().getBoolean("preset-menu.locked-colors.show", true);
        for (ColorDefinition definition : registry.forCategory(category)) if (showLocked || canUse(player, category, definition)) shown.add(definition);
        int index = page * slots.size() + local;
        if (index >= shown.size()) return;
        ColorDefinition definition = shown.get(index);
        colors.setSelection(player, category, PlayerColorSelection.preset(definition.id()), false, result -> {
            if (result == SelectionValidator.Result.OK) { messages.send(player, "selected." + category.key()); sound(player, "select"); openPresets(player, category, page); }
            else deny(player, result == SelectionValidator.Result.PERMISSION ? "color-locked" : "invalid-color");
        });
    }

    private void clickConfirmation(Player player, MenuHolder holder, int slot) {
        if (slot("confirmation.cancel", 15) == slot) { openType(player, holder.category()); return; }
        if (slot("confirmation.confirm", 11) != slot) return;
        colors.setSelection(player, holder.category(), PlayerColorSelection.rgb(holder.value()), false, result -> {
            if (result == SelectionValidator.Result.OK) { messages.send(player, "selected." + holder.category().key()); sound(player, "select"); openType(player, holder.category()); }
            else deny(player, result == SelectionValidator.Result.PERMISSION ? "mode-locked" : "invalid-hex");
        });
    }

    private void clickGradient(Player player, ColorCategory category, int slot, ClickType click) {
        GradientDraft draft = inputs.draftFor(player, category);
        List<Integer> slots = configs.menus().getIntegerList("gradient-menu.color-slots");
        if (slots.isEmpty()) slots = List.of(19, 21, 23, 25, 31);
        int stop = slots.indexOf(slot);
        if (stop >= 0 && stop < draft.stops().size()) {
            if (click.isRightClick()) {
                if (draft.stops().size() <= configuration.minGradientColors()) { deny(player, "invalid-gradient"); return; }
                draft.remove(stop);
                openGradient(player, category);
            } else inputs.requestGradientStop(player, draft, stop);
            return;
        }
        if (slot("gradient-menu.add", 33) == slot && draft.stops().size() < configuration.maxGradientColors()) {
            inputs.requestGradientStop(player, draft, -1);
        } else if (slot("gradient-menu.cancel", 50) == slot) {
            inputs.discardDraft(player.getUniqueId());
            openType(player, category);
        } else if (slot("gradient-menu.confirm", 48) == slot) {
            PlayerColorSelection selection = PlayerColorSelection.gradient(draft.definition());
            colors.setSelection(player, category, selection, false, result -> {
                if (result == SelectionValidator.Result.OK) { inputs.finishDraft(player.getUniqueId()); messages.send(player, "selected." + category.key()); sound(player, "select"); openType(player, category); }
                else deny(player, result == SelectionValidator.Result.PERMISSION ? "mode-locked" : "invalid-gradient");
            });
        }
    }

    public void clearPlayer(UUID uuid) { cooldowns.remove(uuid); inputs.discardDraft(uuid); }
    public void clear() { cooldowns.clear(); }

    private Component formatText(Player player, ColorCategory category, PlayerColorSelection selection) {
        return formatter.format(sample(player, category), selection);
    }

    private String sample(Player player, ColorCategory category) {
        RenderedPlayer rendered = colors.rendered(player.getUniqueId());
        return switch (category) {
            case RANK -> rendered == null ? "RANK" : rendered.rankText();
            case PLUS -> configuration.plusSymbol();
            case NAME -> player.getName();
        };
    }

    private boolean canUse(Player player, ColorCategory category, ColorDefinition definition) {
        return !configuration.validatePermissions() || player.hasPermission(definition.permission(category));
    }

    private boolean canUseMode(Player player, ColorCategory category, ColorMode mode) {
        return configuration.modeEnabled(category, mode) && (!configuration.validatePermissions()
            || player.hasPermission(category.permission(mode.name().toLowerCase(Locale.ROOT))));
    }

    private void deny(Player player, String path) { messages.send(player, path); sound(player, "denied"); }

    private Inventory inventory(String path, MenuHolder holder) {
        int rows = Math.max(1, Math.min(6, configs.menus().getInt(path + ".rows", path.equals("gradient-menu") ? 6 : 5)));
        Component title = Text.legacy(configs.menus().getString(path + ".title", "&8DuskRankColors"));
        Inventory inventory = Bukkit.createInventory(holder, rows * 9, title);
        holder.inventory(inventory);
        return inventory;
    }

    private void decorate(Inventory inventory, String path) {
        if (!configs.menus().getBoolean(path + ".decoration.enabled", configs.menus().getBoolean("main.decoration.enabled", true))) return;
        Material material = material(path + ".decoration.material", material("main.decoration.material", Material.GRAY_STAINED_GLASS_PANE));
        ItemStack pane = ItemBuilder.create(material, Component.empty(), List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, pane);
    }

    private void addNav(Inventory inventory, String path, boolean visible) {
        if (!visible) return;
        putConfigured(inventory, path, path.endsWith("previous") ? 45 : path.endsWith("next") ? 53 : 49,
            path.endsWith("back") ? Material.BARRIER : Material.ARROW, Map.of());
    }

    private void putConfigured(Inventory inventory, String path, int fallbackSlot, Material fallbackMaterial, Map<String, String> replacements) {
        ConfigurationSection section = configs.menus().getConfigurationSection(path);
        inventory.setItem(safeSlot(section, fallbackSlot, inventory), ItemBuilder.configured(section, fallbackMaterial, replacements));
    }

    private int slot(String path, int fallback) { return configs.menus().getInt(path + ".slot", fallback); }
    private int safeSlot(ConfigurationSection section, int fallback, Inventory inventory) {
        int value = section == null ? fallback : section.getInt("slot", fallback);
        return value < 0 || value >= inventory.getSize() ? Math.min(fallback, inventory.getSize() - 1) : value;
    }
    private Material material(String path, Material fallback) {
        String value = configs.menus().getString(path);
        Material material = value == null ? null : Material.matchMaterial(value);
        return material == null ? fallback : material;
    }
    private Material material(ConfigurationSection section, Material fallback) {
        if (section == null) return fallback;
        Material material = Material.matchMaterial(section.getString("material", fallback.name()));
        return material == null ? fallback : material;
    }

    private void sound(Player player, String type) {
        String root = "sounds." + type;
        if (!configs.menus().getBoolean(root + ".enabled", true)) return;
        String value = configs.menus().getString(root + ".sound");
        if (value == null) return;
        try {
            Sound sound = Sound.valueOf(value.toUpperCase(Locale.ROOT));
            player.playSound(player.getLocation(), sound, (float) configs.menus().getDouble(root + ".volume", 0.5),
                (float) configs.menus().getDouble(root + ".pitch", 1.0));
        } catch (IllegalArgumentException ignored) {
            // Invalid or unavailable sounds deliberately fall back to silence.
        }
    }
}
