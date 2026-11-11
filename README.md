# PlayerWarps

[![SpigotMC](https://img.shields.io/badge/SpigotMC-PlayerWarps-orange.svg)](https://www.spigotmc.org/resources/79089/)
[![Discord](https://img.shields.io/badge/Discord-Athelion-5865F2.svg)](https://discord.gg/TfUC8uJ)

PlayerWarps is a Minecraft plugin for warps that players create for each other. Players can set up warps to their shops, farms, builds and events, then manage them from an in-game menu. Other players browse, search, rate and save those warps.

> Want a fancier look? There is a [textured custom UI](https://builtbybit.com/resources/playerwarps-custom-ui.51704/) for PlayerWarps.

## Features

- **Player-created warps** with a display name, description, preview item, category and status (open, closed or password-protected)
- **Warp browsing**: categories, search, and sorting by name, visits, date or rating
- **Ratings and favorites**: players can review warps with stars and save the ones they like
- **Featured warps**: owners can pay to promote a warp to a featured slot for a limited time
- **Economy** through [Vault](https://www.spigotmc.org/resources/34315/):
  - creation pricing (free, fixed, or a custom formula based on how many warps the player owns)
  - fees for renaming, relocating and other changes
  - an admission fee that the owner charges visitors
  - a refund when a warp is deleted
- **Owner tools**: rename, relocate, transfer ownership and block specific players
- **Safe teleports**: a configurable delay that is cancelled when the player takes damage, plus a check for dangerous destinations
- **Claim protection**: warps can only be created inside your own (or trusted) claims with WorldGuard, GriefPrevention, Residence, Lands, Towny, Territory, BentoBox and SuperiorSkyblock2
- **Map markers** on [dynmap](https://www.spigotmc.org/resources/274/), [BlueMap](https://www.spigotmc.org/resources/83557/) and [squaremap](https://modrinth.com/plugin/squaremap)
- **Custom items** from ItemsAdder, Nexo, Oraxen and HeadDatabase, usable as warp icons and anywhere in the configs
- **Three input modes** for typing values: chat, a sign GUI, or a dialog window (Paper 1.21.6+)
- **Fully customizable menus** in the `guis/` folder, and HEX colors (`<#ffffff>`) on 1.16+
- **11 languages**: English, Czech, Chinese, French, German, Italian, Polish, Portuguese, Russian, Spanish and Turkish

## Installation

1. Download the latest release from [SpigotMC](https://www.spigotmc.org/resources/79089/).
2. Put the jar in your server's `plugins/` folder. The server needs **Java 21**.
3. Optional: install Vault and an economy plugin to enable prices and fees.
4. Start the server, edit the files in `plugins/PlayerWarps/` and run `/pwarp reload`.

## Commands

The main command is `/pwarp`, with the aliases `/pw` and `/playerwarps`.

| Command | Description | Permission |
|---|---|---|
| `/pwarp` | Opens the warps menu (or the categories menu if categories are enabled) | `playerwarps.use` |
| `/pwarp <warp> [player]` | Teleports you, or another player, to a warp | `playerwarps.use` / `playerwarps.teleport.others` |
| `/pwarp create <name>` | Creates a warp where you are standing | `playerwarps.create` |
| `/pwarp manage <warp>` | Opens the management menu for a warp | `playerwarps.manage` |
| `/pwarp mywarps` | Opens the menu with your own warps | `playerwarps.mywarps` |
| `/pwarp remove <warp>` | Deletes a warp | `playerwarps.remove` |
| `/pwarp open <warps\|my_warps\|saved_warps>` | Opens a specific menu | `playerwarps.open` |
| `/pwarp reload` | Reloads the configuration | `playerwarps.reload` |
| `/pwarp about` | Shows information about the plugin | `playerwarps.about` |
| `/pwarp help` | Lists the available commands | `playerwarps.help` |

## Permissions

`playerwarps.use`, `playerwarps.create`, `playerwarps.mywarps` and `playerwarps.settings` are granted to everyone by default.

| Permission | Description |
|---|---|
| `playerwarps.admin` | Grants every permission below |
| `playerwarps.use` | Use the warps menu and teleport to warps |
| `playerwarps.teleport.others` | Teleport other players to a warp |
| `playerwarps.create` | Create warps |
| `playerwarps.remove` | Remove your warps |
| `playerwarps.mywarps` | Open the menu with your own warps |
| `playerwarps.manage` | Manage your warps |
| `playerwarps.manage.others` | Manage warps owned by other players |
| `playerwarps.open` | Open menus with `/pwarp open` |
| `playerwarps.favorite` | Save warps to favorites |
| `playerwarps.review` | Rate warps |
| `playerwarps.feature` | Feature a warp |
| `playerwarps.rename` / `.relocate` / `.transfer` | Rename, move or hand over a warp |
| `playerwarps.settings` | Open the settings of your warps |
| `playerwarps.settings.changedisplayname` | Change a warp's display name |
| `playerwarps.settings.setdescription` | Change a warp's description |
| `playerwarps.settings.setitem` | Change a warp's preview item |
| `playerwarps.settings.settype` | Change a warp's category |
| `playerwarps.settings.setstatus` | Open, close or password-protect a warp |
| `playerwarps.settings.setadmission` | Set an admission fee |
| `playerwarps.settings.blockPlayer` | Block players from a warp |
| `playerwarps.icon.<item>` / `playerwarps.icon.*` | Use a specific item, or any item, as a warp icon (e.g. `playerwarps.icon.diamond`, `playerwarps.icon.namespace.ruby` for ItemsAdder) |
| `playerwarps.limit.<number>` / `playerwarps.limit.unlimited` | Maximum number of warps a player can own (the default comes from `default-limit-size`) |
| `playerwarps.category.<name>` | Use a category (set per category in `categories.yml`) |
| `playerwarps.delay.bypass` | Teleport without the delay |
| `playerwarps.verify` | Verify warps |

## Configuration

| File | Contents |
|---|---|
| `config.yml` | General settings: language, input mode, pricing and fees, limits, teleport behavior, hooks and menu items |
| `categories.yml` | Warp categories with their name, item, menu position, lore and permission |
| `guis/*.yml` | Layout and items of every menu |
| `lang/*.yml` | Messages in each language |
| `data.yml` | Stored warps (do not edit while the server is running) |

### Pricing

```yaml
free-warps: 1
warp-pricing: CUSTOM                            # FREE, FIXED or CUSTOM
warp-fixed-price: 100000                        # used with FIXED
warp-custom-price: "100000 + ({amount} * 20000)" # used with CUSTOM, {amount} = warps the player owns
```

Each fee (`rename-warp-fee`, `relocate-warp-fee`, `feature-warp-fee`, ...) can be disabled by setting it to `0`. Prices and fees only work with Vault.

### Hooks

Every integration has its own switch, e.g. `world-guard-hook-enabled: true`. Inside a WorldGuard region, only the region's owners can create warps, unless the region is listed in `enabled-regions`. With `warp-creation-for-trusted-players: true`, players trusted in a claim can create warps there too.

## Placeholders

These need PlaceholderAPI.

| Placeholder | Value |
|---|---|
| `%playerwarps_total_warps%` | Number of warps on the server |
| `%playerwarps_visits%` | Total visits across all warps |
| `%playerwarps_visits_<warp>%` | Visits of a single warp |
| `%playerwarps_unique_visits%` | Unique visits across all warps |
| `%playerwarps_unique_visits_<warp>%` | Unique visits of a single warp |

## Building

You need JDK 21 and Maven:

```bash
mvn clean package
```

The jar is written to `target/`. Some hook dependencies that are not available in public repositories are kept in `libs/`.

## Support

Questions, bug reports and suggestions are welcome on our [Discord](https://discord.gg/TfUC8uJ) or in [GitHub issues](https://github.com/Athelion-Development/PlayerWarps/issues).
