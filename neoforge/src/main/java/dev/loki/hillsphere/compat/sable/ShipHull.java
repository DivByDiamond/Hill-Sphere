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
package dev.loki.hillsphere.compat.sable;

import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.shape.Deck;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/** The hull of a ship as a deck: the box of all its blocks with some room around, turned and placed with the ship. */
final class ShipHull {

    /** Room around the blocks, in blocks: a player standing on the top deck or hanging off the side is still inside. */
    static final double MARGIN = 2;

    private ShipHull() {
    }

    /** The deck of the ship that holds this block, or null when the block is not on a ship. */
    static Deck of(Level level, BlockPos pos) {

        if (!(Sable.HELPER.getContaining(level, pos) instanceof SubLevel ship)) {
            return null;
        }
        final BoundingBox3ic b = ship.getPlot().getBoundingBox();
        if (b == null || b == BoundingBox3i.EMPTY) {
            return null;
        }
        final Pose3dc pose = ship.logicalPose();
        final Vector3d middle = new Vector3d(b.minX() + b.maxX() + 1, b.minY() + b.maxY() + 1, b.minZ() + b.maxZ() + 1).mul(0.5);
        final Vec3d half = new Vec3d((b.maxX() + 1 - b.minX()) / 2.0 + MARGIN, (b.maxY() + 1 - b.minY()) / 2.0 + MARGIN,
                (b.maxZ() + 1 - b.minZ()) / 2.0 + MARGIN);
        return new Deck(vec(pose.transformPosition(middle)), axis(pose, 1, 0, 0), axis(pose, 0, 1, 0), axis(pose, 0, 0, 1), half);
    }

    private static Vec3d axis(Pose3dc pose, double x, double y, double z) {

        return vec(pose.orientation().transform(new Vector3d(x, y, z)));
    }

    private static Vec3d vec(Vector3dc v) {

        return new Vec3d(v.x(), v.y(), v.z());
    }
}
