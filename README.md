<p align="center">
  <img src="images/plugin-logo.png" alt="Wireless Redstone" width="128" />
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

- **Wireless Bulbs & Lamps:** Create linked groups of copper bulbs or redstone lamps that synchronize their lit/unlit state across any distance — up to 26 blocks per group
- **Wireless Containers:** Create linked groups of chests, shulker boxes, or copper chests that share a single inventory in real time across any distance
- **Management GUI:** Visual interface for managing all wireless groups with category organization, custom naming, and icon assignment
- **Circuit Analyser:** Diagnostic tool for inspecting wireless blocks with WireView mode showing color-coded glowing outlines per group
- **Connector Tool:** Management tool for rapidly adding or removing blocks from existing groups, with creation mode for building new groups in place
- **Block Recovery:** Recover lost or accidentally broken wireless blocks that still belong to an existing group
- **All Copper Variants:** Full support for normal, exposed, weathered, and oxidized copper bulbs and chests — including all waxed variants
- **All Shulker Colors:** Support for all 17 shulker box colors as wireless container variants
- **Hopper Compatible:** Wireless containers work seamlessly with hoppers for automated item transfer and sorting systems
- **Persistent Data:** All group data is saved automatically and survives server restarts

### **Supported Platforms**

- **Server Software:** `Spigot`, `Paper`, `Purpur`, `CraftBukkit`
- **Minecraft Versions:** `1.21.5`, `1.21.6`, `1.21.7`, `1.21.8`, `1.21.9`, `1.21.10` and higher
- **Java Requirements:** `Java 17+`

### **Installation**

1. Download the latest `.jar` from the [Releases](https://github.com/Cobbleworks/Wireless-Redstone/releases) page
2. Stop your Minecraft server
3. Copy the `.jar` into your server's `plugins` folder
4. Start your server — a default configuration folder is generated at `plugins/WirelessRedstone/`

### **Player Commands**

| Command | Description |
|---------|-------------|
| `/wireless bulbs [count] [variant]` | Get linked copper bulbs |
| `/wireless lamps [count]` | Get linked redstone lamps |
| `/wireless chests [count] [variant]` | Get linked containers |
| `/wireless append <group> [count]` | Add more blocks to an existing group |
| `/wireless recover <group>` | Recover lost or missing blocks in a group |
| `/wireless setname <group> <newName>` | Rename a group |
| `/wireless setcategory <group> <category>` | Assign a group to a category (or `none`) |
| `/wireless tool inspect [player]` | Get a Circuit Analyser diagnostic tool |
| `/wireless tool connector <group>` | Get a Connector Tool (creates group if new) |
| `/wireless gui [--all]` | Open the category selection GUI |
| `/wireless debug on\|off` | Toggle sync debug messages |
| `/wireless reload` | Reload configuration files (admin only) |

**Aliases:** `/wr` — **Optional Flags:** `--name=<name>` and `--category=<category>` when creating bulbs, lamps, or chests

### **License**

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
