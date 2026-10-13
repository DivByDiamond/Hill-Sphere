# Changelog

## 0.1.0-beta.1

First public test build. Expect rough edges, especially in gravity on walls and ceilings
and in the ship integration; please report them as issues.

- Hill Core: powered by Create rotation; speed sets the field radius, a settings screen sets
  polarity (attract / repel / weightless), strength (fractional, 0-6) and redstone mode
- Gravity "down" points at the nearest powered core for players, mobs, items, falling blocks and
  projectiles; walking on walls and ceilings, turning camera and body
- Weightlessness fades with altitude and is limited by core radius and body mass
- Gravite ore, ingot, block, plate and molten gravite with a Create processing chain
- Goggles: helmet with field view layers (flow, surfaces, boundaries) and a core HUD
- Optional Sable support: cores on ships carry their field, ships can be held up or pulled
- Server config for every number; `noFallDamageInField` and `debug` options
