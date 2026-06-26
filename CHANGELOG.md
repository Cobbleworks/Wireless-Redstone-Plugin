# Changelog

All notable changes to Wireless Redstone will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

- Added category descriptions in the wireless GUI.
- Added GUI buttons for receiving the Connector Tool and Circuit Analyser.
- Changed `/wireless create` to use the Connector Tool chat prompt, making connector-based setup the primary group creation flow.
- Changed `/wireless recover <groupName>` to restore saved block positions in-place after environmental block destruction.
- Changed wireless group GUI right-clicks to print analyser-style group details with clickable teleport links.
- Renamed `/wireless append` to `/wireless extend` and removed the append command alias.
- Removed `/wireless debug` mode.

## [1.0.0] - 2026-04-28

Wireless Redstone v1.0.0 is the initial release, enabling wireless state synchronization for copper bulbs, redstone lamps, and containers across any distance — with GUI management, a circuit analyser, and full hopper compatibility.

### Wireless Signals

- **Bulb And Lamp Groups**: Link copper bulbs or redstone lamps into named groups that synchronize power state across any distance (2–26 blocks per group)
- **All Copper Variants**: Supports normal, exposed, weathered, and oxidized copper bulbs and chests — including all waxed variants

### Wireless Containers

- **Shared Inventory Groups**: Link chests, barrels, shulker boxes, or copper chests into groups that share inventory in real time
- **All Shulker Colors**: All 17 shulker box color variants supported as wireless container types
- **Hopper Compatible**: Wireless containers work seamlessly with hoppers for automated item transfer and sorting

### Management And Diagnostics

- **Management GUI**: Visual interface for all wireless groups with category organisation, custom naming, and icon assignment
- **Circuit Analyser**: Diagnostic tool with WireView mode — color-coded glowing outlines show group membership per block
- **Connector Tool**: Rapidly add or remove blocks from existing groups, with in-place creation mode for building new groups
- **Block Recovery**: Recover lost or accidentally broken wireless blocks that still belong to an existing group

### Persistence

- **Automatic Save**: All group data is saved automatically and survives server restarts

**Note:** If you encounter any bugs or issues, please don't hesitate to open an [issue](https://github.com/Cobbleworks/Wireless-Redstone-Plugin/issues). For any questions or to start a discussion, feel free to initiate a [discussion](https://github.com/Cobbleworks/Wireless-Redstone-Plugin/discussions) on the GitHub repository.
