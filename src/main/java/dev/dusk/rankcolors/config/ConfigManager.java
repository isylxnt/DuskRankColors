package dev.dusk.rankcolors.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public final class ConfigManager {
    private final JavaPlugin plugin;
    private volatile YamlConfiguration config;
    private volatile YamlConfiguration colors;
    private volatile YamlConfiguration menus;
    private volatile YamlConfiguration messages;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        saveIfMissing("config.yml");
        saveIfMissing("colors.yml");
        saveIfMissing("menus.yml");
        saveIfMissing("messages.yml");
        config = loadFile("config.yml");
        colors = loadFile("colors.yml");
        menus = loadFile("menus.yml");
        messages = loadFile("messages.yml");
        applyCurrentFormat();
        applyInternalRanks();
        applyCurrentMenuLayout();
        applyEnglishDefaults();
        if (config.getInt("config-version", 0) != 3) {
            plugin.getLogger().warning("Unknown config-version. Existing values were preserved; compare with the bundled config.yml.");
        }
    }

    private void saveIfMissing(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) plugin.saveResource(name, false);
    }

    private YamlConfiguration loadFile(String name) {
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), name));
        try (InputStream resource = plugin.getResource(name)) {
            if (resource != null && (name.equals("menus.yml") || name.equals("messages.yml"))) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(resource, StandardCharsets.UTF_8));
                loaded.setDefaults(defaults);
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not close bundled " + name + ": " + exception.getMessage());
        }
        return loaded;
    }

    private void applyCurrentFormat() {
        if ("{rank} {plus} {name}".equals(config.getString("format.full"))) {
            config.set("format.full", "{rank}{plus} {name}");
            plugin.getLogger().info("Applied the compact rank-plus format in memory; config.yml was preserved.");
        }
    }

    private void applyInternalRanks() {
        if (config.isConfigurationSection("ranks")) {
            boolean migrated = false;
            if (config.isConfigurationSection("ranks.default")) {
                config.set("ranks.default", null);
                migrated = true;
            }
            org.bukkit.configuration.ConfigurationSection ranks = config.getConfigurationSection("ranks");
            if (ranks != null) {
                for (String id : ranks.getKeys(false)) {
                    String root = "ranks." + id;
                    if (!config.contains(root + ".priority", true) && config.contains(root + ".weight", true)) {
                        config.set(root + ".priority", config.getInt(root + ".weight"));
                        migrated = true;
                    }
                }
            }
            if (config.getInt("config-version", 0) < 3) config.set("config-version", 3);
            if (migrated) {
                plugin.getLogger().info("Removed the legacy default rank and migrated rank weight to priority in memory; config.yml was preserved.");
            }
            return;
        }
        org.bukkit.configuration.ConfigurationSection legacy = config.getConfigurationSection("rank.groups");
        if (legacy != null) {
            int priority = 0;
            for (String id : legacy.getKeys(false)) {
                if (id.equalsIgnoreCase("default")) continue;
                String root = "ranks." + id;
                String normalized = id.toLowerCase(java.util.Locale.ROOT);
                boolean plus = normalized.contains("plus") || normalized.endsWith("+");
                config.set(root + ".display", legacy.getString(id, id));
                config.set(root + ".color", "&f");
                config.set(root + ".plus", plus);
                config.set(root + ".plus-color", plus ? "&6" : "");
                config.set(root + ".bold", true);
                config.set(root + ".priority", priority++);
            }
        }
        config.set("config-version", 3);
        plugin.getLogger().info("Migrated legacy group display mappings to prioritized permission ranks without a default rank in memory; config.yml was preserved.");
    }

    private void applyCurrentMenuLayout() {
        int version = menus.contains("menus-version", true) ? menus.getInt("menus-version", 0) : 0;
        boolean migrated = false;

        if (version < 2) {
            // Only replace known v1 defaults. Values customized by an administrator are preserved.
            migrated |= replaceLegacyValue("main.rows", 5, 3);
            migrated |= replaceLegacyValue("main.decoration.enabled", true, false);
            migrated |= replaceLegacyValue("main.rank.slot", 20, 10);
            migrated |= replaceLegacyValue("main.plus.slot", 22, 12);
            migrated |= replaceLegacyValue("main.plus.material", "NETHER_STAR", "FIREWORK_STAR");
            migrated |= replaceLegacyValue("main.name.slot", 24, 14);
            migrated |= replaceLegacyValue("main.preview.slot", 31, 16);
            migrated |= replaceLegacyValue("main.preview.material", "PAPER", "WRITABLE_BOOK");

            migrated |= replaceLegacyValue("selection-type.rows", 5, 3);
            migrated |= replaceLegacyValue("selection-type.preset.slot", 20, 11);
            migrated |= replaceLegacyValue("selection-type.rgb.slot", 22, 13);
            migrated |= replaceLegacyValue("selection-type.gradient.slot", 24, 15);
            migrated |= replaceLegacyValue("selection-type.gradient.material", "SPECTRAL_ARROW", "BLAZE_ROD");
            migrated |= replaceLegacyValue("selection-type.preview.slot", 31, 26);
            migrated |= replaceLegacyValue("selection-type.preview.material", "PLAYER_HEAD", "WRITABLE_BOOK");
            migrated |= replaceLegacyValue("selection-type.back.slot", 40, 18);
            migrated |= replaceLegacyValue("selection-type.back.material", "ARROW", "BARRIER");
            migrated |= replaceLegacyValue("selection-type.back.name", "&cVolver", "&cVolver al menú principal");
        }

        if (version < 3) {
            migrated |= replaceLegacyValue("gradient-menu.rows", 6, 4);
            if (menus.contains("gradient-menu.color-slots", true)
                && menus.getIntegerList("gradient-menu.color-slots").equals(List.of(19, 21, 23, 25, 31))) {
                menus.set("gradient-menu.color-slots", null);
                migrated = true;
            }
            migrated |= replaceLegacyValue("gradient-menu.add.slot", 33, 31);
            migrated |= replaceLegacyValue("gradient-menu.add.material", "LIME_DYE", "CYAN_DYE");
            migrated |= replaceLegacyValue("gradient-menu.add.name", "&a&l+ AÑADIR COLOR", "&b&lAÑADIR COLOR");
            migrated |= replaceLegacyValue("gradient-menu.confirm.slot", 48, 35);
            migrated |= replaceLegacyValue("gradient-menu.confirm.material", "LIME_CONCRETE", "LIME_DYE");
            migrated |= replaceLegacyValue("gradient-menu.confirm.name", "&a&lCONFIRMAR", "&a&lCONFIRMAR GRADIENTE");
            migrated |= replaceLegacyValue("gradient-menu.cancel.slot", 50, 27);
            migrated |= replaceLegacyValue("gradient-menu.cancel.material", "RED_CONCRETE", "BARRIER");
            migrated |= replaceLegacyValue("gradient-menu.cancel.name", "&c&lCANCELAR", "&cVolver sin guardar");
        }

        if (version < 5) {
            migrated |= replaceLegacyValue("main.title", "&8Tus Colores", "&8Your Colors");
            migrated |= replaceLegacyValue("main.rank.name", "&6&lCOLOR DEL RANGO", "&6&lRANK COLOR");
            migrated |= replaceLegacyValue("main.rank.lore", List.of("", "&7Color actual:", "&f➥ {current}", "", "&eClick para cambiar."),
                List.of("", "&7Current color:", "&f➥ {current}", "", "&eClick to change."));
            migrated |= replaceLegacyValue("main.plus.name", "&e&lCOLOR DEL PLUS", "&e&lPLUS COLOR");
            migrated |= replaceLegacyValue("main.plus.lore", List.of("", "&7Color actual:", "&f➥ {current}", "", "&eClick para cambiar."),
                List.of("", "&7Current color:", "&f➥ {current}", "", "&eClick to change."));
            migrated |= replaceLegacyValue("main.name.name", "&a&lCOLOR DEL NOMBRE", "&a&lNAME COLOR");
            migrated |= replaceLegacyValue("main.name.lore", List.of("", "&7Color actual:", "&f➥ {current}", "", "&eClick para cambiar."),
                List.of("", "&7Current color:", "&f➥ {current}", "", "&eClick to change."));
            migrated |= replaceLegacyValue("main.preview.name", "&f&lVISTA PREVIA", "&f&lPREVIEW");

            migrated |= replaceLegacyValue("selection-type.title", "&8Seleccionar tipo de color", "&8Select Color Type");
            migrated |= replaceLegacyValue("selection-type.preset.name", "&a&lCOLORES PREDEFINIDOS", "&a&lPRESET COLORS");
            migrated |= replaceLegacyValue("selection-type.preset.lore",
                List.of("", "&7Selecciona un color del servidor.", "", "&7Actualmente: &f{current}", "{status}"),
                List.of("", "&7Select a server preset color.", "", "&7Current: &f{current}", "{status}"));
            migrated |= replaceLegacyValue("selection-type.rgb.lore",
                List.of("", "&7Usa cualquier color HEX.", "", "&7Actualmente: &f{current}", "{status}"),
                List.of("", "&7Use any HEX color.", "", "&7Current: &f{current}", "{status}"));
            migrated |= replaceLegacyValue("selection-type.rgb.name", "&b&lCOLOR RGB", "&b&lRGB COLOR");
            migrated |= replaceLegacyValue("selection-type.gradient.name", "&d&lGRADIENTE", "&d&lGRADIENT");
            migrated |= replaceLegacyValue("selection-type.gradient.lore",
                List.of("", "&7Combina varios colores.", "", "&7Actualmente: &f{current}", "{status}"),
                List.of("", "&7Combine multiple colors.", "", "&7Current: &f{current}", "{status}"));
            migrated |= replaceLegacyValue("selection-type.preview.name", "&f&lCOLOR ACTUAL", "&f&lCURRENT COLOR");
            migrated |= replaceLegacyValue("selection-type.preview.lore",
                List.of("", "&7Modo: &f{mode}", "&7Valor: &f{current}", "", "{preview}"),
                List.of("", "&7Mode: &f{mode}", "&7Value: &f{current}", "", "{preview}"));
            migrated |= replaceLegacyValue("selection-type.back.name", "&cVolver al menú principal", "&cBack to Main Menu");
            migrated |= replaceLegacyValue("selection-type.status.selected", "&a✔ Actualmente seleccionado", "&a✔ Currently selected");
            migrated |= replaceLegacyValue("selection-type.status.available", "&eClick para configurar.", "&eClick to configure.");
            migrated |= replaceLegacyValue("selection-type.status.locked", "&c✘ Bloqueado", "&c✘ Locked");

            migrated |= replaceLegacyValue("preset-menu.title", "&8Colores predefinidos", "&8Preset Colors");
            migrated |= replaceLegacyValue("preset-menu.previous.name", "&eAnterior", "&ePrevious");
            migrated |= replaceLegacyValue("preset-menu.back.name", "&cVolver", "&cBack");
            migrated |= replaceLegacyValue("preset-menu.next.name", "&eSiguiente", "&eNext");
            migrated |= replaceLegacyValue("preset-menu.color-item.lore",
                List.of("", "&7Vista previa: {preview}", "", "{status}"),
                List.of("", "&7Preview: {preview}", "", "{status}"));
            migrated |= replaceLegacyValue("preset-menu.status.selected", "&a✔ SELECCIONADO", "&a✔ SELECTED");
            migrated |= replaceLegacyValue("preset-menu.status.available", "&eClick para seleccionar.", "&eClick to select.");
            migrated |= replaceLegacyValue("preset-menu.status.locked", "&c✘ BLOQUEADO", "&c✘ LOCKED");

            migrated |= replaceLegacyValue("gradient-menu.title", "&8Crear gradiente", "&8Create Gradient");
            migrated |= replaceLegacyValue("gradient-menu.stop-item.lore",
                List.of("", "&7Actual: {hex}", "", "&eClick izquierdo para cambiar.", "&cClick derecho para eliminar."),
                List.of("", "&7Current: {hex}", "", "&eLeft-click to change.", "&cRight-click to remove."));
            migrated |= replaceLegacyValue("gradient-menu.add.name", "&b&lAÑADIR COLOR", "&b&lADD COLOR");
            migrated |= replaceLegacyValue("gradient-menu.confirm.name", "&a&lCONFIRMAR GRADIENTE", "&a&lCONFIRM GRADIENT");
            migrated |= replaceLegacyValue("gradient-menu.cancel.name", "&cVolver sin guardar", "&cBack Without Saving");
            migrated |= replaceLegacyValue("confirmation.title", "&8Confirmar color RGB", "&8Confirm RGB Color");
            migrated |= replaceLegacyValue("confirmation.confirm.name", "&a&lCONFIRMAR", "&a&lCONFIRM");
            migrated |= replaceLegacyValue("confirmation.cancel.name", "&c&lCANCELAR", "&c&lCANCEL");
        }

        if (version < 6) {
            migrated |= replaceLegacyValue("main.preview.lore", List.of("", "{preview}"),
                List.of("", "&7Chat: {chat_preview}", "&7TAB: {tab_preview}",
                    "&7Nametag: {nametag_preview}", "&7Scoreboard: {scoreboard_preview}"));
        }

        menus.set("menus-version", 6);
        if (migrated) {
            plugin.getLogger().info("Applied compatible menu upgrades in memory; custom values and menus.yml were preserved.");
        }
    }

    private boolean replaceLegacyValue(String path, Object legacyValue, Object currentValue) {
        return replaceLegacyValue(menus, path, legacyValue, currentValue);
    }

    private boolean replaceLegacyValue(YamlConfiguration file, String path, Object legacyValue, Object currentValue) {
        if (!file.contains(path, true) || !Objects.equals(file.get(path), legacyValue)) return false;
        file.set(path, currentValue);
        return true;
    }

    private void applyEnglishDefaults() {
        boolean migrated = replaceLegacyValue(config, "rank.groups.default", "Jugador", "Player");
        migrated |= replaceLegacyValue(colors, "colors.orange.display-name", "&6Naranja", "&6Orange");
        migrated |= replaceLegacyValue(colors, "colors.yellow.display-name", "&eAmarillo", "&eYellow");
        migrated |= replaceLegacyValue(colors, "colors.gray.display-name", "&7Gris", "&7Gray");
        migrated |= replaceLegacyValue(colors, "colors.dark_red.display-name", "&4Rojo Oscuro", "&4Dark Red");
        migrated |= replaceLegacyValue(colors, "colors.red.display-name", "&cRojo", "&cRed");
        migrated |= replaceLegacyValue(colors, "colors.dark_green.display-name", "&2Verde Oscuro", "&2Dark Green");
        migrated |= replaceLegacyValue(colors, "colors.lime.display-name", "&aVerde Lima", "&aLime");
        migrated |= replaceLegacyValue(colors, "colors.light_lime.display-name", "&aVerde Lima Claro", "&aLight Lime");
        migrated |= replaceLegacyValue(colors, "colors.pink.display-name", "&dRosa", "&dPink");
        migrated |= replaceLegacyValue(colors, "colors.blue.display-name", "&9Azul", "&9Blue");
        migrated |= replaceLegacyValue(colors, "colors.white.display-name", "&fBlanco", "&fWhite");

        migrated |= replaceLegacyValue(messages, "no-permission", "{prefix}&cNo tienes permiso.", "{prefix}&cYou do not have permission.");
        migrated |= replaceLegacyValue(messages, "no-permission", "{prefix}&cYou do not have permission.", "{prefix}&cYou do not have permission to use this command.");
        migrated |= replaceLegacyValue(messages, "player-only", "{prefix}&cEste comando solo puede usarlo un jugador.", "{prefix}&cOnly players can use this command.");
        migrated |= replaceLegacyValue(messages, "player-not-found", "{prefix}&cEse jugador no está conectado.", "{prefix}&cThat player is not online.");
        migrated |= replaceLegacyValue(messages, "reloaded", "{prefix}&aConfiguración recargada.", "{prefix}&aConfiguration reloaded.");
        migrated |= replaceLegacyValue(messages, "usage.player", "{prefix}&cUso: /rankcolors", "{prefix}&cUsage: /rankcolors");
        migrated |= replaceLegacyValue(messages, "usage.admin-namespace", "{prefix}&cLa administración solo está disponible mediante /drc admin.", "{prefix}&cAdministration is only available through /drc admin.");
        migrated |= replaceLegacyValue(messages, "usage.set", "{prefix}&cUso: /drc admin set <player> <rank|plus|name> <preset>", "{prefix}&cUsage: /drc admin set <player> <rank|plus|name> <preset>");
        migrated |= replaceLegacyValue(messages, "usage.setrgb", "{prefix}&cUso: /drc admin setrgb <player> <rank|plus|name> <#RRGGBB>", "{prefix}&cUsage: /drc admin setrgb <player> <rank|plus|name> <#RRGGBB>");
        migrated |= replaceLegacyValue(messages, "usage.setgradient", "{prefix}&cUso: /drc admin setgradient <player> <rank|plus|name> <hex1> <hex2> [hex3...]", "{prefix}&cUsage: /drc admin setgradient <player> <rank|plus|name> <hex1> <hex2> [hex3...]");
        migrated |= replaceLegacyValue(messages, "usage.reset", "{prefix}&cUso: /drc admin reset <player> [rank|plus|name|all]", "{prefix}&cUsage: /drc admin reset <player> [rank|plus|name|all]");
        migrated |= replaceLegacyValue(messages, "admin.usage.title", "&6&lDuskRankColors &7- administración", "&6&lDuskRankColors &7- administration");
        migrated |= replaceLegacyValue(messages, "admin.info.platform", "&7Plataforma: &f{platform}", "&7Platform: &f{platform}");
        migrated |= replaceLegacyValue(messages, "admin.info.server", "&7Servidor: &f{server}", "&7Server: &f{server}");
        migrated |= replaceLegacyValue(messages, "admin.info.colors", "&7Colores cargados: &f{count}", "&7Loaded colors: &f{count}");
        migrated |= replaceLegacyValue(messages, "admin.info.cache", "&7Jugadores en caché: &f{count}", "&7Cached players: &f{count}");
        migrated |= replaceLegacyValue(messages, "admin.info.status.active", "&aActivo", "&aActive");
        migrated |= replaceLegacyValue(messages, "admin.info.status.inactive", "&cInactivo", "&cInactive");
        migrated |= replaceLegacyValue(messages, "invalid-category", "{prefix}&cCategoría inválida. Usa rank, plus o name.", "{prefix}&cInvalid category. Use rank, plus, or name.");
        migrated |= replaceLegacyValue(messages, "invalid-color", "{prefix}&cEse color no existe o no está disponible.", "{prefix}&cThat color does not exist or is unavailable.");
        migrated |= replaceLegacyValue(messages, "invalid-hex", "{prefix}&cEse HEX no es válido. Usa #RRGGBB.", "{prefix}&cThat HEX value is invalid. Use #RRGGBB.");
        migrated |= replaceLegacyValue(messages, "invalid-gradient", "{prefix}&cEse gradiente no es válido.", "{prefix}&cThat gradient is invalid.");
        migrated |= replaceLegacyValue(messages, "color-locked", "{prefix}&cNo tienes acceso a ese color.", "{prefix}&cYou do not have access to that color.");
        migrated |= replaceLegacyValue(messages, "mode-locked", "{prefix}&cNo tienes acceso a este tipo de color.", "{prefix}&cYou do not have access to this color type.");
        migrated |= replaceLegacyValue(messages, "input.rgb", "{prefix}&7Escribe un color HEX. &fEjemplo: #FFB224 &7(cancelar/cancel)", "{prefix}&7Enter a HEX color. &fExample: #FFB224 &7(cancel)");
        migrated |= replaceLegacyValue(messages, "input.gradient", "{prefix}&7Escribe el nuevo HEX. &fEjemplo: #55FFFF &7(cancelar/cancel)", "{prefix}&7Enter the new HEX value. &fExample: #55FFFF &7(cancel)");
        migrated |= replaceLegacyValue(messages, "input.cancelled", "{prefix}&cConfiguración cancelada.", "{prefix}&cConfiguration cancelled.");
        migrated |= replaceLegacyValue(messages, "input.expired", "{prefix}&cLa configuración ha expirado.", "{prefix}&cConfiguration has expired.");
        migrated |= replaceLegacyValue(messages, "selected.rank", "{prefix}&7Color del rango actualizado.", "{prefix}&7Rank color updated.");
        migrated |= replaceLegacyValue(messages, "selected.plus", "{prefix}&7Color del plus actualizado.", "{prefix}&7Plus color updated.");
        migrated |= replaceLegacyValue(messages, "selected.name", "{prefix}&7Color del nombre actualizado.", "{prefix}&7Name color updated.");
        migrated |= replaceLegacyValue(messages, "reset.rank", "{prefix}&7Color del rango restablecido.", "{prefix}&7Rank color reset.");
        migrated |= replaceLegacyValue(messages, "reset.plus", "{prefix}&7Color del plus restablecido.", "{prefix}&7Plus color reset.");
        migrated |= replaceLegacyValue(messages, "reset.name", "{prefix}&7Color del nombre restablecido.", "{prefix}&7Name color reset.");
        migrated |= replaceLegacyValue(messages, "reset.all", "{prefix}&7Tus colores fueron restablecidos.", "{prefix}&7All your colors were reset.");
        migrated |= replaceLegacyValue(messages, "admin.usage.commands",
            "&e/drc admin <reload|info|set|setrgb|setgradient|reset>",
            "&e/drc admin <reload|info|inspect|validate|set|setrgb|setgradient|reset>");

        if (migrated) {
            plugin.getLogger().info("Applied English defaults in memory; custom YAML values were preserved.");
        }
    }

    public YamlConfiguration config() { return config; }
    public YamlConfiguration colors() { return colors; }
    public YamlConfiguration menus() { return menus; }
    public YamlConfiguration messages() { return messages; }
}
