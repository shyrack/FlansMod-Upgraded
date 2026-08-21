# FlansMod-Upgraded

[![CurseForge](https://img.shields.io/badge/CurseForge-FlansMod--Upgraded-f16436?logo=curseforge)](https://www.curseforge.com/minecraft/mc-mods/flans-mod-upgraded)
[![CurseForge Downloads](https://cf.way2muchnoise.eu/full_flans-mod-upgraded_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/flans-mod-upgraded)
[![GitHub release](https://img.shields.io/github/v/release/shyrack/FlansMod-Upgraded.svg?include_prereleases&sort=semver&color=brightgreen)](https://github.com/shyrack/FlansMod-Upgraded/releases/)
[![GitHub stars](https://img.shields.io/github/stars/shyrack/FlansMod-Upgraded.svg?color=brightgreen)](https://github.com/shyrack/FlansMod-Upgraded/stargazers/)
[![GitHub forks](https://img.shields.io/github/forks/shyrack/FlansMod-Upgraded.svg?color=brightgreen)](https://github.com/shyrack/FlansMod-Upgraded/network/)
[![GitHub issues](https://img.shields.io/github/issues/shyrack/FlansMod-Upgraded.svg)](https://github.com/shyrack/FlansMod-Upgraded/issues/)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-blue)](https://www.minecraft.net)
[![License: CC BY-NC-SA 3.0](https://img.shields.io/badge/License-CC%20BY--NC--SA%203.0-lightgrey.svg)](LICENSE.txt)

**FlansMod-Upgraded** is a community project by
[shyrack](https://github.com/shyrack) that brings
[Flan's Mod](https://www.flansmod.com/) — guns, planes, and other vehicles in
Minecraft — to modern versions of the game.

This is a full Fabric port of Flan's Mod, built and tested against Minecraft
**26.1.2**. It preserves the classic Flan's Mod experience — content packs,
crafting benches, driveable vehicles, and team-based game modes — while
rebuilding the engine underneath for the modern rendering pipeline, modern
registries, and modern networking.

## Download

The recommended way to install the mod is through CurseForge:

- [FlansMod-Upgraded on CurseForge](https://www.curseforge.com/minecraft/mc-mods/flans-mod-upgraded)

Pre-release and development builds can also be found on the
[GitHub releases page](https://github.com/shyrack/FlansMod-Upgraded/releases).

## Requirements

| Dependency   | Version          |
| ------------ | ---------------- |
| Minecraft    | **26.1.2**       |
| Fabric Loader| >= **0.19.3**    |
| Fabric API   | >= **0.152.1**   |
| Java         | **25**           |

## Features

- **Guns** — pistols, rifles, SMGs, shotguns, sniper rifles, and LMGs with
  interchangeable attachments (scopes, grips, barrels, magazines), fire mode
  selection (semi-auto, full-auto, burst), and custom spread patterns.
- **Grenades & ordnance** — throwable explosives, sticky grenades, and
  anti-aircraft guns (AAGuns).
- **Driveables** — planes, vehicles, and mechas with multi-seat support,
  wheels, propellers, per-part damage, and collision physics.
- **Paintjobs** — paint your guns and vehicles with rarity-graded paintjobs.
- **Parts & crafting** — build and modify vehicles and mechas from individual
  parts at the Flan's Mod workbenches; gun modification tables for attachments.
- **Teams** — team management with classes, loadouts, flagpoles, spawners,
  armour boxes, and reward boxes.
- **Game modes** — Deathmatch, Team Deathmatch, Capture the Flag, and Zombies.
- **Flan's Mod: Apocalypse** — the post-apocalyptic dimension, included in the
  same jar: wastelands, zombies, and apocalypse loot.
- **Content pack system** — all content is data-driven; add or create packs by
  dropping them into your `Flan` folder.

## Content Packs

The following content packs ship with the port and are verified working:

| Pack | Content |
| ---- | ------- |
| Modern Weapons Pack | Modern firearms, attachments, grenades, vehicles |
| WW2 Pack | WWII weapons, planes, and vehicles |
| Titan Pack | Titans and mecha parts |
| Mecha Parts Pack | Additional mecha components |
| Steampunk Pack | Steampunk guns and gear |
| Nerf Pack | Nerf-style blasters |
| Ye Olde Pack | Ye olde weaponry |
| Utility Pack | Tools and utility items |
| Parts Pack | Vehicle and plane parts |
| Apocalypse Pack | Dimension content, zombies, wasteland loot |

## Installation

1. Install the [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.1.2.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) (0.152.1 or
   newer) and place it in your `mods` folder.
3. Download FlansMod-Upgraded from
   [CurseForge](https://www.curseforge.com/minecraft/mc-mods/flans-mod-upgraded)
   and place the jar in your `mods` folder. A single jar contains both
   `flansmod` and `flansmodapocalypse`.
4. Place your content packs inside `./Flan/` in your Minecraft directory.
5. Launch the game and enjoy.

## Building from Source

The project uses Gradle 9 with the Fabric Loom toolchain. Java 25 is required.

1. Clone this repository:
   ```bash
   git clone https://github.com/shyrack/FlansMod-Upgraded.git
   ```
2. Run the build:
   - Linux/Mac: `./gradlew build`
   - Windows: `gradlew.bat build`
3. The built jar is found in `./build/libs`.

### Development Tasks

| Command | Description |
| ------- | ----------- |
| `./gradlew build` | Build the mod jar |
| `./gradlew test` | Run the unit and integration test suite |
| `./gradlew runClient` | Launch a client with the mod loaded |
| `./gradlew runServer` | Launch a server with the mod loaded |

The test suite uses JUnit 5 and Mockito, and runs against the remapped jar.

## Contributing

Bug reports and pull requests are welcome. Please open an issue on the
[GitHub issue tracker](https://github.com/shyrack/FlansMod-Upgraded/issues)
describing the problem or the change you want to make before starting larger
work. When reporting a bug, include your Minecraft version, Fabric Loader
version, mod version, and a crash report if one was generated.

## Credits

- **Original Flan's Mod** — [jamioflan](https://github.com/jamioflan) and the
  [FlansMods](https://github.com/FlansMods) team. This project would not exist
  without their years of work.
- **Port & modernisation** — [shyrack](https://github.com/shyrack).
- **Content packs** — the many pack authors from the Flan's Mod community.

## License

Licensed under **CC BY-NC-SA 3.0**. See [LICENSE.txt](LICENSE.txt) for the
full text. Third-party components retain their own licenses; see
`src/main/resources` for bundled third-party notices.
