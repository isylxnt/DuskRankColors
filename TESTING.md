# DuskRankColors testing and audits

## Automated suite

Run:

```bash
./gradlew clean build
```

The JUnit 5 suite covers strict HEX validation/normalization (including black and white), invariant selection factories, immutable settings replacement, two-stop and multi-stop interpolation, endpoints, empty/one-character/Unicode input, more stops than characters, gradient serialization/deserialization and corruption/bounds, case-insensitive categories, internal message replacement, component-safe format composition, legacy/HEX rank colors, permission rank priority/no-rank resolution, and every gradient slot count up to the configured maximum.

MockBukkit integration tests additionally boot the real plugin, verify its commands/configuration/API registration, open the main inventory, exercise live permission-rank refresh, run the bundled configuration through `/drc admin validate`, persist an asynchronous API selection to PDC, confirm that cancelled change events complete without mutation, and verify that a custom-chat-style cancelled legacy event still supplies pending HEX input.

GitHub Actions runs a clean build with all warnings enabled on Java 17 and 21 for every push and pull request. The workflow also validates the Gradle wrapper through the official Gradle setup action.

## Runtime matrix

Use a fresh server directory for every row. Install only the resulting plugin unless an integration column says otherwise.

| Server | JVM required by server | PAPI | Expected |
|---|---:|---:|---|
| Paper 1.20.1 | 17 | No | Loads one JAR; internal permission ranks resolve |
| Paper 1.20.1 | 17 | Yes | Expansion registers; internal ranks remain independent |
| Latest Paper 26.2 | Server-defined | No | Same JAR, rank registry, and PDC schema |
| Latest Paper 26.2 | Server-defined | Yes | Placeholder expansion registers |
| Compatible Folia release | Server-defined | Both variants | `Platform: Folia`; no thread warnings |

This repository can compile and unit-test logic without starting Minecraft servers. The named runtime matrix remains a deployment checklist unless those server binaries are supplied to the build environment; do not treat a compile against 1.20.1 as proof that a future server did not introduce a binary-breaking API removal.

## Functional checklist

For rank, plus, and name independently:

1. Join, open `/rankcolors`, and verify the three category buttons and exact preview.
2. Enter a category and verify PREDEFINED, RGB, and GRADIENT are three separate items.
3. Select an allowed preset; restart and confirm persistence.
4. Try `#FFB224`, `FFB224`, and `#ffb224`; cancel once, confirm once, restart.
5. Reject `#FFF`, `#ZZZZZZ`, `rgb(...)`, and named colors.
6. Create and confirm 2-, 3-, 4-, and 5-stop gradients. Exercise left-edit, right-remove, add, cancel, close, quit, and input expiration.
7. Verify minimum and maximum stops cannot be bypassed by commands or GUI actions.
8. Run personal reset per category and all; repeat admin set/reset for an online target.
9. Change a preset HEX, run `/drc admin reload`, and verify cached users update while their PDC still stores the preset ID.
10. Delete the selected preset or corrupt a saved gradient in a disposable player file; rejoin and verify safe default fallback.
11. Grant multiple `duskrankcolors.ranks.<id>` nodes and verify the highest-priority rank wins; test no permission (name only), `display`, `plus`, both colors, and `bold`.
12. Reset every category from its menu button and verify rank/plus return to the current internal rank's configured colors.
13. Configure distinct chat, TAB, nametag, and scoreboard formats and verify all four main-menu previews.

## Permission matrix

- Verify a new non-operator cannot open `/rankcolors`, `/rc`, `/drc`, or `/rankcolor`, receives `no-permission`, and hears the denied sound.
- Grant only `duskrankcolors.use`; verify all GUI aliases open and that revoking it immediately blocks them again.
- Verify `/drc admin` remains available to an authorized administrator without `duskrankcolors.use`.
- Grant and revoke each configured preset permission.
- Grant and revoke all three category RGB nodes and all three gradient nodes.
- Test `settings.validate-permissions: false`.
- Test stored permission loss with `permissions.validate-selected-color` and fallback settings.
- Verify GUI, user commands, and crafted menu-like items reach the same server-side validator.
- Verify every `/drc admin` subcommand independently and through `duskrankcolors.admin` children.
- Configure rank-specific allowed modes/presets, including omitted categories, empty lists, wildcards, and unknown entries.
- Change a player's rank permission while online and verify automatic refresh within `settings.rank-refresh-ticks`.

## Inventory security

While every custom inventory is open, attempt normal click, shift-click, double-click/collect-to-cursor, number-key/hotbar swap, offhand swap, drop, and drag across top and player inventory. The listener cancels every click whenever a `MenuHolder` owns the top inventory and cancels all drags; actions are selected from server-side holder/slot state, never item names, lore, material, or title.

## Sound feedback

- Exercise menu opening, navigation, HEX input, preset selection, RGB/gradient confirmation, cancellation, invalid input, and denied permission.
- Verify each action uses its matching `sounds.<type>` entry from `menus.yml`.
- Disable every sound individually, then use an invalid sound name and verify the action remains safe and silent.

## Placeholder and integration checks

1. With no dependencies, verify clean startup and all plugin features.
2. Add only PlaceholderAPI; verify every README placeholder and rapid repeated expansion.
3. Grant rank permissions using Bukkit's permission attachments or any permission plugin and verify no group name is read.
4. Change a selection while a scoreboard repeatedly requests placeholders; confirm immediate cache invalidation and no PDC/YAML reads.
5. Confirm gradient `legacy`, `hex`, `raw`, `gradient`, and `minimessage` limitations match the README.
6. Verify `rank_id`, `rank_display`, `rank_priority`, `rank_has_plus`, and `rank_bold` for every configured rank.
7. Verify the `chat`, `tab`, `nametag`, and `scoreboard` placeholders against their configured formats.
8. Retrieve every `DisplayPart`, `DisplayContext`, legacy, MiniMessage, and `RankView` API result.
9. Install a custom chat plugin that cancels or bridges chat events; enter HEX and `cancel` during RGB and gradient input, confirm the value is captured, and confirm the message is not broadcast.

## Folia audit

- `plugin.yml` declares `folia-supported: true`.
- Player, inventory, entity, PDC, display-name, and sound work runs through the player's entity scheduler.
- Async chat parses immutable input then schedules mutation back onto that player's scheduler.
- Paper scheduler calls exist only inside `PaperSchedulerAdapter`; Folia scheduler reflection exists only inside `FoliaSchedulerAdapter`.
- Shared selection, rendered, input, draft, and cooldown maps are concurrent; configuration snapshots and registries are safely published.
- Automatic rank checks use one self-cancelling global cycle, then dispatch each comparison to the owning entity scheduler. Input expiration uses one delayed callback with identity validation.

On a Folia test server, enable strict thread checks, join players in different regions, change colors simultaneously, run reload, quit during input/draft, and inspect the console for region ownership violations.

## Multiversion audit

- Compilation API: Paper `1.20.1-R0.1-SNAPSHOT`.
- Plugin `api-version`: `1.20`.
- Class file target: Java 17.
- Bukkit/Paper surface used: PDC, Adventure components, ordinary inventory/item APIs, events, permissions, plugin services, and player presentation APIs present in 1.20.1.
- Folia-only symbols are reflectively resolved after platform detection; Paper never resolves them.
- Materials/sounds in configuration use 1.20-era names and fall back safely.
- No NMS, CraftBukkit, packets, or version-string branching.

For each release under test, inspect startup, all menu materials/sounds, PDC persistence across restart, command registration, PAPI registration, and API service retrieval.

## Performance and memory audit

Idle behavior performs only the configurable permission-rank check (once per second by default), with zero disk writes and no group/YAML/PDC reads. Placeholder requests do a UUID lookup into already-rendered values. Gradients are rebuilt only on join, selection/rank change, or configuration reload. Menus are built only on navigation/change.

At 500 simulated players, sample idle CPU and allocation, then benchmark cached placeholder expansion. Expected retained per-player data is three compact selections plus the individual and four context component/legacy renderings. Quit, cancel, expiry, confirmation, close, and disable paths must reduce cache/input/draft/cooldown sizes; only one rank-watcher token exists for the entire plugin.

## Failure and reload checks

- Invalid preset HEX: skip only that preset and log its ID/value.
- Invalid material: use `PAPER`; invalid sound: silence.
- Invalid/default gradient bounds: clamp minimum to 2 and maximum to `[min,16]`.
- Reload preserves PDC selections, rebuilds definitions/rendered cache, and applies changed preset HEX.
- Reload rebuilds internal ranks, re-resolves every online player's permission rank, and re-registers/unregisters PlaceholderAPI according to the latest configuration.
- `/drc admin inspect` reports resolved state and `/drc admin validate` reports malformed/ambiguous configuration without mutating files.
- Bundled defaults fill newly added YAML paths in memory; known old defaults migrate while custom values remain untouched.
- Disable unregisters the API/PAPI expansion, clears all maps, and cancels Paper tasks.
