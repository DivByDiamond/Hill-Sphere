# Hill Sphere

A NeoForge 1.21.1 mod about placeable gravity cores: a powered core makes "down" point at
itself (or away from it, or nowhere) inside its field. Built on [Create](https://github.com/Creators-of-Create/Create).

**Status: beta.** Most gameplay works and is covered by unit and game tests, but plenty of it
has not seen long play sessions yet. See [CHANGELOG.md](CHANGELOG.md).

## Features

- **Hill Core** block, spun by Create rotation: speed sets the radius of the field, the settings
  screen (use the block) sets polarity, strength and redstone behavior
- Two shapes: a **sphere** whose "down" points at the core, or a **deck** with one "down" all over a box,
  which on a ship covers the whole ship
- Players, mobs, items and projectiles fall toward (or away from) the nearest powered core, walk on
  walls and ceilings, and float in weightless fields
- **Gravite** ore with a Create processing chain, and **Goggles** that show the fields
- Optional [Sable](https://modrinth.com/mod/sable) support: cores on ships take their field along and can hold a ship up

## Requirements

- Minecraft 1.21.1, NeoForge 21.1.256+, Java 21
- Create 6.0.10+ (required)
- Sable 2.0.6+ (optional)

## Configuration

Server config `hillsphere-server.toml` holds every number of the field. Two options help when
playing: `noFallDamageInField` turns fall damage off inside fields, `debug` writes detailed
`[HSDBG]` lines to the log.

## Building

```bash
./gradlew build check              # jar in neoforge/build/libs/
./gradlew :neoforge:runClient
./gradlew :neoforge:runGameTestServer
```

Releases: push a tag like `0.1.0-beta.1`; GitHub Actions builds the jar and publishes a GitHub
release (and uploads to CurseForge when `CURSEFORGE_ID` and `CURSEFORGE_API_KEY` are set).
Design notes live in [docs/CONCEPT.md](docs/CONCEPT.md), open work in [todo.md](todo.md).

## Credits and license

GPL-3.0-or-later, see [LICENSE](LICENSE). Project layout from
[MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template) (CC0).
