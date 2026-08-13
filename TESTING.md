# DuskRankColors testing and audits

## Automated suite

Run:

```bash
./gradlew clean build
```

The JUnit 5 suite covers strict HEX validation/normalization (including black and white), invariant selection factories, immutable settings replacement, two-stop and multi-stop interpolation, endpoints, empty/one-character/Unicode input, more stops than characters, gradient serialization/deserialization and corruption/bounds, case-insensitive categories, and internal message replacement.

## Runtime matrix

Use a fresh server directory for every row. Install only the resulting plugin unless an integration column says otherwise.

| Server | JVM required by server | PAPI | LuckPerms | Expected |
|---|---:|---:|---:|---|
| Paper 1.20.1 | 17 | No | No | Loads one JAR; static rank fallback |
| Paper 1.20.1 | 17 | Yes | Yes | Expansion registers; LP primary group maps |
| Latest Paper 26.2 | Server-defined | No | No | Same JAR and PDC schema |
| Latest Paper 26.2 | Server-defined | Yes | Yes | Both optional hooks register |
| Compatible Folia release | Server-defined | Both variants | Both variants | `Platform: Folia`; no thread warnings |

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
9. Change a preset HEX, run `/rankcolors reload`, and verify cached users update while their PDC still stores the preset ID.
10. Delete the selected preset or corrupt a saved gradient in a disposable player file; rejoin and verify safe default fallback.

## Permission matrix

- Grant and revoke each configured preset permission.
- Grant and revoke all three category RGB nodes and all three gradient nodes.
- Test `settings.validate-permissions: false`.
- Test stored permission loss with `permissions.validate-selected-color` and fallback settings.
- Verify GUI, user commands, and crafted menu-like items reach the same server-side validator.
- Verify every admin subcommand independently and through `duskrankcolors.admin` children.

## Inventory security

While every custom inventory is open, attempt normal click, shift-click, double-click/collect-to-cursor, number-key/hotbar swap, offhand swap, drop, and drag across top and player inventory. The listener cancels every click whenever a `MenuHolder` owns the top inventory and cancels all drags; actions are selected from server-side holder/slot state, never item names, lore, material, or title.

## Placeholder and integration checks

1. With no dependencies, verify clean startup and all plugin features.
2. Add only PlaceholderAPI; verify every README placeholder and rapid repeated expansion.
3. Add only LuckPerms; map known/unknown groups and ensure no LP data changes.
4. Add both and repeat.
5. Change a selection while a scoreboard repeatedly requests placeholders; confirm immediate cache invalidation and no PDC/YAML reads.
6. Confirm gradient `legacy`, `hex`, `raw`, `gradient`, and `minimessage` limitations match the README.

## Folia audit

- `plugin.yml` declares `folia-supported: true`.
- Player, inventory, entity, PDC, display-name, and sound work runs through the player's entity scheduler.
- Async chat parses immutable input then schedules mutation back onto that player's scheduler.
- Paper scheduler calls exist only inside `PaperSchedulerAdapter`; Folia scheduler reflection exists only inside `FoliaSchedulerAdapter`.
- Shared selection, rendered, input, draft, and cooldown maps are concurrent; configuration snapshots and registries are safely published.
- There are no repeating tasks. Input expiration uses one delayed callback with identity validation.

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

Idle behavior has zero loops, zero polling, zero repeating tasks, zero disk writes, and no event hotter than chat (which returns after one concurrent-map lookup). Placeholder requests do a UUID lookup into already-rendered values. Gradients are rebuilt only on join, selection change, or configuration reload. Menus are built only on navigation/change.

At 500 simulated players, sample idle CPU and allocation, then benchmark cached placeholder expansion. Expected retained per-player data is three compact selections plus four components/legacy strings. Quit, cancel, expiry, confirmation, close, disable, and menu cleanup paths must reduce cache/input/draft/cooldown sizes.

## Failure and reload checks

- Invalid preset HEX: skip only that preset and log its ID/value.
- Invalid material: use `PAPER`; invalid sound: silence.
- Invalid/default gradient bounds: clamp minimum to 2 and maximum to `[min,16]`.
- Reload preserves PDC selections, rebuilds definitions/rendered cache, and applies changed preset HEX.
- Disable unregisters the API/PAPI expansion, clears all maps, and cancels Paper tasks.
