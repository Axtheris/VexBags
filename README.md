## VexBags

Supercharged, tiered backpacks for modern Paper servers. Gorgeous in-game UI, intuitive deposit/withdraw controls, safe-by-design item signing, and upgradeable tiers from Leather to Netherite.

### Shields

[![GitHub Stars](https://img.shields.io/github/stars/Axtheris/VexBags?style=for-the-badge)](https://github.com/Axtheris/VexBags/stargazers)
[![Issues](https://img.shields.io/github/issues/Axtheris/VexBags?style=for-the-badge)](https://github.com/Axtheris/VexBags/issues)
[![Last Commit](https://img.shields.io/github/last-commit/Axtheris/VexBags?style=for-the-badge)](https://github.com/Axtheris/VexBags/commits/master)
[![License](https://img.shields.io/github/license/Axtheris/VexBags?style=for-the-badge)](https://github.com/Axtheris/VexBags)

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Minecraft](https://img.shields.io/badge/Paper-1.21.x-3B8EEA?style=for-the-badge)](https://papermc.io/)
[![API](https://img.shields.io/badge/API-Paper%201.21.8-3B8EEA?style=for-the-badge)](https://papermc.io/)
[![Build](https://img.shields.io/badge/build-Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Version](https://img.shields.io/badge/version-1.0-1E90FF?style=for-the-badge)](https://github.com/Axtheris/VexBags)
[![Modrinth](https://img.shields.io/modrinth/dt/vexbags?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/vexbags)
[![Ko-fi](https://img.shields.io/badge/Ko--fi-Support%20the%20Developer-ff5e5b?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/axther)

Repository: [Axtheris/VexBags](https://github.com/Axtheris/VexBags)

## Features at a glance

- **Tiered backpacks**: Leather → Copper → Iron → Gold → Diamond → Netherite
- **Beautiful GUI**: compact grid that summarizes your stored items and totals
- **Dual progress bars**: unique item types and overall capacity (items out of max)
- **Smart controls**: fast deposit/withdraw mappings with consistent 1/8/16/32/64 steps
- **Craftable and upgradeable**: simple base recipe; upgrades require the previous tier
- **Recipe discovery**: auto-unlocked for players on join
- **Dupe-safe**: server-signed backpack items with verification before use
- **Precise stacking**: items tracked by canonical stack signature (includes meta), not just material
- **Persistent storage**: fast YAML-backed storage with debounced async saves

## Requirements

- Paper 1.21.x (Paper API 1.21.8)
- Java 21 (JDK 21)

## Installation

1. Build or download the plugin JAR
2. Place the JAR into your server's `plugins/` folder
3. Start/restart the server
4. A `plugins/VexBags/config.yml` will be generated with a random `secret` used to sign backpacks

## Building from source

```bash
mvn clean package
```

The shaded JAR will be available in `target/`.

## Downloads

Prefer not to build from source?

- GitHub Releases: [Download prebuilt JARs](https://github.com/Axtheris/VexBags/releases)
- Modrinth: [VexBags on Modrinth](https://modrinth.com/plugin/vexbags)

## Commands

- `vexbags [tier]`
  - Gives the executing player a backpack. Optional `tier` defaults to `leather`.
  - Aliases: `vbag`, `vbags`, `vb`
- `vexbagsadmin <list|open|delete|restore> ...`
  - `list <player>`: list backpack IDs owned by player
  - `open <uuid>`: open the backpack GUI by ID (player only)
  - `delete <uuid>`: remove a backpack from storage
  - `restore <uuid> <player>`: give a new item that points to an existing backpack ID

## Permissions

- `vexbags.command`: use `/vexbags` (default: op)
- `vexbags.admin`: use admin subcommands (default: op)

## Controls (GUI)

- Left-click: withdraw 1
- Right-click: withdraw 8
- Shift + Left-click: withdraw 16
- Shift + Right-click: withdraw 32
- Middle-click: withdraw 64
- F (swap-offhand) on a slot: withdraw all from that entry

Depositing from player inventory:

- Left-click item: deposit 1
- Right-click item: deposit up to 8 (or current stack amount)
- Shift + Left-click: deposit up to 16
- Shift + Right-click: deposit up to 32
- Middle-click: deposit up to 64
- F (swap-offhand) on an item: deposit the entire selected stack

## Crafting and Upgrades

Backpacks are crafted and upgraded with shaped recipes. All recipes are namespaced and auto-discovered for players.

- Base (Leather):
  - Shape: `LLL / LCL / LLL`
  - `L = LEATHER`, `C = CHEST`
- Upgrades (to Copper/Iron/Gold/Diamond/Netherite):
  - Shape: `III / IBI / III`
  - `I = tier material` (e.g., `COPPER_INGOT`, `IRON_INGOT`, ...)
  - `B = previous-tier backpack`
  - The result keeps the same backpack ID, upgrading in place

## Tiers

| Tier | Color | Upgrade Material | Storage Slots | Upgrade Slots |
| ---- | ----- | ---------------- | ------------- | ------------- |
| Leather | #a87c5a | Leather (base) | 9 | 1 |
| Copper | #b87333 | Copper Ingot | 18 | 2 |
| Iron | #c7c7c7 | Iron Ingot | 27 | 3 |
| Gold | #ffd700 | Gold Ingot | 36 | 4 |
| Diamond | #3de2ff | Diamond | 45 | 5 |
| Netherite | #6b3fa0 | Netherite Ingot | 54 | 6 |

## Data and persistence

- Data folder: `plugins/VexBags/`
- Primary store: `backpacks.yml`
- Owner index: `index.yml`
- Items persist as a map of `stackKey -> count` where `stackKey` is a canonical, hashed signature of the item stack (includes meta/NBT). Legacy material-only entries are auto-read when possible.
- Saves are debounced and run asynchronously to minimize disk churn

## Security and anti-dupe

- Each backpack item is signed with a server-only `secret` plus internal fields
- Signatures are verified on use; failed verification prevents opening
- The `secret` is generated on first run and stored in `config.yml`

## API and internals (for developers)

- Main plugin: `com.axther.vexBags.VexBags`
- Keys: `backpack_id`, `backpack_tier`, `backpack_ver`, `backpack_sess`, `backpack_sig`
- Storage model: YAML of per-backpack entries keyed by `stackKey` (canonical serialized stack + SHA-256) with counts; includes owner index
- UI: dynamic inventory sized to tier capacity; GUI shows top items and both unique-type and capacity progress bars

## Roadmap ideas

- Upgrades system for quality-of-life perks (filters, magnets, compaction)
- Permission-based tier caps and per-world rules
- Economy hooks for upgrades

## Contributing

Issues and pull requests welcome. Please open an issue to discuss substantial changes.

## Support development

If you'd like to support my plugin development and further development of VexBags, please consider donating:

- Ko‑fi: [https://ko-fi.com/axther](https://ko-fi.com/axther)

## License

This repository does not currently include a license file. If you intend to use or redistribute this code, please contact the authors or open an issue to clarify licensing.

## Links

- GitHub Repository: [Axtheris/VexBags](https://github.com/Axtheris/VexBags)


