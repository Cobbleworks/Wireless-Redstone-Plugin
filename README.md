# Wireless Redstone

Wireless Redstone is a simple Minecraft plugin that lets players create groups of "wireless" bulbs (waxed copper bulbs) or redstone lamps that mirror each other's lit state across distance. Bulb groups persist across restarts and provide a small management GUI for renaming, teleporting, and removing groups.

---

## 📦 Features

- Create linked groups of bulbs (copper bulbs or redstone lamps) with a single command.
- Support for multi-connections: groups can have 2-26 bulbs (A, B, C, D, etc.) that all sync together.
- When bulbs in a group are placed, they will synchronize their lit/unlit state automatically.
- GUI-based management: teleport to bulbs, rename groups, and remove groups.
- Visual particle effects for placement, sync, and break events.
- Debug mode with `/wireless debug on|off` to show sync messages for nearby blocks.
- Bulb group data is stored in `plugins/WirelessRedstone/bulbs.yml` to persist between restarts.

---

## ⚙️ Installation

1. Build the plugin JAR (Maven):

```bash
mvn clean package
```

2. Copy the resulting JAR into your server's `plugins` folder.
3. Start (or restart) the Minecraft server.
4. Confirm `WirelessRedstone` appears in the server console and `plugins` list.

---

## 🔧 Commands

All commands are rooted under `/wireless`.

- `/wireless bulbs [count] [variant]`

  - Gives you a group of _linked copper bulbs_.
  - `count` is optional and defaults to 2 (range: 2-26).
  - `variant` is optional and defaults to `--copper`. Supported copper variants:
    - `--copper` (default)
    - `--exposed` (exposed copper)
    - `--weathered` (weathered copper)
    - `--oxidized` (oxidized copper)
  - Example: `/wireless bulbs 4 --oxidized` gives you 4 linked oxidized copper bulbs (A, B, C, D)

- `/wireless lamps [count]`

  - Gives you a group of _linked redstone lamps_.
  - `count` is optional and defaults to 2 (range: 2-26).
  - Example: `/wireless lamps 3` gives you 3 linked redstone lamps (A, B, C)

- `/wireless gui [--all]`

  - Opens the Bulb Manager GUI. If `--all` is specified and the player has admin permissions, the GUI shows every placed group on the server.

- `/wireless wireview`

  - Displays all connected bulb groups with a glowing outline effect.

- `/wireless debug on|off`
  - Toggles debug mode for the player. When enabled, you'll see sync messages for all blocks within 3 blocks of you.

### Command Notes

- If a player's inventory is full when receiving bulbs the plugin will spawn the items on the ground at the player's location.
- Tab completion is provided for subcommands, counts, and variants.

---

## 🔐 Permissions

Permissions are defined in `plugin.yml`:

- `wirelessredstone.use` — Allows the player to use `/wireless` commands. (Default: op)
- `wirelessredstone.teleport` — Allows teleporting to a bulb location via the GUI. (Default: op)
- `wirelessredstone.remove` — Allows removing bulb groups via the GUI. (Default: op)
- `wirelessredstone.admin` — Full admin control (see GUI `--all` view). (Default: op)

Note: You can change defaults or assign these permissions using a permissions plugin such as LuckPerms.

---

## 🧭 GUI (`/wireless gui`) — Controls / Interactions

- The GUI shows a paginated list of all groups owned by the player (or all groups if admin and `--all` is used).
- Item interactions in the GUI:
  - Left-click: Teleport to first placed bulb in the group (requires `wirelessredstone.teleport`).
  - Right-click: Teleport to last placed bulb in the group (requires `wirelessredstone.teleport`).
  - Middle-click: Rename the group — rename is processed through the chat (type `cancel` to abort; type `reset` or `clear` to reset to default; max 32 characters).
  - Shift+Click: Remove the group (requires `wirelessredstone.remove`, or admin to remove other players' groups).
  - Arrow icon: next/previous page navigation.
  - Book: shows page info / group count.

---

## 🔁 How it Works (Implementation Summary)

- Bulb groups are created with `/wireless bulbs [count]` or `/wireless lamps [count]` and carry metadata (group ID, index [A/B/C/...], owner, bulb type, group size).
- When a player places a wireless bulb, the `BlockPlaceEvent` registers the bulb's location in the server memory via `LinkedBulbManager`.
- Once multiple bulbs of a group are placed and loaded (chunks must be loaded), a sync task will mirror the `lit` state across all bulbs in the group.
- The `BulbSyncTask` runs every tick and does the following:
  - Spawn ambient particles for placed bulbs occasionally.
  - On each group where multiple bulbs are present and chunks are loaded, check their block states; if one changes, apply the same state to all others.
  - Use a `recentlySynced` set to avoid infinite sync loops.
  - Send debug messages to players within 3 blocks who have debug mode enabled, and spawn particle effects on sync.
- When a bulb is broken, only that bulb is unregistered from the group. Other bulbs in the group remain functional.
- All group data persists in `plugins/WirelessRedstone/bulbs.yml`.

---

## 📝 Data / Configuration

- Bulb groups are saved in `bulbs.yml` with the following properties for each group:
  - `id`, `lit`, `bulbType`, `maxSize`, `owner`, `customName`, `locations` (map of index to location).
- Location serialization uses `world,x,y,z`. If a world is missing (e.g. removed or renamed), that saved location will be ignored when loading.
- Old data format (pairs with loc1/loc2) is automatically migrated to the new group format on first load.

---

## 🖼️ Screenshot

Here is a screenshot showing linked bulbs.

![Wireless Bulb Screenshot](screenshot.png)

_Caption: A group of wireless bulbs showing synchronized lit state and the GUI title._

---

## 🚧 Edge Cases & Things to Watch Out For

- Chunks must be loaded for syncing to occur. If bulbs are in an unloaded chunk, syncing will not happen until the chunk is loaded.
- Teleportation from the GUI requires `wirelessredstone.teleport` permission. If a player has no permission the teleport will be refused.
- When a player breaks a bulb, only that bulb is unregistered. The other bulbs in the group remain functional and will continue to sync.
- If the server renames or removes a world referenced in `bulbs.yml`, the plugin will ignore the missing world, and those locations will not be loaded into memory — groups may become incomplete.
- Renaming groups uses chat input — other chat plugins that cancel/modify chat (chat channels, moderation, or format enforcement) might interfere; if rename fails, cancel or retype.
- The plugin uses persistent data container keys to detect wireless bulbs; removing or modifying these keys via other plugins may break detection.
- The plugin supports both waxed and non-waxed copper bulb material checks; however, the factory currently creates the waxed versions to avoid oxidation.
- Redstone lamp syncing uses `Lightable` block data. Physics options differ from copper bulbs set via `setBlockData(..., false/true)` — in rare cases, this may lead to different behavior or block updates. Expect slightly different sync behaviors across Minecraft versions.
- Players without `wirelessredstone.use` cannot use commands and will receive a permission-denied message.

---

## 🧩 Tips & Troubleshooting

- If items don't sync or show errors, check server logs for errors or stack traces — look for exceptions from `WirelessRedstone` plugin.
- If renaming doesn't work, check for chat plugins that intercept or cancel player's chat events.
- If a group persists after breaking all bulbs, check `plugins/WirelessRedstone/bulbs.yml` and remove entries manually if needed.
- If the GUI seems to not work or produce errors on startup, verify the plugin registers listeners and is started successfully (`onEnable` log entry).
- Use `/wireless debug on` to see sync messages and troubleshoot connection issues.

---

## 🆘 Developer Notes

- The code expects `api-version: 1.21` in `plugin.yml`.
- Bulbs use `PersistentDataContainer` keys defined in `LinkedBulbManager` to store group information (`wireless_bulb`, `group_id`, `bulb_index`, `bulb_type`, `owner`, `group_size`). Modifying the plugin to change or migrate keys must handle saved data.
- If you want to add custom behavior (e.g., linking to commands, additional bulb types, or config toggles), consider adding a `config.yml` and exposing config options for particle effects, debug radius, and storage location.

---

## 📄 License

(Include your preferred license here; e.g., MIT License)

---

If you’d like, I can also:

- Add a small `config.yml` to expose settings (e.g., particle intensity, message radius, save path).
- Improve edge-case handling for chunk unloading and cross-world pairing.
- Add a command to list/inspect a player's pairs and provide a JSON export.

If you want me to implement any of those suggestions, tell me which one and I’ll proceed.
