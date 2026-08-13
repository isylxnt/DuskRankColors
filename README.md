# DuskRankColors

**Fast. Simple. Beautiful. Lightweight.**

DuskRankColors 1.0.0 is a Paper plugin that lets every player choose the visual color of three independent parts of their identity: rank, plus symbol, and player name. Each part supports a configurable preset, an arbitrary RGB color, or a two-to-five-stop gradient.

The plugin does not rewrite LuckPerms metadata and does not try to intercept every chat plugin. It exposes cached PlaceholderAPI values and a small Adventure-based Java API so the display plugin remains in control.

## Requirements and compatibility

- Paper 1.20.1 or newer, including the 26.1/26.2 target line.
- Folia is supported through entity/global/async scheduler adapters.
- Java 17 bytecode. Run the server with the Java version required by that server release (new Paper releases may require a newer JVM; newer JVMs load Java 17 bytecode).
- PlaceholderAPI and LuckPerms are optional soft dependencies.
- One JAR; no NMS, CraftBukkit internals, Vault, ProtocolLib, PacketEvents, database, or runtime libraries.

The project compiles against Paper 1.20.1 and intentionally uses the minimum stable API surface. Folia-only scheduler symbols are isolated behind one small reflective compatibility boundary so the same class files load on Paper 1.20.1. See [TESTING.md](TESTING.md) for the runtime matrix and audit details.

## Installation

1. Stop the server.
2. Copy `DuskRankColors-1.0.0.jar` into `plugins/`.
3. Start the server and edit `plugins/DuskRankColors/config.yml`, `colors.yml`, `menus.yml`, and `messages.yml` if desired.
4. Grant preset/RGB/gradient permissions with the server permission system.

Never use Bukkit `/reload`; use `/rankcolors reload`.

## User flow

`/rankcolors` opens the main menu with independent Rank, Plus, and Name buttons. Choosing one always opens a second screen with three distinct buttons:

- **PREDEFINED** opens the paginated, config-driven preset list.
- **RGB** asks for a six-digit HEX value through chat and then opens a confirmation menu.
- **GRADIENT** opens a draft editor. Left-click changes a stop, right-click removes it, and the add button creates another stop. The stored selection changes only after confirmation.

Chat accepts `#FFB224`, `FFB224`, and lower-case forms. `#FFF`, named colors, and malformed input are rejected. `cancel` and `cancelar` return without saving. A one-shot expiration callback is used—there is no polling task.

## Commands

| Command | Purpose |
|---|---|
| `/rankcolors` | Open the main menu |
| `/rankcolors rank\|plus\|name` | Open a category |
| `/rankcolors preview` | Show the current styled result |
| `/rankcolors reset [rank\|plus\|name\|all]` | Reset personal selections |
| `/rankcolors rgb <category> <hex>` | Set personal RGB |
| `/rankcolors gradient <category> <hex1> <hex2> [hex...]` | Set personal gradient |
| `/rankcolors help` | Show command help |
| `/rankcolors reload` | Reload all four YAML files |
| `/rankcolors info` | Show platform, hooks, registry, and cache state |
| `/rankcolors set <player> <category> <preset>` | Set an online player's preset |
| `/rankcolors setrgb <player> <category> <hex>` | Set an online player's RGB |
| `/rankcolors setgradient <player> <category> <hex...>` | Set an online player's gradient |
| `/rankcolors reset <player> [category\|all]` | Reset an online player |

Aliases: `/rc`, `/drc`, `/rankcolor`. All command paths have tab completion. Administrative editing intentionally targets online players; the plugin never edits offline NBT.

## Permissions

General nodes:

- `duskrankcolors.use`, `.gui`, `.rank`, `.plus`, `.name`, `.reset`, `.preview`
- `duskrankcolors.<rank|plus|name>.rgb`
- `duskrankcolors.<rank|plus|name>.gradient`
- A preset uses the category-specific node from `colors.yml`, defaulting to `duskrankcolors.<category>.<preset-id>`.

Administrative nodes:

- `duskrankcolors.admin`
- `duskrankcolors.admin.reload`, `.set`, `.reset`, `.info`, `.rgb`, `.gradient`

The `admin` node declares its children in `plugin.yml`. Wildcards such as `duskrankcolors.rank.*` and `duskrankcolors.*` are handled by modern permission plugins such as LuckPerms; DuskRankColors does not implement a competing wildcard engine.

## Presets

Presets are parsed once on startup/reload into an O(1), case-insensitive registry. A definition controls display name, HEX, legacy fallback, material, enabled state, category availability, and optional category-specific permissions. Invalid definitions are skipped with a focused warning; an invalid material becomes `PAPER`.

PDC stores the preset ID, not its HEX. Changing `orange.hex` and running `/rankcolors reload` therefore updates every cached `orange` user immediately without rewriting player data.

## RGB and gradients

RGB values are normalized to uppercase `#RRGGBB`. Gradients store the compact form `#RRGGBB,#RRGGBB,...`; interpolation is RGB-linear over all segments and operates on Unicode code points. Empty text and a one-code-point plus symbol are safe; one code point deliberately uses the first gradient stop. Adventure components are the internal source of truth.

## PlaceholderAPI

Identifier: `duskrankcolors`. Placeholder requests are memory-only and use pre-rendered cache entries—no PDC, YAML, disk, or LuckPerms reads occur in the placeholder hot path.

- `%duskrankcolors_rank%`, `_plus`, `_name`, `_full`, `_preview`
- `%duskrankcolors_rank_color%`, `_plus_color`, `_name_color`
- `%duskrankcolors_rank_hex%`, `_plus_hex`, `_name_hex`
- `%duskrankcolors_rank_legacy%`, `_plus_legacy`, `_name_legacy`
- `%duskrankcolors_rank_mode%`, `_plus_mode`, `_name_mode`
- `%duskrankcolors_rank_raw%`, `_plus_raw`, `_name_raw`
- `%duskrankcolors_rank_gradient%`, `_plus_gradient`, `_name_gradient`
- `%duskrankcolors_rank_minimessage%`, `_plus_minimessage`, `_name_minimessage`

Repeat the suffix with `rank`, `plus`, or `name`. `*_gradient` is empty for non-gradients. `*_hex` returns the first stop for a gradient. `*_legacy` returns the configured preset legacy code, or the nearest legacy color to the RGB/first gradient stop. Formatted basic placeholders use section-sign/HEX legacy serialization for consumers that accept it. MiniMessage gradients use `<gradient:#STOP:#STOP...>`.

## LuckPerms

With LuckPerms present and `rank.source: LUCKPERMS_PRIMARY_GROUP`, the primary group is mapped through `rank.groups`. Unknown groups use the configured group-name fallback. Without LuckPerms, the static rank text is used. DuskRankColors never modifies a prefix, suffix, group, permission, or user node.

## Storage and cache

Selections are saved atomically when confirmed to the player's `PersistentDataContainer`:

```text
duskrankcolors:rank_mode / rank_value
duskrankcolors:plus_mode / plus_value
duskrankcolors:name_mode / name_value
```

Legacy `duskrankcolors:<category>` preset keys migrate on join. Corrupt data, deleted presets, disabled modes, and invalid gradients fall back to configuration defaults and their invalid keys are removed. Player settings and all rendered variants are held in concurrent maps from join to quit.

## Configuration

- `config.yml`: defaults, format, rank source, custom-mode limits, integrations, optional display-name application, and cooldown.
- `colors.yml`: presets and per-category access.
- `menus.yml`: titles, rows, slots, items, lore, pagination, editor layout, and sounds.
- `messages.yml`: all user-facing command/input messages and internal replacements.

Rows and slots are clamped/validated when inventories are built. Missing or unavailable materials and sounds have safe fallbacks. Existing files are never overwritten during version checks.

## Java API

Retrieve the registered service through Bukkit's `ServicesManager`:

```java
DuskRankColorsAPI api = Bukkit.getServicesManager().load(DuskRankColorsAPI.class);
Component display = api.formatFull(player);
```

The API exposes all three selections, their setters, and Adventure component formatters. `PlayerColorChangeEvent` is cancellable and fires before PDC/cache mutation.

## Building

```bash
./gradlew clean build
```

The reproducible JAR is written to `build/libs/DuskRankColors-1.0.0.jar`. Production classes target Java 17 via `--release 17`; tests run on JUnit 5.

## Display plugins

Minecraft has no universal rank-color API. Configure the chat, tab, scoreboard, or nametag plugin to consume the appropriate DuskRankColors placeholder/API format. Automatic `displayName` and player-list-name application is disabled by default to avoid conflicts.
