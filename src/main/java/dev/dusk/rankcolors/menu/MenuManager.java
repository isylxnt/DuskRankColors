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
import dev.dusk.rankcolors.config.SoundService;
import dev.dusk.rankcolors.input.GradientDraft;
import dev.dusk.rankcolors.input.InputManager;
import dev.dusk.rankcolors.service.PlayerColorService;
import dev.dusk.rankcolors.service.RenderedPlayer;
import dev.dusk.rankcolors.service.SelectionValidator;
import dev.dusk.rankcolors.rank.RankRegistry;
import dev.dusk.rankcolors.util.ItemBuilder;
import dev.dusk.rankcolors.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
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
    private final SoundService sounds;
    private final ColorRegistry registry;
    private final ColorFormatter formatter;
    private final RankRegistry ranks;
    private final PlayerColorService colors;
    private final InputManager inputs;
    private final MenuLayout layout;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public MenuManager(ConfigManager configs, PluginConfiguration configuration, MessageService messages, SoundService sounds,
                       ColorRegistry registry, ColorFormatter formatter, RankRegistry ranks,
                       PlayerColorService colors, InputManager inputs) {
        this.configs = configs;
        this.configuration = configuration;
        this.messages = messages;
        this.sounds = sounds;
        this.registry = registry;
        this.formatter = formatter;
        this.ranks = ranks;
        this.colors = colors;
        this.inputs = inputs;
        this.layout = new MenuLayout(configs);
    }

    public void openMain(Player player) {
        long now = System.currentTimeMillis();
        Long previous = cooldowns.put(player.getUniqueId(), now);
        if (previous != null && now - previous < configuration.guiCooldownMillis()) return;
        colors.synchronizeRank(player, () -> openMainInternal(player));
    }

    private void openMainInternal(Player player) {
        RenderedPlayer rendered = colors.rendered(player.getUniqueId());
        if (rendered == null) {
            colors.load(player);
            return;
        }
        Inventory inventory = layout.inventory("main", new MenuHolder(MenuHolder.MenuType.MAIN, null, 0, null));
        layout.decorate(inventory, "main");
        for (ColorCategory category : ColorCategory.values()) {
            if (category == ColorCategory.RANK && rendered.rankText().isBlank()) continue;
            if (category == ColorCategory.PLUS && !rendered.rankHasPlus()) continue;
            ConfigurationSection button = configs.menus().getConfigurationSection("main." + category.key());
            int slot = layout.safeSlot(button, switch (category) { case RANK -> 10; case PLUS -> 12; case NAME -> 14; }, inventory);
            PlayerColorSelection selection = rendered.settings().selection(category);
            ItemStack item = ItemBuilder.configured(button, category == ColorCategory.NAME ? Material.PLAYER_HEAD : Material.PAPER,
                Map.of(), Map.of("current", coloredValue(selection)));
            if (category == ColorCategory.NAME && item.getType() == Material.PLAYER_HEAD) {
                item = ItemBuilder.playerHead(player, item.getItemMeta().displayName(), item.getItemMeta().lore());
            }
            if (item.getType() == Material.FIREWORK_STAR) {
                item = ItemBuilder.tintFireworkStar(item, formatter.describe(selection).hex());
            }
            inventory.setItem(slot, item);
        }
        ConfigurationSection preview = configs.menus().getConfigurationSection("main.preview");
        inventory.setItem(layout.safeSlot(preview, 16, inventory), ItemBuilder.configured(preview, Material.WRITABLE_BOOK,
            Map.of(), Map.of(
                "preview", rendered.full(),
                "chat_preview", rendered.contexts().get(dev.dusk.rankcolors.api.DisplayContext.CHAT),
                "tab_preview", rendered.contexts().get(dev.dusk.rankcolors.api.DisplayContext.TAB),
                "nametag_preview", rendered.contexts().get(dev.dusk.rankcolors.api.DisplayContext.NAMETAG),
                "scoreboard_preview", rendered.contexts().get(dev.dusk.rankcolors.api.DisplayContext.SCOREBOARD))));
        player.openInventory(inventory);
        sounds.play(player, "open");
    }

    public void openType(Player player, ColorCategory category) {
        if (!player.hasPermission("duskrankcolors." + category.key())) {
            deny(player, "no-permission");
            return;
        }
        Inventory inventory = layout.inventory("selection-type", new MenuHolder(MenuHolder.MenuType.TYPE, category, 0, null));
        layout.decorate(inventory, "selection-type");
        PlayerColorSelection current = colors.settings(player.getUniqueId()).selection(category);
        addModeButton(inventory, player, category, ColorMode.PRESET, "preset", 11, current);
        addModeButton(inventory, player, category, ColorMode.RGB, "rgb", 13, current);
        addModeButton(inventory, player, category, ColorMode.GRADIENT, "gradient", 15, current);
        ConfigurationSection preview = configs.menus().getConfigurationSection("selection-type.preview");
        ItemStack previewItem = ItemBuilder.configured(preview, Material.WRITABLE_BOOK,
            Map.of("mode", current.mode().name()), Map.of("current", coloredValue(current),
                "preview", formatText(player, category, current)));
        Component currentName = applySelectionStyle(previewItem.getItemMeta().displayName(), current);
        inventory.setItem(layout.safeSlot(preview, 26, inventory), ItemBuilder.create(previewItem.getType(), currentName,
            previewItem.getItemMeta().lore() == null ? List.of() : previewItem.getItemMeta().lore()));
        ConfigurationSection back = configs.menus().getConfigurationSection("selection-type.back");
        inventory.setItem(layout.safeSlot(back, 18, inventory), ItemBuilder.configured(back, Material.BARRIER, Map.of()));
        ConfigurationSection reset = configs.menus().getConfigurationSection("selection-type.reset");
        inventory.setItem(layout.safeSlot(reset, 22, inventory), ItemBuilder.configured(reset, Material.RED_DYE, Map.of()));
        player.openInventory(inventory);
    }

    private void addModeButton(Inventory inventory, Player player, ColorCategory category, ColorMode mode,
                               String path, int fallbackSlot, PlayerColorSelection current) {
        if (!configuration.modeEnabled(category, mode)) return;
        boolean permittedByRank = ranks.resolve(player).allowsMode(category, mode);
        boolean permitted = permittedByRank && (mode == ColorMode.PRESET || !configuration.validatePermissions()
            || player.hasPermission(category.permission(mode.name().toLowerCase(Locale.ROOT))));
        boolean selected = current.mode() == mode;
        if (!permitted && !selected && !configuration.showLockedMode(mode)) return;
        ConfigurationSection section = configs.menus().getConfigurationSection("selection-type." + path);
        String statusPath = selected ? "selected" : !permitted ? "locked" : "available";
        Component status = menuComponent("selection-type.status." + statusPath);
        ItemStack base = ItemBuilder.configured(section, Material.PAPER, Map.of("mode", current.mode().name()),
            Map.of("current", coloredValue(current), "status", status));
        List<Component> lore = base.getItemMeta().lore() == null ? List.of() : base.getItemMeta().lore();
        ItemStack item = ItemBuilder.create(base.getType(), base.getItemMeta().displayName(), lore);
        if (item.getType() == Material.FIREWORK_STAR) {
            item = ItemBuilder.tintFireworkStar(item, formatter.describe(current).hex());
        }
        inventory.setItem(layout.safeSlot(section, fallbackSlot, inventory), item);
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
        Inventory inventory = layout.inventory("preset-menu", new MenuHolder(MenuHolder.MenuType.PRESETS, category, page, null));
        layout.decorate(inventory, "preset-menu");
        PlayerColorSelection selected = colors.settings(player.getUniqueId()).selection(category);
        int start = page * slots.size();
        for (int index = 0; index < slots.size() && start + index < available.size(); index++) {
            int slot = slots.get(index);
            if (slot < 0 || slot >= inventory.getSize()) continue;
            ColorDefinition definition = available.get(start + index);
            boolean permitted = canUse(player, category, definition);
            boolean current = selected.mode() == ColorMode.PRESET && selected.presetId().equals(definition.id());
            Material lockedMaterial = layout.material("preset-menu.locked-colors.material", Material.BARRIER);
            String statusPath = !permitted ? "locked" : current ? "selected" : "available";
            ConfigurationSection colorItem = configs.menus().getConfigurationSection("preset-menu.color-item");
            ItemStack item = ItemBuilder.configured(colorItem, permitted ? definition.material() : lockedMaterial, Map.of(), Map.of(
                "display_name", definition.displayName(),
                "preview", Component.text(sample(player, category), definition.textColor()),
                "status", menuComponent("preset-menu.status." + statusPath)));
            if (permitted && item.getType() == Material.FIREWORK_STAR) {
                item = ItemBuilder.tintFireworkStar(item, definition.hex());
            }
            inventory.setItem(slot, item);
        }
        layout.addNavigation(inventory, "preset-menu.previous", page > 0);
        layout.addNavigation(inventory, "preset-menu.back", true);
        layout.addNavigation(inventory, "preset-menu.next", page + 1 < pages);
        player.openInventory(inventory);
    }

    public void openRgbConfirmation(Player player, ColorCategory category, String hex) {
        Inventory inventory = layout.inventory("confirmation", new MenuHolder(MenuHolder.MenuType.RGB_CONFIRMATION, category, 0, hex));
        layout.decorate(inventory, "confirmation");
        ConfigurationSection previewSection = configs.menus().getConfigurationSection("confirmation.preview");
        int previewSlot = layout.safeSlot(previewSection, 13, inventory);
        layout.putConfigured(inventory, "confirmation.preview", 13, Material.FIREWORK_STAR, Map.of("hex", hex));
        ItemStack preview = inventory.getItem(previewSlot);
        if (preview != null) inventory.setItem(previewSlot, ItemBuilder.tintFireworkStar(preview, hex));
        layout.putConfigured(inventory, "confirmation.confirm", 11, Material.LIME_CONCRETE, Map.of("hex", hex));
        layout.putConfigured(inventory, "confirmation.cancel", 15, Material.RED_CONCRETE, Map.of("hex", hex));
        player.openInventory(inventory);
    }

    public void openGradient(Player player, ColorCategory category) {
        GradientDraft draft = inputs.draftFor(player, category);
        Inventory inventory = layout.inventory("gradient-menu", new MenuHolder(MenuHolder.MenuType.GRADIENT, category, 0, null));
        layout.decorate(inventory, "gradient-menu");
        List<Integer> slots = gradientColorSlots(draft.stops().size());
        for (int index = 0; index < draft.stops().size() && index < slots.size(); index++) {
            if (slots.get(index) < 0 || slots.get(index) >= inventory.getSize()) continue;
            String hex = draft.stops().get(index);
            ConfigurationSection stopItem = configs.menus().getConfigurationSection("gradient-menu.stop-item");
            ItemStack configured = ItemBuilder.configured(stopItem, Material.FIREWORK_STAR,
                Map.of("index", Integer.toString(index + 1)),
                Map.of("hex", Component.text(hex, dev.dusk.rankcolors.color.HexColor.toTextColor(hex))));
            Component configuredName = configured.getItemMeta().displayName();
            Component tintedName = configuredName == null ? Component.empty()
                : configuredName.color(dev.dusk.rankcolors.color.HexColor.toTextColor(hex));
            List<Component> lore = configured.getItemMeta().lore() == null ? List.of() : configured.getItemMeta().lore();
            ItemStack star = ItemBuilder.create(configured.getType(), tintedName, lore);
            inventory.setItem(slots.get(index), ItemBuilder.tintFireworkStar(star, hex));
        }
        if (draft.stops().size() < configuration.maxGradientColors()) {
            layout.putConfigured(inventory, "gradient-menu.add", 31, Material.CYAN_DYE, Map.of());
        }
        layout.putConfigured(inventory, "gradient-menu.confirm", 35, Material.LIME_DYE, Map.of());
        layout.putConfigured(inventory, "gradient-menu.cancel", 27, Material.BARRIER, Map.of());
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
            if (slot == (section == null ? -1 : section.getInt("slot"))) {
                sounds.play(player, "navigation");
                openType(player, category);
                return;
            }
        }
    }

    private void clickType(Player player, ColorCategory category, int slot) {
        if (layout.slot("selection-type.back", 18) == slot) { openMainInternal(player); return; }
        if (layout.slot("selection-type.reset", 22) == slot) {
            colors.reset(player, category, changed -> {
                if (changed) {
                    messages.send(player, "reset." + category.key());
                    sounds.play(player, "reset");
                    openType(player, category);
                }
            });
            return;
        }
        if (layout.slot("selection-type.preset", 11) == slot) { sounds.play(player, "navigation"); openPresets(player, category, 0); return; }
        if (layout.slot("selection-type.rgb", 13) == slot) {
            if (canUseMode(player, category, ColorMode.RGB)) inputs.requestRgb(player, category); else deny(player, "mode-locked");
            return;
        }
        if (layout.slot("selection-type.gradient", 15) == slot) {
            if (canUseMode(player, category, ColorMode.GRADIENT)) {
                sounds.play(player, "navigation");
                openGradient(player, category);
            } else deny(player, "mode-locked");
        }
    }

    private void clickPreset(Player player, ColorCategory category, int page, int slot) {
        if (layout.slot("preset-menu.back", 49) == slot) { sounds.play(player, "navigation"); openType(player, category); return; }
        if (layout.slot("preset-menu.previous", 45) == slot) { sounds.play(player, "navigation"); openPresets(player, category, page - 1); return; }
        if (layout.slot("preset-menu.next", 53) == slot) { sounds.play(player, "navigation"); openPresets(player, category, page + 1); return; }
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
            if (result == SelectionValidator.Result.OK) { messages.send(player, "selected." + category.key()); sounds.play(player, "select"); openPresets(player, category, page); }
            else deny(player, selectionFailure(result, "color-locked", "invalid-color"));
        });
    }

    private void clickConfirmation(Player player, MenuHolder holder, int slot) {
        if (layout.slot("confirmation.cancel", 15) == slot) { sounds.play(player, "cancel"); openType(player, holder.category()); return; }
        if (layout.slot("confirmation.confirm", 11) != slot) return;
        colors.setSelection(player, holder.category(), PlayerColorSelection.rgb(holder.value()), false, result -> {
            if (result == SelectionValidator.Result.OK) { messages.send(player, "selected." + holder.category().key()); sounds.play(player, "confirm"); openType(player, holder.category()); }
            else deny(player, selectionFailure(result, "mode-locked", "invalid-hex"));
        });
    }

    private void clickGradient(Player player, ColorCategory category, int slot, ClickType click) {
        GradientDraft draft = inputs.draftFor(player, category);
        List<Integer> slots = gradientColorSlots(draft.stops().size());
        int stop = slots.indexOf(slot);
        if (stop >= 0 && stop < draft.stops().size()) {
            if (click.isRightClick()) {
                if (draft.stops().size() <= configuration.minGradientColors()) { deny(player, "invalid-gradient"); return; }
                draft.remove(stop);
                sounds.play(player, "navigation");
                openGradient(player, category);
            } else inputs.requestGradientStop(player, draft, stop);
            return;
        }
        if (layout.slot("gradient-menu.add", 31) == slot && draft.stops().size() < configuration.maxGradientColors()) {
            inputs.requestGradientStop(player, draft, -1);
        } else if (layout.slot("gradient-menu.cancel", 27) == slot) {
            inputs.discardDraft(player.getUniqueId());
            sounds.play(player, "cancel");
            openType(player, category);
        } else if (layout.slot("gradient-menu.confirm", 35) == slot) {
            PlayerColorSelection selection = PlayerColorSelection.gradient(draft.definition());
            colors.setSelection(player, category, selection, false, result -> {
                if (result == SelectionValidator.Result.OK) { inputs.finishDraft(player.getUniqueId()); messages.send(player, "selected." + category.key()); sounds.play(player, "confirm"); openType(player, category); }
                else deny(player, selectionFailure(result, "mode-locked", "invalid-gradient"));
            });
        }
    }

    public void clearPlayer(UUID uuid) { cooldowns.remove(uuid); inputs.discardDraft(uuid); }
    public void clear() { cooldowns.clear(); }

    private Component formatText(Player player, ColorCategory category, PlayerColorSelection selection) {
        return formatter.format(sample(player, category), selection);
    }

    private Component coloredValue(PlayerColorSelection selection) {
        return formatter.format(formatter.displayValue(selection), selection);
    }

    private List<Integer> gradientColorSlots(int stopCount) {
        return GradientSlotLayout.resolve(stopCount,
            configs.menus().getIntegerList("gradient-menu.color-slots." + stopCount));
    }

    private Component applySelectionStyle(Component configuredName, PlayerColorSelection selection) {
        Component result = formatter.format(Text.plain(configuredName), selection);
        for (TextDecoration decoration : TextDecoration.values()) {
            TextDecoration.State state = configuredName.decoration(decoration);
            if (state != TextDecoration.State.NOT_SET) result = result.decoration(decoration, state);
        }
        return result;
    }

    private Component menuComponent(String path) {
        return Text.legacy(configs.menus().getString(path, ""));
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
        return ranks.resolve(player).allowsColor(category, definition.id())
            && (!configuration.validatePermissions() || player.hasPermission(definition.permission(category)));
    }

    private boolean canUseMode(Player player, ColorCategory category, ColorMode mode) {
        return configuration.modeEnabled(category, mode) && ranks.resolve(player).allowsMode(category, mode)
            && (!configuration.validatePermissions()
            || player.hasPermission(category.permission(mode.name().toLowerCase(Locale.ROOT))));
    }

    private String selectionFailure(SelectionValidator.Result result, String permissionPath, String invalidPath) {
        if (result == SelectionValidator.Result.RANK_RESTRICTED) return "rank-restricted";
        return result == SelectionValidator.Result.PERMISSION ? permissionPath : invalidPath;
    }

    private void deny(Player player, String path) { messages.send(player, path); sounds.play(player, "denied"); }

}
