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
package dev.loki.hillsphere.entity;

import dev.loki.hillsphere.field.resolve.Gravity;
import dev.loki.hillsphere.world.FieldLookup;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * What the cores do to the gravity of players and mobs. Their movement code only knows "down" along y,
 * so for now only the vertical directions are honoured: a negative factor makes them fall upward.
 */
public final class LivingGravity {

    /** How vertical the pull must be (the y part of its direction) to count as straight up or down. */
    private static final double VERTICAL = 0.5;

    private LivingGravity() {
    }

    /** Multiplier for the entity's gravity: 1 in vanilla, below 0 when "down" is up, 1 for non-living things. */
    public static double factor(Entity entity) {

        if (!(entity instanceof LivingEntity)) {
            return 1;
        }
        final Gravity planet = FieldLookup.planetAt(entity);
        final double y = planet.direction().y();
        if (Gravity.VANILLA.equals(planet) || Math.abs(y) < VERTICAL) {
            return 1;
        }
        return Math.signum(y) * -planet.strength();
    }
}
