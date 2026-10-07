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
package dev.loki.hillsphere.world;

import dev.loki.hillsphere.config.DimensionScale;
import dev.loki.hillsphere.field.GravityField;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.Gravity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Answers "what do the cores do to this entity" on either side: the server asks its cores, the client its mirror. */
public final class FieldLookup {

    private FieldLookup() {
    }

    /** How much of gravity levitation cores cancel where the entity is: 0 none, 1 all of it. */
    public static double liftAt(Entity entity) {

        final GravityField<BlockPos> field = fieldOf(entity.level());
        if (field == null) {
            return 0;
        }
        final Vec3 c = entity.getBoundingBox().getCenter();
        return field.liftAt(new Vec3d(c.x, c.y, c.z));
    }

    /** Where "down" points for the entity because of attracting and repelling cores; vanilla if there are none. */
    public static Gravity planetAt(Entity entity) {

        final GravityField<BlockPos> field = fieldOf(entity.level());
        if (field == null) {
            return Gravity.VANILLA;
        }
        final Vec3 c = entity.getBoundingBox().getCenter();
        final Gravity planet = field.planetAt(new Vec3d(c.x, c.y, c.z));
        return Gravity.VANILLA.equals(planet) ? planet : new Gravity(planet.direction(), planet.strength() * DimensionScale.of(entity.level()));
    }

    private static GravityField<BlockPos> fieldOf(Level level) {

        if (level.isClientSide) {
            return ClientFields.field();
        }
        final LevelFields fields = WorldFields.peek(level);
        return fields == null ? null : fields.field();
    }
}
