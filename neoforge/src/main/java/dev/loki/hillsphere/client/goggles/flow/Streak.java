/*
 * Copyright (C) 2026 loki
 *
 * This file is part of Hill Sphere.
 *
 * Hill Sphere is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hill Sphere is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hill Sphere. If not, see <https://www.gnu.org/licenses/>.
 */
package dev.loki.hillsphere.client.goggles.flow;

import dev.loki.hillsphere.client.goggles.Palette;
import dev.loki.hillsphere.client.goggles.render.Brush;

import net.minecraft.world.phys.Vec3;

/**
 * One moving mark of the flow layer, alive for {@code period} ticks from {@code born}. A pull streak runs
 * along a streamline ({@code path}, points {@link #STEP} apart) with a fading tail and a chevron at its head;
 * a levitation mote drifts up the path, wobbling a little. It fades in and out over its life.
 */
record Streak(Vec3[] path, double born, double period, int[] ramp, double alpha, boolean drift) {

    /** Distance between two points of the path. */
    static final double STEP = 0.35;
    private static final double TAIL = 0.45;
    private static final double BARB = 0.16;
    private static final double MOTE = 0.05;
    private static final double WOBBLE = 0.07;

    void render(Brush brush, double time) {

        final double life = (time - born) / period;
        if (life < 0 || life >= 1) {
            return;
        }
        final double fade = alpha * Math.sin(Math.PI * life);
        final double length = STEP * (path.length - 1);
        final double head = life * length;
        if (drift) {
            mote(brush, head, life, fade);
        } else {
            streak(brush, head, length, fade);
        }
    }

    private void streak(Brush brush, double head, double length, double fade) {

        final double tail = Math.max(0, head - TAIL * length);
        final int steps = 4;
        Vec3 from = at(tail);
        for (int i = 1; i <= steps; i++) {
            final Vec3 to = at(tail + (head - tail) * i / steps);
            brush.line(from, to, colour(fade * (i - 1) / steps), colour(fade * i / steps));
            from = to;
        }
        final Vec3 tip = at(head);
        final Vec3 back = at(Math.max(0, head - STEP)).subtract(tip);
        if (back.lengthSqr() < 1e-8) {
            return;
        }
        final Vec3 dir = back.normalize();
        final Vec3 side = dir.cross(brush.camera().subtract(tip)).normalize().scale(BARB * 0.6);
        final int tipColour = Palette.argb(ramp[Palette.LIGHT], fade);
        brush.line(tip, tip.add(dir.scale(BARB)).add(side), tipColour, colour(fade * 0.6));
        brush.line(tip, tip.add(dir.scale(BARB)).subtract(side), tipColour, colour(fade * 0.6));
    }

    private void mote(Brush brush, double head, double life, double fade) {

        final Vec3 up = path[path.length - 1].subtract(path[0]).normalize();
        final Vec3 side = up.cross(brush.camera().subtract(path[0])).normalize();
        final Vec3 p = at(head).add(side.scale(WOBBLE * Math.sin(life * Math.PI * 4 + born)));
        brush.line(p.subtract(up.scale(STEP)), p, colour(0), colour(fade * 0.7));
        final int light = Palette.argb(ramp[Palette.LIGHT], fade);
        final Vec3 u = up.scale(MOTE);
        final Vec3 s = side.scale(MOTE);
        brush.line(p.add(u), p.add(s), light, light);
        brush.line(p.add(s), p.subtract(u), light, light);
        brush.line(p.subtract(u), p.subtract(s), light, light);
        brush.line(p.subtract(s), p.add(u), light, light);
    }

    /** The point at a distance along the path. */
    private Vec3 at(double distance) {

        final double index = Math.max(0, Math.min(path.length - 1.0, distance / STEP));
        final int i = Math.min(path.length - 2, (int) index);
        return path[i].lerp(path[i + 1], index - i);
    }

    private int colour(double a) {

        return Palette.argb(ramp[Palette.MAIN], a);
    }
}
