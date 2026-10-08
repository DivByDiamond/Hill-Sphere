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

import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.Gravity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;

/** Decides when a living thing's "down" follows the pull, and turns its body without pushing it into blocks. */
public final class FrameSwitch {

    /** In the air a frame is kept while the pull is within 60 degrees of its "down". */
    private static final double KEEP_IN_AIR = 0.5;

    /** On the ground it is kept until the pull points well above the horizon, so a platform stays a floor beside the core. */
    private static final double KEEP_ON_GROUND = -0.4;

    /** Ticks a new direction must hold before the frame follows it. */
    private static final int DWELL_TICKS = 4;

    private GravityFrame candidate = GravityFrame.DOWN;
    private int dwell;

    /** The frame the entity should be in now, given the pull; {@code current} while a new direction has not held long enough. */
    public GravityFrame next(Entity self, GravityFrame current, Gravity planet) {

        if (Gravity.VANILLA.equals(planet)) {
            return GravityFrame.DOWN;
        }
        final Vec3d pull = planet.direction();
        final GravityFrame wanted = GravityFrame.of(pull);
        if (wanted == current || pull.dot(current.toReal(Vec3d.DOWN)) > (self.onGround() ? KEEP_ON_GROUND : KEEP_IN_AIR)) {
            dwell = 0;
            return current;
        }
        dwell = wanted == candidate ? dwell + 1 : 1;
        candidate = wanted;
        return dwell >= DWELL_TICKS ? wanted : current;
    }

    /** Turns the body around its middle, and only if the turned body has room; false means wait and try again. */
    public boolean turn(Entity self, GravityFrame from, GravityFrame to) {

        final Vec3 center = self.getBoundingBox().getCenter();
        final EntityDimensions size = self.getDimensions(self.getPose());
        final Vec3d off = to.toReal(new Vec3d(0, -size.height() / 2, 0));
        final Vec3 feet = new Vec3(center.x + off.x(), center.y + off.y(), center.z + off.z());
        if (!self.level().noCollision(self, LivingGravity.box(to, feet, size))) {
            return false;
        }
        dropSpeedAlong(self, from.toReal(Vec3d.DOWN));
        dwell = 0;
        self.setPos(feet);
        return true;
    }

    /** What was fallen so far must not carry on as sideways flight once "down" has turned. */
    private static void dropSpeedAlong(Entity self, Vec3d down) {

        final Vec3 v = self.getDeltaMovement();
        final double along = v.x * down.x() + v.y * down.y() + v.z * down.z();
        self.setDeltaMovement(v.x - along * down.x(), v.y - along * down.y(), v.z - along * down.z());
    }
}
