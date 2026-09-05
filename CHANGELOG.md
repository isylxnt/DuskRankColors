# Changelog

## 1.0

- Replaced external group-name lookup with prioritized, permission-based internal ranks and no default rank.
- Added independent chat, TAB, nametag, and scoreboard display contexts.
- Added rank policies for permitted modes and preset colors.
- Added reset, inspection, configuration validation, and permission-aware administration under `/drc admin`.
- Added configurable feedback sounds and fully English bundled configuration.
- Added context, part, rank, legacy, and MiniMessage placeholders/API output.
- Added result-bearing asynchronous API setters/resets while keeping legacy setters compatible.
- Reworked the main, selection, preset, RGB confirmation, and gradient menus, including real RGB firework-star colors.
- Replaced one rank-watcher chain per player with one global cycle and entity-safe dispatch.
- Split administrative command handling and inventory layout concerns into focused classes.
- Expanded configuration validation for formats, defaults, ranks, policies, menu sizes/slots, materials, and sounds.
- Added unit and MockBukkit lifecycle/integration coverage.
- Made pending HEX input compatible with cancelled modern and legacy chat events used by custom chat plugins.
