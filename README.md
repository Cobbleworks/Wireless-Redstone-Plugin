# 📡 Wireless Redstone

<p align="center">
  <img src="assets/icon.png" alt="Wireless Redstone Banner" width="600">
</p>

**Wireless Redstone** is a powerful Minecraft plugin for Paper servers that enables players to create groups of wirelessly linked blocks that synchronize their states across any distance. Link copper bulbs, redstone lamps, chests, shulker boxes, and even the new copper chests!

[![Paper](https://img.shields.io/badge/Paper-1.21.10+-blue)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21+-orange)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

---

## ✨ Features

### 🔆 Wireless Bulbs & Lamps

- Create linked groups of **copper bulbs** or **redstone lamps** that sync their lit/unlit state
- Support for **2-26 bulbs per group** (labeled A through Z)
- All copper bulb oxidation stages: normal, exposed, weathered, and oxidized
- Ambient particle effects show when bulbs are active

### 📦 Wireless Containers

- Create linked groups of **chests**, **shulker boxes**, or **copper chests** that share inventory
- Support for **all 17 shulker box colors** plus regular chests
- **8 copper chest variants**: normal, exposed, weathered, oxidized (+ waxed versions)
- Real-time inventory synchronization across unlimited distance
- Hopper-compatible for automation

### 🎛️ WireView Mode

- Toggle visual highlighting of all your wireless connections
- Placed bulbs and containers glow with color-coded outlines
- Each group has a unique color for easy identification

### 🖥️ Management GUI

- Visual interface to manage all your wireless groups
- **Category system** to organize groups (bulbs, lamps, containers)
- Teleport to any placed bulb or container
- Rename groups and categories with custom names
- Set custom icons for groups and categories
- Remove groups with one click (also removes all placed blocks)

### 🔍 Circuit Analyser

- Diagnostic tool for inspecting wireless blocks
- Get one with `/wireless inspect`
- Right-click any wireless block to see:
  - Group name, ID, and category
  - Owner information
  - All associated block locations (clickable to teleport)
  - Placed/total count status
  - Block-specific info (bulb state, container type)
- **Mini WireView**: While held, nearby wireless blocks within 8 blocks glow with color-coded outlines
  - Each group has a unique color for easy identification
  - Works in both main hand and off hand

### 🔄 Block Recovery

- Recover lost or broken wireless blocks with `/wireless recover <group>`
- If you placed blocks and later broke some, they show as "not placed" in the group
- The recover command regenerates items for those missing slots
- Maintains the same group ID and slot labels (A, B, C, etc.)

### 💾 Persistence

- All data saved automatically to YAML files
- Survives server restarts and reloads
- Automatic cleanup when groups are emptied

---

## 📋 Requirements

- **Paper Server** 1.21.10 or higher
- **Java 21** or higher
- For copper chest support: Minecraft 1.21.10+

---

## ⚙️ Installation

1. Download the latest release JAR from [Releases](../../releases)
2. Place the JAR in your server's `plugins` folder
3. Restart your server
4. Done! Use `/wireless` to get started

### Building from Source

```bash
git clone https://github.com/yourusername/Wireless-Redstone.git
cd Wireless-Redstone
mvn clean package
```

The compiled JAR will be in `target/WirelessRedstone-1.0.0.jar`

---

## 🔧 Commands

All commands use `/wireless` (or `/wr` alias).

| Command                              | Description                            |
| ------------------------------------ | -------------------------------------- |
| `/wireless bulbs [count] [variant]`  | Get linked copper bulbs                |
| `/wireless lamps [count]`            | Get linked redstone lamps              |
| `/wireless chests [count] [variant]` | Get linked containers                  |
| `/wireless append <group> [count]`   | Add more blocks to existing group      |
| `/wireless recover <group>`          | Recover lost/missing blocks in group   |
| `/wireless inspect [player]`         | Get a Circuit Analyser diagnostic tool |
| `/wireless gui [--all]`              | Open category selection GUI            |
| `/wireless gui --nocategory`         | Open GUI without categories            |
| `/wireless wireview`                 | Toggle connection highlighting         |
| `/wireless debug on\|off`            | Toggle sync debug messages             |

### Bulb Variants

| Variant       | Description           |
| ------------- | --------------------- |
| `--copper`    | Copper bulb (default) |
| `--exposed`   | Exposed copper bulb   |
| `--weathered` | Weathered copper bulb |
| `--oxidized`  | Oxidized copper bulb  |

### Container Variants

| Variant                     | Description                           |
| --------------------------- | ------------------------------------- |
| `--chest`                   | Regular chest (default)               |
| `--shulker`                 | Purple shulker box                    |
| `--white`, `--orange`, etc. | Colored shulker boxes (all 16 colors) |
| `--copper`                  | Copper chest                          |
| `--copper-exposed`          | Exposed copper chest                  |
| `--copper-weathered`        | Weathered copper chest                |
| `--copper-oxidized`         | Oxidized copper chest                 |
| `--copper-waxed`            | Waxed copper chest                    |
| `--copper-exposed-waxed`    | Waxed exposed copper chest            |
| `--copper-weathered-waxed`  | Waxed weathered copper chest          |
| `--copper-oxidized-waxed`   | Waxed oxidized copper chest           |

### Examples

```bash
# Get 2 linked copper bulbs
/wireless bulbs

# Get 4 linked oxidized copper bulbs
/wireless bulbs 4 --oxidized

# Get 3 linked redstone lamps
/wireless lamps 3

# Get 2 linked regular chests
/wireless chests

# Get 4 linked cyan shulker boxes
/wireless chests 4 --cyan

# Get 2 linked copper chests
/wireless chests 2 --copper

# Add 3 more bulbs to an existing group named "Kitchen"
/wireless append Kitchen 3

# Add 2 more blocks to a group (defaults to 2 if no count specified)
/wireless append "My Group"

# Recover lost blocks (e.g., if you placed 4, broke 2, they show as "not placed")
/wireless recover MyGroup

# Get a Circuit Analyser to inspect wireless blocks
/wireless inspect

# Give a Circuit Analyser to another player (admin only)
/wireless inspect Steve
```

---

## 🔐 Permissions

| Permission                  | Description                    | Default |
| --------------------------- | ------------------------------ | ------- |
| `wirelessredstone.use`      | Use `/wireless` commands       | op      |
| `wirelessredstone.teleport` | Teleport via GUI               | op      |
| `wirelessredstone.remove`   | Remove groups via GUI          | op      |
| `wirelessredstone.admin`    | Admin access (view all groups) | op      |

---

## 🖥️ GUI Controls

Open the GUI with `/wireless gui` to access the category selection screen.

### Category Selection

| Action                   | Function                               |
| ------------------------ | -------------------------------------- |
| **Click category**       | View groups in that category           |
| **Click Uncategorized**  | View groups without a category         |
| **Click emerald button** | Create a new category                  |
| **Middle-click**         | Rename the category                    |
| **Shift+Right-click**    | Set category icon (with held item)     |
| **Shift+Left-click**     | Delete category (moves groups to none) |

### Group Management

| Action                | Function                           |
| --------------------- | ---------------------------------- |
| **Left-click**        | Teleport to first placed block     |
| **Right-click**       | Teleport to last placed block      |
| **Middle-click**      | Rename the group                   |
| **Q (Drop key)**      | Change group category              |
| **Shift+Right-click** | Set custom icon (with held item)   |
| **Shift+Left-click**  | Remove group and all placed blocks |

Use the arrow buttons to navigate pages if you have many groups.

---

## 📁 Data Storage

Data is stored in the `plugins/WirelessRedstone/` folder:

- `bulbs.yml` - Wireless bulb/lamp group data
- `chests.yml` - Wireless container group data
- `categories.yml` - Category organization data

Data includes group IDs, locations, ownership, custom names/icons, categories, and shared inventories.

---

## 🔁 How It Works

### Bulb Synchronization

1. Create a group with `/wireless bulbs` or `/wireless lamps`
2. Place the bulbs anywhere in the world
3. When one bulb's state changes (powered/unpowered), all others sync automatically
4. Chunks must be loaded for syncing to occur

### Container Synchronization

1. Create a group with `/wireless chests`
2. Place the containers anywhere in the world
3. Items added/removed from one container instantly appear in all others
4. Breaking a container does NOT drop items (they remain in the shared inventory)
5. Breaking the last container removes the shared inventory permanently

---

## 💡 Tips & Notes

- **Chunk Syncing**: Bulbs and containers automatically sync when their chunks are loaded - no manual intervention needed!
- **Cross-Dimension**: Groups work across dimensions (Overworld, Nether, End)
- **Waxed Bulbs**: Copper bulbs are automatically waxed to prevent oxidation changes
- **Container Breaking**: Items stay in shared inventory when containers are broken
- **Hopper Support**: Hoppers can interact with wireless containers and trigger syncs
- **Debug Mode**: Use `/wireless debug on` to see sync messages near you

---

## 🐛 Troubleshooting

| Issue                      | Solution                                            |
| -------------------------- | --------------------------------------------------- |
| Bulbs not syncing          | Ensure all bulbs are placed, check debug mode       |
| Containers not syncing     | Verify group exists in GUI, try reopening container |
| GUI not working            | Check console for errors on startup                 |
| Rename not working         | Chat plugins may intercept; type `cancel` to abort  |
| Groups disappeared         | Check if world was renamed/removed                  |
| Lost blocks after breaking | Use `/wireless recover <group>` to get them back    |
| Analyser not working       | Right-click directly on the wireless block          |

---

## 🛠️ For Developers

### Building

```bash
mvn clean package
```

### Key Classes

- `LinkedBulbManager` - Manages bulb groups and synchronization
- `LinkedChestManager` - Manages container groups and inventory sync
- `CategoryManager` - Manages category creation, renaming, and persistence
- `WireViewManager` - Handles glowing entity visualization
- `BulbSyncTask` - Tick-based bulb state synchronization

### PDC Keys

Items use `PersistentDataContainer` for metadata storage:

- `wireless:wireless_bulb` / `wireless:wireless_chest` - Identifier
- `wireless:group_id` - UUID string
- `wireless:bulb_index` / `wireless:chest_index` - Position in group
- `wireless:owner` - Owner UUID
- `wireless:group_size` - Total group size

---

## 📸 Screenshots

<p align="center">
  <img src="assets/screenshots/bulbs.png" alt="Wireless Bulbs" width="400">
  <br>
  <em>Linked copper bulbs syncing their lit state</em>
</p>

<p align="center">
  <img src="assets/screenshots/chests.png" alt="Wireless Chests" width="400">
  <br>
  <em>Linked chests (or shulker boxes) sharing inventory. Works with hoppers and golems too!</em>
</p>

<p align="center">
  <img src="assets/screenshots/gui.png" alt="Management GUI" width="400">
  <br>
  <em>Management GUI for all your wireless groups</em>
</p>

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📞 Support

- Open an [Issue](../../issues) for bug reports or feature requests
- Check the [Wiki](../../wiki) for additional documentation

---

<p align="center">
  Made with ❤️ for the Minecraft community
</p>
