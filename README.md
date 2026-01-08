# 📡 Wireless Redstone

**Wirelessly link copper bulbs, redstone lamps, chests, and shulker boxes.** Syncs states and inventories across any distance.

[![Paper](https://img.shields.io/badge/Paper-1.21.10+-blue)](https://papermc.io/) [![Java](https://img.shields.io/badge/Java-21+-orange)](https://adoptium.net/)

<p align="left">
  <img src="assets/icon.png" alt="Wireless Redstone Banner" width="200">
</p>

---

## 🚀 Quick Start

1. Download from [Releases](../../releases) and place in `plugins/` folder
2. Restart server
3. Run `/wireless give bulb` or `/wireless give chest` to create your first group
4. Place the items anywhere - they sync automatically!

---

## 🔧 Core Tools

### 🔍 Circuit Analyser
Get with `/wireless inspect` - **Right-click any wireless block** to see group info, locations, and owner. **Hold it to see ALL wireless blocks glow** with color-coded outlines (each group has a unique color).

### 🔗 Connector Tool
Get with `/wireless create <groupName>` - **Right-click blocks to add** them to the group, **left-click to remove**. Creates new groups automatically if they don't exist.

### 🖥️ Management GUI
Open with `/wireless gui` - **Teleport to blocks**, rename groups, organize into categories, set custom icons, and delete groups.

---

## 📋 All Commands

### Creating Groups
```bash
/wireless give bulb [amount] [--copper|--exposed|--weathered|--oxidized]
/wireless give lamp [amount]
/wireless give chest [amount] [--chest|--shulker|--cyan|--copper|etc.]
```

**Optional flags**: `--name="Group Name"` `--category=CategoryName`

### Tools
```bash
/wireless inspect [player]          # Get Circuit Analyser
/wireless create <group> [category] # Get Connector Tool
```

### Managing Groups
```bash
/wireless gui [--all|--nocategory]           # Open GUI
/wireless modify name <group> <newName>      # Rename group
/wireless modify category <group> <category> # Assign to category
/wireless append <group> [count]             # Add more blocks
/wireless recover <group>                    # Recover lost blocks
/wireless debug on|off                       # Toggle sync messages
/wireless reload                             # Reload config (admin)
```

---

## 🎮 GUI Controls

### Category Screen
- **Click** category to view groups
- **Middle-click** to rename
- **Shift+Right-click** to set icon (hold item)
- **Shift+Left-click** to delete

### Group Screen
- **Left/Right-click** to teleport to first/last block
- **Middle-click** to rename
- **Q (Drop key)** to change category
- **Shift+Right-click** to set icon (hold item)
- **Shift+Left-click** to delete group

---

## ✨ What It Does

- **Bulbs & Lamps**: All linked bulbs sync their lit/unlit state instantly
- **Containers**: All linked chests/shulkers share the same inventory in real-time
- **Cross-dimension**: Works across Overworld, Nether, End
- **Hopper-compatible**: Automation works normally
- **Categories**: Organize your groups however you want

---

## 📸 Screenshots

<p align="center">
  <img src="assets/screenshots/bulbs.png" alt="Wireless Bulbs" width="400">
  <br><em>Linked copper bulbs syncing their lit state</em>
</p>

<p align="center">
  <img src="assets/screenshots/chests.png" alt="Wireless Chests" width="400">
  <br><em>Linked chests sharing inventory</em>
</p>

<p align="center">
  <img src="assets/screenshots/gui.png" alt="Management GUI" width="400">
  <br><em>Management GUI with categories</em>
</p>

---

## 🔐 Permissions

| Permission                  | Description            | Default |
|-----------------------------|------------------------|---------|
| `wirelessredstone.use`      | Use commands           | op      |
| `wirelessredstone.teleport` | Teleport via GUI       | op      |
| `wirelessredstone.remove`   | Remove groups via GUI  | op      |
| `wirelessredstone.admin`    | View all groups        | op      |

---

## 💡 Tips

- **Lost blocks?** Use `/wireless recover <group>` to get items back
- **Chunks must be loaded** for syncing to work
- **Breaking containers** doesn't drop items - they stay in shared inventory
- **WireView mode** automatically toggles when holding the Circuit Analyser

---

## 📄 License

MIT License - see [LICENSE](LICENSE) file

---

<p align="center">Made with ❤️ for the Minecraft community</p>
