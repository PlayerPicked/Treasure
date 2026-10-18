# Treasure

Treasure chests for Paper and Folia: every player loots their own copy of a chest, then waits for a per-player cooldown. Chests can have a hologram, a particle effect, a permission and a pushback, and a tracker compass points players to the nearest one.

- **Server:** Paper or Folia 1.21.8 and newer
- **Java:** 21+
- **Optional:** [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)

## Features

- Per-player loot and cooldowns. The cooldown starts when the player takes the first item.
- Holograms built from text displays, with no hologram plugin needed. They are rendered for each player, so `<time-left>` shows that player's own cooldown.
- Seven particle shapes: helix, heart, pulsar, sphere, tornado, phoenix and spiral.
- A tracker compass that shows distance and direction to the nearest treasure in the action bar.
- An in-game setup menu for name, permission, cooldown, size, location, pushback, hologram lines, particles and loot.
- Treasure blocks cannot be broken or blown up.
- Cooldowns are stored in H2 (a local file, no setup needed) or MySQL.

## Commands and permissions

| Command | Permission | Description |
|---|---|---|
| `/treasure` or `/treasure setup` | `treasure.setup` | Opens the setup menu |
| `/treasure give tracker <player>` | `treasure.tracker` | Gives a treasure tracker |
| `/treasure reload` | `treasure.reload` | Reloads `config.yml`, `messages.yml` and `guis.yml` |
| - | `treasure.update` | Notifies the player about new versions when they join |

You can change the command aliases under `command-aliases` in `config.yml` (default: `treasure`, `treasurechest`).

## Setting up a treasure

1. Run `/treasure setup` and click **Create treasure**.
2. Click **Set location**, then break the block that should become the treasure.
3. Click **Set loot**, put the items in, and close the inventory to save.
4. Set the cooldown, permission, hologram and particles as you like. Every change is saved right away.

## Hologram lines

Lines are MiniMessage. Besides normal formatting you can use:

| Tag | Value |
|---|---|
| `<name>` | The treasure's name |
| `<time-left>` | The viewer's remaining cooldown, or the "ready" text from `messages.yml` |
| `%any_placeholder%` | Any PlaceholderAPI placeholder, resolved per viewer (needs PlaceholderAPI) |

## PlaceholderAPI

| Placeholder | Value |
|---|---|
| `%treasure_time_left_<id>%` | The player's remaining cooldown for that treasure |
| `%treasure_remaining_<world>%` | Treasures in the world the player can open now |
| `%treasure_opened_<world>%` | Treasures in the world the player is on cooldown for |
| `%treasure_all_<world>%` | All treasures in the world |

The treasure id is shown in the setup menu.

## Building

```bash
./gradlew build
```

The plugin jar is `build/libs/Treasure-<version>.jar`.

## License

MIT, see [LICENSE](LICENSE).
