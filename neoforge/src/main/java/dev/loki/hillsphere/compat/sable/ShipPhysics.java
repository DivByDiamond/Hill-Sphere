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

import dev.loki.hillsphere.config.DimensionScale;
import dev.loki.hillsphere.config.HillSphereConfig;
import dev.loki.hillsphere.field.body.Body;
import dev.loki.hillsphere.field.body.BodyForces;
import dev.loki.hillsphere.field.body.Mounted;
import dev.loki.hillsphere.field.body.Push;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.world.LevelFields;
import dev.loki.hillsphere.world.WorldFields;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Every physics step, turns the fields of the cores into forces on Sable ships: levitation lifts,
 * attracting and repelling cores pull, and a core on a ship gets back what it pulls with.
 * Levitation goes into Sable's levitation group, pulls into its magnetic force group.
 */
final class ShipPhysics {

    private static final ResourceLocation LIFT = ResourceLocation.fromNamespaceAndPath("sable", "levitation");
    private static final ResourceLocation PULL = ResourceLocation.fromNamespaceAndPath("sable", "magnetic_force");

    private ShipPhysics() {
    }

    static void onPhysicsTick(ForgeSablePrePhysicsTickEvent event) {

        final ServerLevel level = event.getPhysicsSystem().getLevel();
        final LevelFields fields = WorldFields.peek(level);
        final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (!HillSphereConfig.shipForces() || fields == null || fields.field().size() == 0 || container == null) {
            return;
        }
        final List<Mounted<ServerSubLevel>> cores = new ArrayList<>();
        fields.field().forEach((pos, core) -> cores.add(
                new Mounted<>(core, Sable.HELPER.getContaining(level, pos) instanceof ServerSubLevel ship ? ship : null)));
        final List<Body<ServerSubLevel>> bodies = new ArrayList<>();
        for (final ServerSubLevel ship : container.getAllSubLevels()) {
            final MassData mass = ship.getMassTracker();
            if (!ship.isRemoved() && !mass.isInvalid()) {
                final Vector3d center = ship.logicalPose().transformPosition(new Vector3d(mass.getCenterOfMass()));
                bodies.add(new Body<>(ship, vec(center), mass.getMass(), vec(DimensionPhysicsData.getGravity(level, center))));
            }
        }
        final BodyForces forces = new BodyForces(HillSphereConfig.tuning());
        final List<Push<ServerSubLevel>> pushes = forces.of(bodies, cores, DimensionScale.of(level));
        for (final Push<ServerSubLevel> push : pushes) {
            apply(push, event.getTimeStep());
        }
    }

    /** Sable takes an impulse in the ship's own frame, at a point of its blocks. */
    private static void apply(Push<ServerSubLevel> push, double timeStep) {

        final ServerSubLevel ship = push.body();
        final Pose3dc pose = ship.logicalPose();
        final Vector3d point = pose.transformPositionInverse(joml(push.point()));
        final Vector3d impulse = pose.transformNormalInverse(joml(push.force())).mul(timeStep);
        ship.getOrCreateQueuedForceGroup(group(push.kind())).applyAndRecordPointForce(point, impulse);
    }

    private static ForceGroup group(Push.Kind kind) {

        return ForceGroups.REGISTRY.get(kind == Push.Kind.LIFT ? LIFT : PULL);
    }

    private static Vec3d vec(Vector3dc v) {

        return new Vec3d(v.x(), v.y(), v.z());
    }

    private static Vector3d joml(Vec3d v) {

        return new Vector3d(v.x(), v.y(), v.z());
    }
}
