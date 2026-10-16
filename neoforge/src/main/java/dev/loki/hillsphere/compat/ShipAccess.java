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
package dev.loki.hillsphere.compat;

import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.shape.Deck;
import java.util.List;
import net.minecraft.core.BlockPos;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** What the mod asks of a ship mod. The defaults are a world without ships. */
public interface ShipAccess {

    ShipAccess NONE = new ShipAccess() {
    };

    /** Where a point of a ship's blocks is in the world now; points outside any ship stay as they are. */
    default Vec3d toWorld(Level level, Vec3d point) {

        return point;
    }

    /** A short text about the ship under an entity, for the debug log. */
    default String describe(Entity entity) {

        return "ships:none";
    }

    /** True while the ship mod turns this entity's camera with the ship it rides. */
    default boolean turnsCamera(Entity entity) {

        return false;
    }

    /** True when a block of a ship overlaps the box; the world's own blocks are not asked. */
    default boolean collides(Level level, AABB box) {

        return false;
    }

    /** The ships whose blocks may lie within {@code range} of the point; none without a ship mod. */
    default List<ShipPlot> shipsNear(Level level, Vec3d point, double range) {

        return List.of();
    }

    /** The deck a deck-shaped core on a ship fills: the ship's hull; null when the block is not on a ship. */
    default Deck hullOf(Level level, BlockPos pos) {

        return null;
    }
}
