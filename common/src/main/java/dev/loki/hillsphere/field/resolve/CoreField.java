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

import dev.loki.hillsphere.field.FieldTuning;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.math.Vec3d;

/**
 * Current state of one core as the resolver sees it.
 *
 * @param center core position
 * @param radius current radius in blocks (0 means the field is off)
 * @param strength current pull in multiples of vanilla gravity; for {@link Polarity#LEVITATE}
 *     the share of gravity the core cancels (1 cancels it fully)
 * @param polarity what the core does
 */
public record CoreField(Vec3d center, double radius, double strength, Polarity polarity) {

    /** Pull direction at a point (unit vector), or {@link Vec3d#ZERO} at the exact center. */
    Vec3d direction(Vec3d point) {

        final Vec3d toCore = center.sub(point).normalized();
        return polarity == Polarity.REPEL ? toCore.scale(-1) : toCore;
    }

    /** Falloff factor in the point, 0 when it is outside the field. */
    double fade(FieldTuning tuning, Vec3d point) {

        if (radius <= 0) {
            return 0;
        }
        return tuning.fade(center.sub(point).length() / radius);
    }
}
