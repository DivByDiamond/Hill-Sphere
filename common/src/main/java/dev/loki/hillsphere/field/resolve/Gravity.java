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
package dev.loki.hillsphere.field.resolve;

import dev.loki.hillsphere.field.math.Vec3d;

/**
 * Gravity at a point.
 *
 * @param direction unit vector of "down"
 * @param strength multiples of vanilla gravity
 */
public record Gravity(Vec3d direction, double strength) {

    public static final Gravity VANILLA = new Gravity(Vec3d.DOWN, 1);

    /**
     * Gravity weakened by levitation. Zero lift changes nothing, a lift of 1 cancels gravity,
     * more than 1 makes the strength negative, which means a push against "down".
     */
    public Gravity withLift(double lift) {

        return lift == 0 ? this : new Gravity(direction, strength * (1 - lift));
    }
}
