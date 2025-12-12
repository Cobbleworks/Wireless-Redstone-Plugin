# Wireless Redstone

Wireless Redstone is a Minecraft plugin that lets players create groups of "wireless" bulbs (waxed copper bulbs), redstone lamps, and storage containers (chests and shulker boxes) that synchronize across distance. Bulb groups sync their lit state, while container groups share their inventory contents. All data persists across restarts with a management GUI for easy control.

---

## 📦 Features

### Wireless Bulbs & Lamps

- Create linked groups of bulbs (copper bulbs or redstone lamps) with a single command.
- Support for multi-connections: groups can have 2-26 bulbs (A, B, C, D, etc.) that all sync together.
- When bulbs in a group are placed, they will synchronize their lit/unlit state automatically.
- Multiple copper bulb variants: normal, exposed, weathered, and oxidized.

### Wireless Containers (Chests & Shulker Boxes)

- Create linked groups of chests or shulker boxes that share their inventory contents.
- Support for all 17 shulker box colors plus regular chests.
- When items are added or removed from one container, all linked containers update automatically.
- Container groups support 2-26 instances syncing together.

### Management & GUI

- GUI-based management: teleport to bulbs, rename groups, remove groups, and set custom icons.
- Set custom group icons via Shift+Middle-click while holding an item.
- Visual particle effects for placement, sync, and break events.
- Removing a group also removes all placed physical blocks for a clean reset.
- Groups are automatically removed when the last instance is destroyed.
- Debug mode with `/wireless debug on|off` to show sync messages for nearby blocks.
- Data stored in `plugins/WirelessRedstone/bulbs.yml` and `chests.yml` to persist between restarts.

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

- `/wireless chests [count] [variant]`

  - Gives you a group of _linked storage containers_ (chests or shulker boxes).
  - `count` is optional and defaults to 2 (range: 2-26).
  - `variant` is optional and defaults to `--chest`. Supported variants:
    - `--chest` (default regular chest)
    - `--shulker` (default purple shulker box)
    - `--white`, `--orange`, `--magenta`, `--light-blue`, `--yellow`, `--lime`
    - `--pink`, `--gray`, `--light-gray`, `--cyan`, `--purple`, `--blue`
    - `--brown`, `--green`, `--red`, `--black`
  - Example: `/wireless chests 4 --cyan` gives you 4 linked cyan shulker boxes (A, B, C, D)
  - Example: `/wireless chests 2` gives you 2 linked regular chests (A, B)

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
- Both bulb groups (displayed with aqua color) and container groups (displayed with gold color) are shown together.
- Item interactions in the GUI:
  - **Left-click**: Teleport to first placed item in the group (requires `wirelessredstone.teleport`).
  - **Right-click**: Teleport to last placed item in the group (requires `wirelessredstone.teleport`).
  - **Middle-click**: Rename the group — rename is processed through the chat (type `cancel` to abort; type `reset` or `clear` to reset to default; max 32 characters).
  - **Shift+Right-click**: Set the group icon — hold any item in your hand and shift+right-click a group to set that item as the group's custom icon in the GUI.
  - **Shift+Left-click**: Remove the group and all its placed blocks (requires `wirelessredstone.remove`, or admin to remove other players' groups).
  - **Arrow icon**: next/previous page navigation.
  - **Book**: shows page info / group count.

---

## 🔁 How it Works (Implementation Summary)

### Wireless Bulbs

- Bulb groups are created with `/wireless bulbs [count]` or `/wireless lamps [count]` and carry metadata (group ID, index [A/B/C/...], owner, bulb type, group size).
- When a player places a wireless bulb, the `BlockPlaceEvent` registers the bulb's location in the server memory via `LinkedBulbManager`.
- Once multiple bulbs of a group are placed and loaded (chunks must be loaded), a sync task will mirror the `lit` state across all bulbs in the group.
- The `BulbSyncTask` runs every tick and does the following:
  - Spawn ambient particles for placed bulbs occasionally.
  - On each group where multiple bulbs are present and chunks are loaded, check their block states; if one changes, apply the same state to all others.
  - Use a `recentlySynced` set to avoid infinite sync loops.
  - Send debug messages to players within 3 blocks who have debug mode enabled, and spawn particle effects on sync.
- When a bulb is broken, only that bulb is unregistered from the group. If it was the last bulb, the group is automatically removed.
- When a group is removed via GUI, all physical bulb blocks are also removed from the world.
- All group data persists in `plugins/WirelessRedstone/bulbs.yml`.

### Wireless Containers

- Container groups are created with `/wireless chests [count] [variant]` and carry metadata (group ID, index, owner, container type, group size).
- When a player places a wireless container, the `BlockPlaceEvent` registers it via `LinkedChestManager` and restores the shared inventory.
- When items are added or removed from any container in a group, the `InventoryClickEvent` and `InventoryDragEvent` trigger synchronization to all other containers.
- The shared inventory is stored in memory and synced to all placed containers in real-time.
- When a container is broken, its contents are NOT dropped (since they're shared). The container is unregistered from the group.
- If it was the last container in a group, the group is automatically removed.
- All container group data persists in `plugins/WirelessRedstone/chests.yml`.

---

## 📝 Data / Configuration

- Bulb groups are saved in `bulbs.yml` with the following properties for each group:
  - `id`, `lit`, `bulbType`, `maxSize`, `owner`, `customName`, `customIcon`, `locations` (map of index to location).
- Container groups are saved in `chests.yml` with:
  - `id`, `containerType`, `maxSize`, `owner`, `customName`, `sharedInventory` (base64 encoded), `locations`.
- Location serialization uses `world,x,y,z`. If a world is missing (e.g. removed or renamed), that saved location will be ignored when loading.
- Old data format (pairs with loc1/loc2) is automatically migrated to the new group format on first load.

---

## 🖼️ Screenshot

Here is a screenshot showing linked bulbs.

![Wireless Bulb Screenshot](screenshot.png)

_Caption: A group of wireless bulbs showing synchronized lit state and the GUI title._

---

## 🚧 Edge Cases & Things to Watch Out For

### Bulbs

- Chunks must be loaded for syncing to occur. If bulbs are in an unloaded chunk, syncing will not happen until the chunk is loaded.
- Teleportation from the GUI requires `wirelessredstone.teleport` permission. If a player has no permission the teleport will be refused.
- When a player breaks a bulb, only that bulb is unregistered. If it's the last bulb in the group, the group is automatically removed.
- If the server renames or removes a world referenced in `bulbs.yml`, the plugin will ignore the missing world, and those locations will not be loaded into memory — groups may become incomplete.
- Renaming groups uses chat input — other chat plugins that cancel/modify chat (chat channels, moderation, or format enforcement) might interfere; if rename fails, cancel or retype.
- The plugin uses persistent data container keys to detect wireless bulbs; removing or modifying these keys via other plugins may break detection.
- The plugin supports both waxed and non-waxed copper bulb material checks; however, the factory currently creates the waxed versions to avoid oxidation.
- Redstone lamp syncing uses `Lightable` block data. Physics options differ from copper bulbs set via `setBlockData(..., false/true)` — in rare cases, this may lead to different behavior or block updates. Expect slightly different sync behaviors across Minecraft versions.
- Players without `wirelessredstone.use` cannot use commands and will receive a permission-denied message.

### Containers (Chests & Shulker Boxes)

- Wireless containers share their entire inventory. When one is broken, the contents stay in the shared inventory (not dropped).
- Breaking the last container in a group will remove the group and its shared inventory permanently.
- Chunks must be loaded for containers to receive inventory updates.
- Shulker boxes must be placed to access inventory — they cannot be opened from inventory like regular shulker boxes.
- Hopper interactions with wireless containers will trigger inventory sync to all other containers in the group.
- If the server renames or removes a world referenced in `chests.yml`, those locations will be ignored.

---

## 🧩 Tips & Troubleshooting

- If items don't sync or show errors, check server logs for errors or stack traces — look for exceptions from `WirelessRedstone` plugin.
- If renaming doesn't work, check for chat plugins that intercept or cancel player's chat events.
- Groups are now automatically removed when the last instance is broken — no need to manually clean `bulbs.yml` or `chests.yml`.
- If the GUI seems to not work or produce errors on startup, verify the plugin registers listeners and is started successfully (`onEnable` log entry).
- Use `/wireless debug on` to see sync messages and troubleshoot connection issues.
- For wireless containers, if inventory doesn't sync, ensure all container chunks are loaded.

---

## 🆘 Developer Notes

- The code expects `api-version: 1.21` in `plugin.yml`.
- Bulbs use `PersistentDataContainer` keys defined in `LinkedBulbManager` to store group information (`wireless_bulb`, `group_id`, `bulb_index`, `bulb_type`, `owner`, `group_size`). Modifying the plugin to change or migrate keys must handle saved data.
- Containers use similar PDC keys defined in `LinkedChestManager` (`wireless_chest`, `group_id`, `chest_index`, `container_type`, `owner`, `group_size`).
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
