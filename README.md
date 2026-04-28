<p align="center">
  <img src="images/ <p align="center">
  <img src="images/plugin-logo.png" alt="Wireless Redstone" width="180" />
</p>
<h1 align="center">Wireless Redstone</h1>
<p align="center">
  <b>Wirelessly link copper bulbs, redstone lamps, and container blocks.</b><br>
  <b>State and inventory synchronization across any distance with GUI management.</b>
</p>
<p align="center">
  <a href="https://github.com/Cobbleworks/Wireless-Redstone/releases"><img src="https://img.shields.io/github/v/release/Cobbleworks/Wireless-Redstone?include_prereleases&style=flat-square&color=4CAF50" alt="Latest Release"></a>&nbsp;&nbsp;<a href="https://github.com/Cobbleworks/Wireless-Redstone/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License"></a>&nbsp;&nbsp;<img src="https://img.shields.io/badge/Java-17+-orange?style=flat-square" alt="Java Version">&nbsp;&nbsp;<img src="https://img.shields.io/badge/Minecraft-1.21+-green?style=flat-square" alt="Minecraft Version">&nbsp;&nbsp;<img src="https://img.shields.io/badge/Platform-Spigot%2FPaper-yellow?style=flat-square" alt="Platform">&nbsp;&nbsp;<img src="https://img.shields.io/badge/Status-Active-brightgreen?style=flat-square" alt="Status">
</p>

Wireless Redstone is an open-source Minecraft plugin that allows players to create named groups of wirelessly linked blocks that synchronize their states or inventories across any distance. Link copper bulbs and redstone lamps into groups that toggle together, or link chests, shulker boxes, and copper chests into shared inventory groups that update in real time. All group data is saved persistently and survives server restarts, with full GUI-based management, a circuit analyser diagnostic tool, a connector tool for rapid assignment, and hopper-compatible container support.

### **Core Features**

- **Wireless Bulbs & Lamps:** Create linked groups of copper bulbs or redstone lamps that synchronize state across any distance (2-26 blocks per group)
- **Wireless Containers:** Create linked groups of chests, barrels, shulker boxes, or copper chests that share inventory in real time
- **Management GUI:** Visual interface for managing all wireless groups with category organization, custom naming, and icon assignment
- **Circuit Analyser:** Diagnostic tool for inspecting wireless blocks with WireView mode showing color-coded glowing outlines per group
- **Connector Tool:** Management tool for rapidly adding or removing blocks from existing groups, with creation mode for building new groups in place
- **Block Recovery:** Recover lost or accidentally broken wireless blocks that still belong to an existing group
- **All Copper Variants:** Full support for normal, exposed, weathered, and oxidized copper bulbs and chests ï¿½ including all waxed variants
- **All Shulker Colors:** Support for all 17 shulker box colors as wireless container variants
- **Hopper Compatible:** Wireless containers work seamlessly with hoppers for automated item transfer and sorting systems
- **Persistent Data:** All group data is saved automatically and survives server restarts

### **Supported Platforms**

- **Server Software:** `Spigot`, `Paper`, `Purpur`, `CraftBukkit`
- **Minecraft Versions:** `1.21` and higher
- **Java Requirements:** `Java 17+`

### **Installation**

1. Download the latest `.jar` from the [Releases](https://github.com/Cobbleworks/Wireless-Redstone/releases) page
2. Stop your Minecraft server
3. Copy the `.jar` into your server's `plugins/` folder
4. Start your server ï¿½ a default configuration folder is generated at `plugins/WirelessRedstone/`

### **Player Commands**

| Command | Description |
|---------|-------------|
| `/wireless` | Open management GUI (same as `/wireless gui`) |
| `/wireless help` | Show command help |
| `/wireless give bulb [amount] [--variant] [--name=...] [--category=...]` | Give linked copper bulb items |
| `/wireless give lamp [amount] [--name=...] [--category=...]` | Give linked redstone lamp items |
| `/wireless give chest [amount] [--variant] [--name=...] [--category=...]` | Give linked container items |
| `/wireless create <groupName> [categoryName]` | Give a Connector Tool for existing group, or creation-mode tool for a new group |
| `/wireless inspect [player]` | Give a Circuit Analyser (admin can target another player) |
| `/wireless modify name <groupName> <newName>` | Rename a group |
| `/wireless modify category <groupName> <categoryName>` | Assign group to category (use `none` to remove) |
| `/wireless append <groupName> [count]` | Extend an existing group by 1-24 slots (max 26 total) |
| `/wireless recover <groupName>` | Recover unplaced/missing items for a group |
| `/wireless gui [--all] [--nocategory]` | Open management GUI (category view or direct list view) |
| `/wireless debug on\|off` | Toggle sync debug messages |
| `/wireless reload` | Reload configuration files (admin only) |

**Aliases:** `/wr`

### **Variant Flags**

`/wireless give bulb` supports:

- `--copper`
- `--exposed`
- `--weathered`
- `--oxidized`

`/wireless give chest` supports:

- `--chest`
- `--barrel`
- `--shulker` plus color variants like `--white`, `--red`, `--blue`, etc.
- copper chest variants: `--copper`, `--copper-exposed`, `--copper-weathered`, `--copper-oxidized`, `--copper-waxed`, `--copper-waxed-exposed`, `--copper-waxed-weathered`, `--copper-waxed-oxidized`

### **Data Files**

Wireless-Redstone persists all runtime data to YAML files under `plugins/WirelessRedstone/`.

| File | Purpose |
|------|---------|
| `bulbs.yml` | Bulb/lamp groups, ownership, names, category links, variant material, locations |
| `chests.yml` | Container groups, shared inventories, ownership, names, category links, locations |
| `categories.yml` | Category definitions, owners, icons |

### **Permissions**

| Permission | Description | Default |
|------------|-------------|---------|
| `wirelessredstone.use` | Allows using wireless redstone commands | `op` |
| `wirelessredstone.teleport` | Allows teleporting to bulb locations via GUI | `op` |
| `wirelessredstone.remove` | Allows removing bulb groups via GUI | `op` |
| `wirelessredstone.admin` | Allows viewing/managing all players' groups and using admin actions | `op` |

### **License**

This project is licensed under the **MIT License** ï¿½ see the [LICENSE](LICENSE) file for details.

## **Screenshots**

The screenshots below demonstrate the core features of the Wireless Redstone plugin, including a lever activating a lamp without any redstone connection, and the connector tool for linking components.

<table>
  <tr>
    <th>Wireless Redstone - Lever Activating Lamp</th>
    <th>Wireless Redstone - Connector Tool</th>
  </tr>
  <tr>
    <td><a href="https://github.com/Cobbleworks/Wireless-Redstone/raw/main/images/screenshot-lever-lamp.png" target="_blank" rel="noopener noreferrer"><img src="https://github.com/Cobbleworks/Wireless-Redstone/raw/main/images/screenshot-lever-lamp.png" alt="Lever Activating Lamp" width="450"></a></td>
    <td><a href="https://github.com/Cobbleworks/Wireless-Redstone/raw/main/images/screenshot-connector.png" target="_blank" rel="noopener noreferrer"><img src="https://github.com/Cobbleworks/Wireless-Redstone/raw/main/images/screenshot-connector.png" alt="Connector Tool" width="450"></a></td>
  </tr>
</table>
.Value -replace 'width="180"', 'width="180"'  />
</p>
<h1 align="center">Wireless Redstone</h1>
<p align="center">
  <b>Wirelessly link copper bulbs, redstone lamps, and container blocks.</b><br>
  <b>State and inventory synchronization across any distance with GUI management.</b>
</p>
<p align="center">
  <a href="https://github.com/Cobbleworks/Wireless-Redstone/releases"><img src="https://img.shields.io/github/v/release/Cobbleworks/Wireless-Redstone?include_prereleases&style=flat-square&color=4CAF50" alt="Latest Release"></a>&nbsp;&nbsp;<a href="https://github.com/Cobbleworks/Wireless-Redstone/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License"></a>&nbsp;&nbsp;<img src="https://img.shields.io/badge/Java-17+-orange?style=flat-square" alt="Java Version">&nbsp;&nbsp;<img src="https://img.shields.io/badge/Minecraft-1.21+-green?style=flat-square" alt="Minecraft Version">&nbsp;&nbsp;<img src="https://img.shields.io/badge/Platform-Spigot%2FPaper-yellow?style=flat-square" alt="Platform">&nbsp;&nbsp;<img src="https://img.shields.io/badge/Status-Active-brightgreen?style=flat-square" alt="Status">
</p>

Wireless Redstone is an open-source Minecraft plugin that allows players to create named groups of wirelessly linked blocks that synchronize their states or inventories across any distance. Link copper bulbs and redstone lamps into groups that toggle together, or link chests, shulker boxes, and copper chests into shared inventory groups that update in real time. All group data is saved persistently and survives server restarts, with full GUI-based management, a circuit analyser diagnostic tool, a connector tool for rapid assignment, and hopper-compatible container support.

### **Core Features**

- **Wireless Bulbs & Lamps:** Create linked groups of copper bulbs or redstone lamps that synchronize state across any distance (2-26 blocks per group)
- **Wireless Containers:** Create linked groups of chests, barrels, shulker boxes, or copper chests that share inventory in real time
- **Management GUI:** Visual interface for managing all wireless groups with category organization, custom naming, and icon assignment
- **Circuit Analyser:** Diagnostic tool for inspecting wireless blocks with WireView mode showing color-coded glowing outlines per group
- **Connector Tool:** Management tool for rapidly adding or removing blocks from existing groups, with creation mode for building new groups in place
- **Block Recovery:** Recover lost or accidentally broken wireless blocks that still belong to an existing group
- **All Copper Variants:** Full support for normal, exposed, weathered, and oxidized copper bulbs and chests ï¿½ including all waxed variants
- **All Shulker Colors:** Support for all 17 shulker box colors as wireless container variants
- **Hopper Compatible:** Wireless containers work seamlessly with hoppers for automated item transfer and sorting systems
- **Persistent Data:** All group data is saved automatically and survives server restarts

### **Supported Platforms**

- **Server Software:** `Spigot`, `Paper`, `Purpur`, `CraftBukkit`
- **Minecraft Versions:** `1.21` and higher
- **Java Requirements:** `Java 17+`

### **Installation**

1. Download the latest `.jar` from the [Releases](https://github.com/Cobbleworks/Wireless-Redstone/releases) page
2. Stop your Minecraft server
3. Copy the `.jar` into your server's `plugins/` folder
4. Start your server ï¿½ a default configuration folder is generated at `plugins/WirelessRedstone/`

### **Player Commands**

| Command | Description |
|---------|-------------|
| `/wireless` | Open management GUI (same as `/wireless gui`) |
| `/wireless help` | Show command help |
| `/wireless give bulb [amount] [--variant] [--name=...] [--category=...]` | Give linked copper bulb items |
| `/wireless give lamp [amount] [--name=...] [--category=...]` | Give linked redstone lamp items |
| `/wireless give chest [amount] [--variant] [--name=...] [--category=...]` | Give linked container items |
| `/wireless create <groupName> [categoryName]` | Give a Connector Tool for existing group, or creation-mode tool for a new group |
| `/wireless inspect [player]` | Give a Circuit Analyser (admin can target another player) |
| `/wireless modify name <groupName> <newName>` | Rename a group |
| `/wireless modify category <groupName> <categoryName>` | Assign group to category (use `none` to remove) |
| `/wireless append <groupName> [count]` | Extend an existing group by 1-24 slots (max 26 total) |
| `/wireless recover <groupName>` | Recover unplaced/missing items for a group |
| `/wireless gui [--all] [--nocategory]` | Open management GUI (category view or direct list view) |
| `/wireless debug on\|off` | Toggle sync debug messages |
| `/wireless reload` | Reload configuration files (admin only) |

**Aliases:** `/wr`

### **Variant Flags**

`/wireless give bulb` supports:

- `--copper`
- `--exposed`
- `--weathered`
- `--oxidized`

`/wireless give chest` supports:

- `--chest`
- `--barrel`
- `--shulker` plus color variants like `--white`, `--red`, `--blue`, etc.
- copper chest variants: `--copper`, `--copper-exposed`, `--copper-weathered`, `--copper-oxidized`, `--copper-waxed`, `--copper-waxed-exposed`, `--copper-waxed-weathered`, `--copper-waxed-oxidized`

### **Data Files**

Wireless-Redstone persists all runtime data to YAML files under `plugins/WirelessRedstone/`.

| File | Purpose |
|------|---------|
| `bulbs.yml` | Bulb/lamp groups, ownership, names, category links, variant material, locations |
| `chests.yml` | Container groups, shared inventories, ownership, names, category links, locations |
| `categories.yml` | Category definitions, owners, icons |

### **Permissions**

| Permission | Description | Default |
|------------|-------------|---------|
| `wirelessredstone.use` | Allows using wireless redstone commands | `op` |
| `wirelessredstone.teleport` | Allows teleporting to bulb locations via GUI | `op` |
| `wirelessredstone.remove` | Allows removing bulb groups via GUI | `op` |
| `wirelessredstone.admin` | Allows viewing/managing all players' groups and using admin actions | `op` |

### **License**

This project is licensed under the **MIT License** ï¿½ see the [LICENSE](LICENSE) file for details.

## **Screenshots**

The screenshots below demonstrate the core features of the Wireless Redstone plugin, including a lever activating a lamp without any redstone connection, and the connector tool for linking components.

<table>
  <tr>
    <th>Wireless Redstone - Lever Activating Lamp</th>
    <th>Wireless Redstone - Connector Tool</th>
  </tr>
  <tr>
    <td><a href="https://github.com/Cobbleworks/Wireless-Redstone/raw/main/images/screenshot-lever-lamp.png" target="_blank" rel="noopener noreferrer"><img src="https://github.com/Cobbleworks/Wireless-Redstone/raw/main/images/screenshot-lever-lamp.png" alt="Lever Activating Lamp" width="450"></a></td>
    <td><a href="https://github.com/Cobbleworks/Wireless-Redstone/raw/main/images/screenshot-connector.png" target="_blank" rel="noopener noreferrer"><img src="https://github.com/Cobbleworks/Wireless-Redstone/raw/main/images/screenshot-connector.png" alt="Connector Tool" width="450"></a></td>
  </tr>
</table>
