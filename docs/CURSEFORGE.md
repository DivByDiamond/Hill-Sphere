# CurseForge page text

## Summary (max 200 characters)

Gravity cores for Create: "down" points at the nearest powered core. Walk on walls and ceilings, float in zero-g, hold ships up. Optional Sable support.

## Description

![Hill Sphere](https://raw.githubusercontent.com/DivByDiamond/Hill-Sphere/main/docs/art/avatar.png)

# Hill Sphere

**Gravity cores for Create.** Place a **Hill Core**, spin it with a shaft, and everything inside its field falls toward it, away from it, or not at all.

> **Beta.** Core features work and are covered by automated tests, but long play sessions are still rare. Expect rough edges around walls, ceilings and ships, and please report them on the issue tracker.

## Features

- **Hill Core.** Powered by Create rotation: the faster the shaft, the bigger the field. Use the block to open its screen and set the polarity (**attract**, **repel**, **weightless**), the strength (fractional, 0 to 6) and how redstone controls it.
- **Real gravity direction.** Players, mobs, items, falling blocks and projectiles fall toward the nearest powered core. Walk on walls and ceilings; camera, body model, jumping and block placement turn with you.
- **Weightlessness.** Float, drift, push off with Space and Shift. The effect fades with altitude and is limited by how big the core is and how heavy the thing is, so a tiny core won't lift a giant ship.
- **Gravite.** A new ore in deepslate with its own Create processing chain: crushing, washing, mixing and molten gravite.
- **Goggles.** A helmet that shows the fields: flow lines, standing surfaces and field boundaries, plus a core readout on your HUD.
- **Sable support (optional).** Cores on ships carry their field along; weightless cores hold ships up and attracting cores pull ships together.
- **Configurable.** Every number lives in the server config, including an option to disable fall damage inside fields.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.256+ |
| Create | 6.0.10+ (required) |
| Sable | 2.0.6+ (optional) |

## Getting started

1. Find gravite ore in deepslate and process it with Create machines.
2. Craft a Hill Core and connect a shaft or motor to its axis.
3. Right-click the core to choose polarity, strength and redstone mode.
4. Craft the goggles to see what you are doing.

## Links

- [Source code and issues](https://github.com/DivByDiamond/Hill-Sphere)
- License: GPL-3.0-or-later
