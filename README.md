# DuskRankColors

**Fast. Simple. Beautiful. Lightweight.**

DuskRankColors 1.0 is a Paper plugin that lets every player choose the visual color of three independent parts of their identity: rank, plus symbol, and player name. Each part supports a configurable preset, an arbitrary RGB color, or a two-to-five-stop gradient.

Ranks are defined entirely inside DuskRankColors and assigned through ordinary Bukkit permissions. The plugin does not read group names or metadata from a permission plugin and does not try to intercept every chat plugin. It exposes cached PlaceholderAPI values and a small Adventure-based Java API so the display plugin remains in control.

## Requirements and compatibility

- Paper 1.20.1 or newer, including the 26.1/26.2 target line.
- Folia is supported through entity/global/async scheduler adapters.
- Java 17 bytecode. Run the server with the Java version required by that server release (new Paper releases may require a newer JVM; newer JVMs load Java 17 bytecode).
- PlaceholderAPI is an optional soft dependency. No permission-plugin API is required.
- One JAR; no NMS, CraftBukkit internals, Vault, ProtocolLib, PacketEvents, database, or runtime libraries.

The project compiles against Paper 1.20.1 and intentionally uses the minimum stable API surface. Folia-only scheduler symbols are isolated behind one small reflective compatibility boundary so the same class files load on Paper 1.20.1. See [TESTING.md](TESTING.md) for the runtime matrix and audit details.

## Installation

1. Stop the server.
2. Copy `DuskRankColors-1.0.jar` into `plugins/`.
3. Start the server and edit `plugins/DuskRankColors/config.yml`, `colors.yml`, `menus.yml`, and `messages.yml` if desired.
4. Grant `duskrankcolors.use` to every player who should be able to open the menu, then grant the desired preset/RGB/gradient permissions.

Never use Bukkit `/reload`; use `/drc admin reload`. The plugin reload refreshes internal ranks for every online player and safely re-registers or unregisters its PlaceholderAPI expansion.

## User flow

`/rankcolors` opens the main menu with independent Rank, Plus, and Name buttons. Choosing one always opens a second screen with three distinct buttons:

- **PREDEFINED** opens the paginated, config-driven preset list.
- **RGB** asks for a six-digit HEX value through chat and then opens a confirmation menu.
- **GRADIENT** opens a draft editor. Left-click changes a stop, right-click removes it, and the add button creates another stop. The stored selection changes only after confirmation.

Chat accepts `#FFB224`, `FFB224`, and lower-case forms. `#FFF`, named colors, and malformed input are rejected. `cancel` returns without saving. Pending input listens to both Paper's modern chat event and Bukkit's legacy compatibility event, including events already cancelled by a custom chat plugin; the captured HEX is cancelled before normal chat handling. A one-shot expiration callback is used—there is no polling task.

Each category screen also includes a configurable reset button. The main-menu preview renders the separate chat, TAB, nametag, and scoreboard formats rather than assuming every display plugin needs the same text.

## Commands

| Command | Purpose |
|---|---|
| `/rankcolors` | Open the main menu |
| `/drc admin reload` | Reload all four YAML files |
| `/drc admin info` | Show platform, hooks, registry, and cache state |
| `/drc admin inspect <player>` | Show the resolved rank, permission, priority, selections, and rendered contexts |
| `/drc admin validate` | Audit ranks, colors, permissions, materials, sounds, and policy lists |
| `/drc admin set <player> <category> <preset>` | Set an online player's preset |
| `/drc admin setrgb <player> <category> <hex>` | Set an online player's RGB |
| `/drc admin setgradient <player> <category> <hex...>` | Set an online player's gradient |
| `/drc admin reset <player> [category\|all]` | Reset an online player |

Aliases for opening the GUI: `/rc`, `/drc`, `/rankcolor`. Player customization is GUI-only. Administrative commands live exclusively under the `/drc admin` namespace and have permission-aware tab completion. They are not blocked by the player-only `duskrankcolors.use` node. Administrative editing intentionally targets online players; the plugin never edits offline NBT.

## Permissions

General nodes:

- `duskrankcolors.use`, `.gui`, `.rank`, `.plus`, `.name`
- `duskrankcolors.<rank|plus|name>.rgb`
- `duskrankcolors.<rank|plus|name>.gradient`
- A preset uses the category-specific node from `colors.yml`, defaulting to `duskrankcolors.<category>.<preset-id>`.

Administrative nodes:

- `duskrankcolors.admin`
- `duskrankcolors.admin.reload`, `.set`, `.reset`, `.info`, `.rgb`, `.gradient`, `.inspect`, `.validate`

The `admin` node declares its children in `plugin.yml`. Wildcards such as `duskrankcolors.rank.*` and `duskrankcolors.*` are handled by modern permission plugins such as LuckPerms; DuskRankColors does not implement a competing wildcard engine.

Menu access is denied by default. Grant `duskrankcolors.use`; it includes the internal `duskrankcolors.gui` node. Without it, `/rankcolors` and its GUI aliases return the configurable `no-permission` message and play the configured denied sound. Administrative `/drc admin` commands remain controlled only by their respective admin permissions.

## Internal ranks

Ranks are declared under `ranks` in `config.yml`. They are not matched against a LuckPerms group name. Every entry automatically uses `duskrankcolors.ranks.<id>` unless `permission` is explicitly configured.

```yaml
ranks:
  vip:
    display: VIP
    priority: 10
    color: "&a"
    plus: false
    plus-color: ""
    bold: true
    allowed-modes:
      rank: [PRESET, RGB, GRADIENT]
      plus: [PRESET, RGB, GRADIENT]
      name: [PRESET, RGB, GRADIENT]
    allowed-colors:
      rank: ["*"]
      plus: [orange, yellow]
      name: ["*"]
  vip+:
    display: VIP
    priority: 20
    color: "&a"
    plus: true
    plus-color: "&6"
    bold: true
```

Legacy colors such as `&a` must be quoted because `&` has special meaning in YAML. `#RRGGBB` and `&#RRGGBB` are also accepted. `priority` is an integer, and the rank with the greatest value wins when a player has several rank permissions.

Rank permission nodes are registered dynamically with a default value of `false`. Operators therefore do not inherit every configured rank automatically; a rank permission must be granted explicitly. There is no default, member, or player rank. Without a matching rank permission, the rank and plus placeholders are empty, the full placeholder contains only the colored player name, and the main menu hides the rank/plus selectors.

For example:

```text
/lp group vip permission set duskrankcolors.ranks.vip true
/lp group vipplus permission set duskrankcolors.ranks.vip+ true
```

The configured rank and plus colors are used when the player has not stored a personal choice. Menu or admin selections override those initial colors; resetting rank/plus restores the defaults of the player's current internal rank. The `plus` field controls whether the plus is rendered at all and whether its selector appears in the main menu, while `bold` applies to both the rank display and its plus. Permission changes are synchronized automatically, on join, whenever the player opens the main menu, or through `/drc admin reload`.

`allowed-modes` and `allowed-colors` are optional per-category policies. Omitting a category allows everything, `[]` allows nothing, and `["*"]` allows every preset—including presets added later. These policies supplement Bukkit permissions; they do not replace them. Administrative setters bypass rank policies intentionally. Stored choices that cease to be valid for a newly assigned rank fall back to that rank's configured color.

`settings.rank-refresh-ticks` controls one lightweight global refresh cycle. Each cycle dispatches only the actual rank comparison to the correct player/entity scheduler, which keeps Folia ownership safe without retaining a repeating task per player. The bundled value `20` updates within approximately one second without querying YAML, PDC, or group metadata. Set it to `0` to disable automatic checks.

## Display contexts

Four independently configurable formats live under `format.contexts`:

```yaml
format:
  contexts:
    chat: "{rank}{plus} {name}"
    tab: "{rank}{plus} {name}"
    nametag: "{rank}{plus}"
    scoreboard: "{rank}{plus} {name}"
```

Each accepts `{rank}`, `{plus}`, and `{name}`. When a player has no rank, rank and plus tokens are removed cleanly. These formats are exposed through PlaceholderAPI and the Java API; DuskRankColors does not take control away from the chat, TAB, nametag, or scoreboard plugin. If the optional built-in `apply.player-display-name` or `apply.player-list-name` switches are enabled, they use the configured `chat` and `tab` contexts respectively.

## Presets

Presets are parsed once on startup/reload into an O(1), case-insensitive registry. A definition controls display name, HEX, legacy fallback, material, enabled state, category availability, and optional category-specific permissions. Invalid definitions are skipped with a focused warning; an invalid material becomes `PAPER`.

PDC stores the preset ID, not its HEX. Changing `orange.hex` and running `/drc admin reload` therefore updates every cached `orange` user immediately without rewriting player data.

## RGB and gradients

RGB values are normalized to uppercase `#RRGGBB`. Gradients store the compact form `#RRGGBB,#RRGGBB,...`; interpolation is RGB-linear over all segments and operates on Unicode code points. Empty text and a one-code-point plus symbol are safe; one code point deliberately uses the first gradient stop. Adventure components are the internal source of truth.

## PlaceholderAPI

Identifier: `duskrankcolors`. Placeholder requests are memory-only and use pre-rendered cache entries—no PDC, YAML, disk, permission, or group reads occur in the placeholder hot path.

- `%duskrankcolors_rank%`, `_plus`, `_name`, `_full`, `_preview`
- `%duskrankcolors_chat%`, `_tab`, `_nametag`, `_scoreboard`
- `%duskrankcolors_rank_id%`, `_rank_display`, `_rank_priority`, `_rank_has_plus`, `_rank_bold`
- `%duskrankcolors_rank_color%`, `_plus_color`, `_name_color`
- `%duskrankcolors_rank_hex%`, `_plus_hex`, `_name_hex`
- `%duskrankcolors_rank_legacy%`, `_plus_legacy`, `_name_legacy`
- `%duskrankcolors_rank_mode%`, `_plus_mode`, `_name_mode`
- `%duskrankcolors_rank_raw%`, `_plus_raw`, `_name_raw`
- `%duskrankcolors_rank_gradient%`, `_plus_gradient`, `_name_gradient`
- `%duskrankcolors_rank_minimessage%`, `_plus_minimessage`, `_name_minimessage`

Repeat the suffix with `rank`, `plus`, or `name`. `*_gradient` is empty for non-gradients. `*_hex` returns the first stop for a gradient. `*_legacy` returns the configured preset legacy code, or the nearest legacy color to the RGB/first gradient stop. Formatted basic placeholders use section-sign/HEX legacy serialization for consumers that accept it. MiniMessage gradients use `<gradient:#STOP:#STOP...>`.

## Storage and cache

Selections are saved atomically when confirmed to the player's `PersistentDataContainer`:

```text
duskrankcolors:rank_mode / rank_value
duskrankcolors:plus_mode / plus_value
duskrankcolors:name_mode / name_value
```

Legacy `duskrankcolors:<category>` preset keys migrate on join. Corrupt data, deleted presets, disabled modes, and invalid gradients fall back to configuration defaults and their invalid keys are removed. Player settings and all rendered variants are held in concurrent maps from join to quit.

## Configuration

- `config.yml`: internal ranks, defaults, format, custom-mode limits, integrations, optional display-name application, and cooldown.
- `colors.yml`: presets and per-category access.
- `menus.yml`: titles, rows, slots, items, lore, statuses, pagination, editor layout, and sounds. Every displayed menu string is configurable.
- `messages.yml`: every user-facing command/input/info message and internal replacement.

Rows and slots are clamped/validated when inventories are built. Missing or unavailable materials and sounds have safe fallbacks. Existing files inherit newly bundled keys in memory, known legacy defaults are upgraded, and administrator customizations are preserved. Existing files are never overwritten during version checks.

The bundled language is English. Known Spanish defaults from earlier builds are translated in memory on startup/reload, while any value that was customized by an administrator remains untouched.

Every UI sound is configured under `sounds` in `menus.yml`. Opening, navigation, HEX input, selection, confirmation, cancellation, and denied actions each have independent `enabled`, `sound`, `volume`, and `pitch` values. Invalid sound names safely fall back to silence.

## Java API

Retrieve the registered service through Bukkit's `ServicesManager`:

```java
DuskRankColorsAPI api = Bukkit.getServicesManager().load(DuskRankColorsAPI.class);
Component display = api.formatFull(player);
```

The API exposes all three selections, their setters, and Adventure component formatters. `PlayerColorChangeEvent` is cancellable and fires before PDC/cache mutation. Legacy `void` setters remain available for source compatibility; new integrations should use the asynchronous methods so they can observe validation or cancellation:

```java
api.setNameSelectionAsync(player, PlayerColorSelection.rgb("#9863E7"))
    .thenAccept(result -> plugin.getLogger().info("Color result: " + result));

api.resetAllAsync(player).thenAccept(changed -> {
    if (!changed) plugin.getLogger().warning("The reset was cancelled or could not run.");
});
```

The setter result is one of `SUCCESS`, `INVALID_SELECTION`, `MODE_DISABLED`, `NO_PERMISSION`, `RANK_RESTRICTED`, or `CANCELLED`. `isLoaded(UUID)` lets an integration check whether cached output is ready before requesting formatted values. On Folia, an operation whose entity scheduler retires completes instead of leaving its future pending.

It also exposes the current `RankView`, individual `DisplayPart` values, and every `DisplayContext` as an Adventure `Component`, section-sign legacy string, or serialized MiniMessage string:

```java
Component tab = api.formatContext(player, DisplayContext.TAB);
String legacy = api.formatContextLegacy(player, DisplayContext.NAMETAG);
String miniMessage = api.formatPartMiniMessage(player, DisplayPart.FULL);
Optional<RankView> rank = api.getRank(player);
```

## Building

```bash
./gradlew clean build
```

The reproducible JAR is written to `build/libs/DuskRankColors-1.0.jar`. Production classes target Java 17 via `--release 17`; tests run on JUnit 5 and MockBukkit.

## Display plugins

Minecraft has no universal rank-color API. Configure the chat, tab, scoreboard, or nametag plugin to consume the appropriate DuskRankColors placeholder/API format. Automatic `displayName` and player-list-name application is disabled by default to avoid conflicts.

## License

DuskRankColors is available under the [MIT License](LICENSE).
