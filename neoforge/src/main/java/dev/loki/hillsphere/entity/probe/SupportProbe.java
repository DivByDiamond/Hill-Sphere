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
package dev.loki.hillsphere.entity.probe;

import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.field.math.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Looks for something to stand on under an entity, as if its body were turned into a frame around its middle. */
public final class SupportProbe {

    /** Steps of the search, in blocks. */
    private static final double STEP = 0.25;

    /** The box is shrunk this much so a wall it only touches at the side is not taken for a floor. */
    private static final double SKIN = 0.01;

    private SupportProbe() {
    }

    /** Where the feet are once the body is turned into the frame around its middle; for the frame it is in, where they are. */
    public static Vec3 turnedFeet(Entity self, GravityFrame to) {

        final Vec3 center = self.getBoundingBox().getCenter();
        final EntityDimensions size = self.getDimensions(self.getPose());
        final Vec3d off = to.toReal(new Vec3d(0, -size.height() / 2, 0));
        return new Vec3(center.x + off.x(), center.y + off.y(), center.z + off.z());
    }

    /**
     * How far under the feet of the body turned into the frame the nearest block is, in blocks, searched in
     * quarter-block steps; {@link Double#POSITIVE_INFINITY} when there is nothing within {@code reach}.
     */
    public static double floorBelow(Entity self, GravityFrame frame, double reach) {

        final AABB box = LivingGravity.box(frame, turnedFeet(self, frame), self.getDimensions(self.getPose())).deflate(SKIN);
        final Vec3d down = frame.toReal(Vec3d.DOWN);
        for (double d = 0; d <= reach; d += STEP) {
            if (!self.level().noCollision(self, box.move(down.x() * d, down.y() * d, down.z() * d))) {
                return d;
            }
        }
        return Double.POSITIVE_INFINITY;
    }
}
