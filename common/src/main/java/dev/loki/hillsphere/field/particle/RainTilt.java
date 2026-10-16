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
 * How far the rain leans. A drop falls along the field's "down", so a strip of rain, which vanilla draws as a
 * vertical quad, is sheared: a point {@code dy} higher lies {@code slope * dy} to the side. The lean is soft: the
 * direction of the fall is a blend of the world's down and the field's, and a fall that is nearly sideways is
 * kept from shearing the strips without end.
 */
public final class RainTilt {

    /** The steepest a strip may lean: how flat the fall may get before the lean stops growing, as a share of its speed. */
    static final double MIN_DOWN = 0.4;

    private RainTilt() {
    }

    /**
     * The sideways lean per block of height, as (x, z), for the field and the share {@code mix} (0 vertical rain, 1 along
     * the field). A push-away field makes drops fall away from its core.
     */
    public static Vec3d slope(Gravity gravity, double mix) {

        final double sign = gravity.strength() < 0 ? -1 : 1;
        final Vec3d fall = gravity.direction().scale(sign).scale(mix).add(Vec3d.DOWN.scale(1 - mix));
        final double length = fall.length();
        if (length < 1e-9) {
            return Vec3d.ZERO;
        }
        final Vec3d along = fall.scale(1 / length);
        final double vertical = along.y() <= 0 ? Math.min(along.y(), -MIN_DOWN) : Math.max(along.y(), MIN_DOWN);
        return new Vec3d(along.x() / vertical, 0, along.z() / vertical);
    }
}
