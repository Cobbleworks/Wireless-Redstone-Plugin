# Wireless Redstone

Wireless Redstone is a simple Minecraft plugin that lets players create pairs of "wireless" bulbs (waxed copper bulbs) or redstone lamps that mirror each other's lit state across distance. Bulb pairs persist across restarts and provide a small management GUI for renaming, teleporting, and controlling sync messages.

---

## 📦 Features

- Create linked pairs of bulbs (copper bulbs or redstone lamps) with a single command.
- When both bulbs are placed, they will synchronize their lit/unlit state automatically.
- GUI-based management: teleport to bulbs, rename pairs, toggle sync messages, and remove pairs.
- Visual particle effects for placement, sync, and break events.
- Bulb pair data is stored in `plugins/WirelessRedstone/bulbs.yml` to persist between restarts.

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

- `/wireless bulbs [variant]`

  - Gives you a pair of _linked copper bulbs_.
  - `variant` is optional and defaults to `--copper`. Supported copper variants:
    - `--copper` (default)
    - `--exposed` (exposed copper)
    - `--weathered` (weathered copper)
    - `--oxidized` (oxidized copper)
  - Example: `/wireless bulbs --exposed`

- `/wireless lamps`

  - Give a pair of _linked redstone lamps_.

- `/wireless gui [--all]`

  - Opens the Bulb Manager GUI. If `--all` is specified and the player has admin permissions, the GUI shows every placed pair on the server.

- `/wireless wireview`
  - Displays all connected bulb pairs with a glowing outline effect.

### Command Notes

- If a player's inventory is full when receiving bulbs the plugin will spawn the items on the ground at the player's location.
- Tab completion is provided for subcommands and variants.

---

## 🔐 Permissions

Permissions are defined in `plugin.yml`:

- `wirelessredstone.use` — Allows the player to use `/wireless` commands. (Default: op)
- `wirelessredstone.teleport` — Allows teleporting to a bulb location via the GUI. (Default: op)
- `wirelessredstone.remove` — Allows removing bulb pairs via the GUI. (Default: op)
- `wirelessredstone.admin` — Full admin control (see GUI `--all` view). (Default: op)

Note: You can change defaults or assign these permissions using a permissions plugin such as LuckPerms.

---

## 🧭 GUI (`/wireless gui`) — Controls / Interactions

- The GUI shows a paginated list of all pairs owned by the player (or all pairs if admin and `--all` is used).
- Item interactions in the GUI:
  - Left-click: Teleport to Location A (requires `wirelessredstone.teleport`).
  - Right-click: Teleport to Location B (requires `wirelessredstone.teleport`).
  - Middle-click: Toggle sync messages for that pair (visible in the GUI lore).
  - Drop key (Q): Rename the pair — rename is processed through the chat (type `cancel` to abort; type `reset` or `clear` to reset to default; max 32 characters).
  - Shift+Click: Remove the pair (requires `wirelessredstone.remove`, or admin to remove other players' pairs).
  - Arrow icon: next/previous page navigation.
  - Book: shows page info / pair count.

---

## 🔁 How it Works (Implementation Summary)

- Bulb pairs are created with `/wireless bulbs` or `/wireless lamps` and carry metadata (pair ID, index [A/B], owner, bulb type).
- When a player places a wireless bulb, the `BlockPlaceEvent` registers the bulb's location in the server memory via `LinkedBulbManager`.
- Once both bulbs of a pair are placed and loaded (chunks must be loaded), a sync task will mirror the `lit` state of either bulb to its linked pair.
- The `BulbSyncTask` runs every tick and does the following:
  - Spawn ambient particles for placed bulbs occasionally.
  - On each pair where both bulbs are present and chunks are loaded, check their block states; if one changes, apply the same state to the other.
  - Use a `recentlySynced` set to avoid infinite sync loops.
  - Send sync messages to nearby players within 8 blocks (can be toggled per pair), and spawn particle effects on sync.
- When one bulb of a linked pair is broken, the plugin tries to break the linked partner to avoid orphaned pairs (it verifies the partner is a compatible block and performs a scheduled tick to trigger a natural break so it drops correctly).
- All pair data persists in `plugins/WirelessRedstone/bulbs.yml`.

---

## 📝 Data / Configuration

- Bulb pairs are saved in `bulbs.yml` with the following properties for each pair:
  - `id`, `lit`, `bulbType`, `showSyncMessages`, `owner`, `customName`, `loc1`, `loc2`.
- Location serialization uses `world,x,y,z`. If a world is missing (e.g. removed or renamed), that saved location will be ignored when loading.

---

## 🚧 Edge Cases & Things to Watch Out For

- Chunks must be loaded for syncing to occur. If one or both bulbs are in an unloaded chunk, syncing will not happen until the chunk is loaded.
- Teleportation from the GUI requires `wirelessredstone.teleport` permission. If a player has no permission the teleport will be refused.
- When a player breaks a bulb, the plugin will attempt to break the other bulb if it exists and is still a compatible block. If the other bulb is in an unloaded chunk or has been replaced or is not a wireless-compatible block, it may not break or the pair may remain until next server restart.
- If the server renames or removes a world referenced in `bulbs.yml`, the plugin will ignore the missing world, and those locations will not be loaded into memory — pairs may become incomplete.
- Renaming pairs uses chat input — other chat plugins that cancel/modify chat (chat channels, moderation, or format enforcement) might interfere; if rename fails, cancel or retype.
- The plugin uses persistent data container keys to detect wireless bulbs; removing or modifying these keys via other plugins may break detection.
- The plugin supports both waxed and non-waxed copper bulb material checks; however, the factory currently creates the waxed versions to avoid oxidation.
- Redstone lamp syncing uses `Lightable` block data. Physics options differ from copper bulbs set via `setBlockData(..., false/true)` — in rare cases, this may lead to different behavior or block updates. Expect slightly different sync behaviors across Minecraft versions.
- Players without `wirelessredstone.use` cannot use commands and will receive a permission-denied message.

---

## 🧩 Tips & Troubleshooting

- If items don't sync or show errors, check server logs for errors or stack traces — look for exceptions from `WirelessRedstone` plugin.
- If renaming doesn't work, check for chat plugins that intercept or cancel player's chat events.
- If a pair persists after breaking both bulbs, check `plugins/WirelessRedstone/bulbs.yml` and remove entries manually if needed.
- If the GUI seems to not work or produce errors on startup, verify the plugin registers listeners and is started successfully (`onEnable` log entry).

---

## 🆘 Developer Notes

- The code expects `api-version: 1.21` in `plugin.yml`.
- Bulbs use `PersistentDataContainer` keys defined in `LinkedBulbManager` to store pair information (`wireless_bulb`, `pair_id`, `bulb_index`, `bulb_type`, `owner`). Modifying the plugin to change or migrate keys must handle saved data.
- If you want to add custom behavior (e.g., linking to commands, additional bulb types, or config toggles), consider adding a `config.yml` and exposing config options for particle effects, messaging radius, and storage location.

---

## 📄 License

(Include your preferred license here; e.g., MIT License)

---

If you’d like, I can also:

- Add a small `config.yml` to expose settings (e.g., particle intensity, message radius, save path).
- Improve edge-case handling for chunk unloading and cross-world pairing.
- Add a command to list/inspect a player's pairs and provide a JSON export.

If you want me to implement any of those suggestions, tell me which one and I’ll proceed.
