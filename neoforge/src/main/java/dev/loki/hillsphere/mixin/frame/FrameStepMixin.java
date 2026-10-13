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
package dev.loki.hillsphere.mixin.frame;

import java.util.List;

import dev.loki.hillsphere.debug.Debug;
import dev.loki.hillsphere.entity.FrameState;
import dev.loki.hillsphere.entity.StepHeights;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.field.math.Vec3d;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Collision with a step along the frame's "up": vanilla's own collide, with every y replaced by the
 * frame's vertical axis, so a pig can climb a slab nailed to a ceiling just as it climbs one on the floor.
 */
@Mixin(Entity.class)
public abstract class FrameStepMixin {

    @Shadow
    private static List<VoxelShape> collectColliders(Entity entity, Level level, List<VoxelShape> list, AABB box) {

        throw new AssertionError();
    }

    /** Movement, collisions and the step attempt, all measured along the frame's axes. */
    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void hillsphereFrameCollide(Vec3 move, CallbackInfoReturnable<Vec3> cir) {

        final GravityFrame frame = frame();
        if (frame == null || frame.isVanilla()) {
            return;
        }
        final Entity self = (Entity) (Object) this;
        final Vec3 up = hillsphereVec(frame.toReal(new Vec3d(0, 1, 0)));
        final AABB box = self.getBoundingBox();
        final AABB swept = box.expandTowards(move);
        final List<VoxelShape> pushed = self.level().getEntityCollisions(self, swept);
        final List<VoxelShape> around = collectColliders(self, self.level(), pushed, swept);
        final Vec3 resolved = move.lengthSqr() == 0.0 ? move : hillsphereClip(move, box, around, frame);

        final double asked = move.dot(up);
        final double done = resolved.dot(up);
        final Vec3 flatMove = move.subtract(up.scale(asked));
        final Vec3 flatDone = resolved.subtract(up.scale(done));
        final boolean flatBlocked = flatMove.x != flatDone.x || flatMove.y != flatDone.y || flatMove.z != flatDone.z;
        final boolean sinking = asked != done && asked < 0.0;
        if (self.maxUpStep() > 0.0F && (sinking || self.onGround()) && flatBlocked) {
            final AABB risen = sinking ? box.move(up.scale(done)) : box;
            AABB probe = risen.expandTowards(flatMove.add(up.scale(self.maxUpStep())));
            if (!sinking) {
                probe = probe.expandTowards(up.scale(-1.0E-5));
            }
            final List<VoxelShape> nearby = collectColliders(self, self.level(), pushed, probe);
            final Direction.Axis axis = hillsphereAxis(up);
            final double sign = hillsphereComponent(up, axis);
            for (float offer : StepHeights.of(risen, nearby, self.maxUpStep(), (float) done, axis, sign)) {
                final Vec3 tried = hillsphereClip(flatMove.add(up.scale(offer)), risen, nearby, frame);
                if (hillsphereAside(tried, up).lengthSqr() > hillsphereAside(resolved, up).lengthSqr()) {
                    Debug.every("step" + self.getId() + self.level().isClientSide, self.level().getGameTime(), 2,
                            "{} STEP {} offer={} maxUpStep={} asked={} resolved={} flatMove={} flatDone={} sinking={} ground={} frame={}",
                            self.level().isClientSide ? "C" : "S", self.getName().getString(), offer, self.maxUpStep(), move, resolved,
                            flatMove, flatDone, sinking, self.onGround(), frame);
                    cir.setReturnValue(tried.subtract(up.scale(done)));
                    return;
                }
            }
        }
        cir.setReturnValue(resolved);
    }

    /**
     * Clipping against the shapes: the frame's "up" axis first, then the flat axes in vanilla's order of
     * dominance, each step handed to vanilla's own per-axis collision along the matching world axis.
     */
    @Unique
    private static Vec3 hillsphereClip(Vec3 move, AABB box, List<VoxelShape> shapes, GravityFrame frame) {

        if (shapes.isEmpty()) {
            return move;
        }
        final Vec3 ex = hillsphereVec(frame.axes()[0]);
        final Vec3 ey = hillsphereVec(frame.axes()[1]);
        final Vec3 ez = hillsphereVec(frame.axes()[2]);

        AABB swept = box;
        double y = move.dot(ey);
        if (y != 0.0) {
            y = hillsphereAxisClip(ey, y, swept, shapes);
            if (y != 0.0) {
                swept = swept.move(ey.scale(y));
            }
        }

        double x = move.dot(ex);
        double z = move.dot(ez);
        final boolean zFirst = Math.abs(x) < Math.abs(z);
        if (zFirst && z != 0.0) {
            z = hillsphereAxisClip(ez, z, swept, shapes);
            if (z != 0.0) {
                swept = swept.move(ez.scale(z));
            }
        }
        if (x != 0.0) {
            x = hillsphereAxisClip(ex, x, swept, shapes);
            if (!zFirst && x != 0.0) {
                swept = swept.move(ex.scale(x));
            }
        }
        if (!zFirst && z != 0.0) {
            z = hillsphereAxisClip(ez, z, swept, shapes);
        }
        return ey.scale(y).add(ex.scale(x)).add(ez.scale(z));
    }

    /** Clips a component along a frame axis by the shapes, turning it into a component along the world axis. */
    @Unique
    private static double hillsphereAxisClip(Vec3 axis, double along, AABB box, List<VoxelShape> shapes) {

        final Direction.Axis world = hillsphereAxis(axis);
        final double sign = hillsphereComponent(axis, world);
        return Shapes.collide(world, box, shapes, along * sign) * sign;
    }

    /** What is left of a movement once its part along "up" is taken out: its sideways distance squared. */
    @Unique
    private static Vec3 hillsphereAside(Vec3 v, Vec3 up) {

        return v.subtract(up.scale(v.dot(up)));
    }

    @Unique
    private static Direction.Axis hillsphereAxis(Vec3 axis) {

        if (axis.x != 0.0) {
            return Direction.Axis.X;
        }
        return axis.y != 0.0 ? Direction.Axis.Y : Direction.Axis.Z;
    }

    /** The direction a frame axis points along its world axis: +1 or -1. */
    @Unique
    private static double hillsphereComponent(Vec3 axis, Direction.Axis world) {

        return switch (world) {
            case X -> axis.x;
            case Y -> axis.y;
            case Z -> axis.z;
        };
    }

    @Unique
    private static Vec3 hillsphereVec(Vec3d v) {

        return new Vec3(v.x(), v.y(), v.z());
    }

    @Unique
    private GravityFrame frame() {

        return ((FrameState) this).hillsphereFrame();
    }
}
