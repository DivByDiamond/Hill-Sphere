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

import dev.loki.hillsphere.compat.ShipAccess;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Sable ships: a core on a ship has its field where the ship is in the world, and the fields push
 * the ships themselves through {@link ShipPhysics}.
 */
public final class SableShips implements ShipAccess {

    private SableShips() {
    }

    public static ShipAccess install() {

        NeoForge.EVENT_BUS.addListener(ForgeSablePrePhysicsTickEvent.class, ShipPhysics::onPhysicsTick);
        return new SableShips();
    }

    @Override
    public Vec3d toWorld(Level level, Vec3d point) {

        final Vec3 world = Sable.HELPER.projectOutOfSubLevel(level, new Vec3(point.x(), point.y(), point.z()));
        return new Vec3d(world.x, world.y, world.z);
    }

    @Override
    public String describe(Entity entity) {

        final var tracking = Sable.HELPER.getTrackingSubLevel(entity);
        final var containing = Sable.HELPER.getContaining(entity);
        if (tracking == null && containing == null) {
            return "ship:none";
        }
        final var ship = tracking != null ? tracking : containing;
        final var pose = ship.logicalPose();
        final var speed = Sable.HELPER.getVelocity(entity.level(), new org.joml.Vector3d(entity.getX(), entity.getY(), entity.getZ()),
                new org.joml.Vector3d());
        return String.format(java.util.Locale.ROOT, "ship[tracking=%s inside=%s at=(%.2f,%.2f,%.2f) up=%s speed=(%.2f,%.2f,%.2f)]",
                tracking != null, containing != null, pose.position().x(), pose.position().y(), pose.position().z(),
                pose.transformNormal(new org.joml.Vector3d(0, 1, 0)).y > 0 ? "world-up" : "world-down", speed.x, speed.y, speed.z);
    }

    /** Sable turns the camera of whoever rides a seat on a ship, the same way it turns the ship. */
    @Override
    public boolean turnsCamera(Entity entity) {

        final Entity vehicle = entity.getVehicle();
        return vehicle != null && Sable.HELPER.getContaining(vehicle) != null;
    }
}
