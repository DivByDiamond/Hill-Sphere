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
package dev.loki.hillsphere.field.particle;

import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.Gravity;

/**
 * A soft nudge of a particle's motion toward the field, not physics: the field's "down" replaces the
 * world's down only in part, and less and less toward the edge of the range around the player.
 */
public final class ParticlePull {

    /** What a vanilla particle's gravity takes from its vertical speed each tick, per unit of gravity. */
    public static final double TICK_PULL = 0.04;

    private static final double FULL_UNTIL = 0.7;
    private static final double MAX_STRENGTH = 2;
    private static final Vec3d WORLD_UP = new Vec3d(0, 1, 0);

    private ParticlePull() {
    }

    /** Share of the effect left at the distance: all of it up to 70% of the range, then fading to nothing. */
    public static double fade(double distance, double range) {

        if (range <= 0 || distance >= range) {
            return 0;
        }
        final double edge = range * FULL_UNTIL;
        return distance <= edge ? 1 : (range - distance) / (range - edge);
    }

    /** True when the gravity is vanilla's, so a particle there is left alone. */
    public static boolean isVanilla(Gravity gravity) {

        return gravity.strength() == 1 && gravity.direction().sub(Vec3d.DOWN).length() < 1e-4;
    }

    /**
     * The change to add to a falling particle's speed: vanilla pulls it along -y by {@code 0.04 * gravity}
     * and this turns that pull toward the field's "down", scaled by its strength, by the share {@code mix}.
     */
    public static Vec3d fall(Gravity gravity, double particleGravity, double mix) {

        final double s = Math.max(-MAX_STRENGTH, Math.min(MAX_STRENGTH, gravity.strength()));
        return gravity.direction().scale(s).sub(Vec3d.DOWN).scale(TICK_PULL * particleGravity * mix);
    }

    /** Where a particle that rises goes: the field's "up", which is against "down" and flips with a push. */
    public static Vec3d up(Gravity gravity) {

        return gravity.direction().scale(gravity.strength() < 0 ? 1 : -1);
    }

    /**
     * The launch speed with its vertical part turned toward the field's "up" by the share {@code mix}: smoke
     * and flames that start with an upward speed climb against the field, a speed along -y falls along it.
     */
    public static Vec3d lean(Vec3d velocity, Gravity gravity, double mix) {

        return velocity.add(up(gravity).sub(WORLD_UP).scale(velocity.y() * mix));
    }
}
