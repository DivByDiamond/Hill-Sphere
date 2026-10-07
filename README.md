# Hill Sphere

Minecraft mod (1.21.1, NeoForge) about placeable gravity cores:
a powered core makes "down" point at itself inside its field.

**Status: early development. There is no gameplay yet** - the repository contains
only the build setup (Gradle, Checkstyle, PMD, Spotless, CI).
Nothing below the "Planned" heading exists in code.

## Planned

- A gravity core block powered by Create's rotation: speed sets the field radius, a strength
  setting sets the pull, polarity (attract / repel) is switched on the block, redstone turns the field off
- Goggles that make the fields visible
- A progression that starts from one ore found in the world
- Gravity acting on ships through Sable's physics, camera tilt through Aeronautics Camera Sync

The design is still being worked out. See [docs/CONCEPT.md](docs/CONCEPT.md).

## Requirements

- Java 21, Minecraft 1.21.1
- NeoForge 21.1.256+
- Required add-ons (planned): Create 6, Sable, Aeronautics Camera Sync

## Building

```bash
./gradlew build                  # all modules, jars in <module>/build/libs/
./gradlew :neoforge:runClient
./gradlew check                  # checkstyle + pmd + spotless
```

## Credits and license

GPL-3.0-or-later, see [LICENSE](LICENSE).

- Project layout from [MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template) (CC0)
