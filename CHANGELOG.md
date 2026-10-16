# Changelog

## 0.1.0-beta.2

Second test build: gravity on ships and walls reworked, plus new goggle options. Still rough; please report
what you find.

- Hill Core shape: "Sphere" or "Hull". A hull core fills a box: on a Sable ship the whole ship (turned with
  it), so a ship of any shape has a floor; on the ground a cube. New cores start at level 4 (1 g)
- "Down" turns toward a floor under the feet: step off the edge of a small planet and you land on its side,
  walk up to a wall the field leans toward and you stand on it; a hop no longer turns you, and the turn no
  longer flickers on stepped surfaces
- Air control (WASD along the look, space and shift along "up") only in free flight; normal jumps are vanilla
- Fixes on ships: no more falling through a planet when "down" turns, no sliding on the side of a ship, no
  runaway speed, the camera and the light on the model are right on walls, the crosshair aims from the head
- Overlapping fields blend as vectors: two pulls cancel smoothly on the line between cores, an attracting core
  next to a repelling one no longer throws the body sideways, three cores do not jump
- Goggles: faces you can stand on are marked on Sable ships too; the seam between two fields is drawn where the
  fields really split; client settings for the Boundaries layer (`hillsphere-shell.toml`: presets, colours,
  density, spin, pulse, wave, flicker, fade-in)
- Rain and snow lean along the field (client option `rainEnabled` in `hillsphere-particles.toml`, `rain`
  sets how far); particles fall along the field too (`enabled`, `strength`, `range`)
- Own creative tab; the core settings screen is no longer blurred; the goggle overlay is drawn after clouds
- Camera follows the turn along an arc, ship levitation scales with the field radius

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
