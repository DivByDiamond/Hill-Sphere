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

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** What the cores do to the gravity of players and mobs: see {@link GravityFrame}. */
public final class LivingGravity {

    private LivingGravity() {
    }

    /** Where "down" is for the entity: vanilla for everything that is not alive. */
    public static GravityFrame frameOf(Entity entity) {

        return entity instanceof LivingEntity && entity instanceof FrameState state ? state.hillsphereFrame() : GravityFrame.DOWN;
    }

    /** Multiplier for the entity's gravity: 1 in vanilla and for things that are not alive. */
    public static double factor(Entity entity) {

        return entity instanceof LivingEntity && entity instanceof FrameState state ? state.hillsphereStrength() : 1;
    }

    /** The entity's box turned into the frame; its position is the middle of the face it stands on. */
    public static AABB box(GravityFrame frame, Vec3 feet, EntityDimensions size) {

        final double half = size.width() / 2;
        final Vec3d a = frame.toReal(new Vec3d(-half, 0, -half));
        final Vec3d b = frame.toReal(new Vec3d(half, size.height(), half));
        return new AABB(feet.x + Math.min(a.x(), b.x()), feet.y + Math.min(a.y(), b.y()), feet.z + Math.min(a.z(), b.z()),
                feet.x + Math.max(a.x(), b.x()), feet.y + Math.max(a.y(), b.y()), feet.z + Math.max(a.z(), b.z()));
    }
}
