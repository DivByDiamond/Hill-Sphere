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

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Decides when a living thing's "down" follows the pull, and turns its body without pushing it into blocks. */
public final class FrameSwitch {

    /** In the air a frame is kept while the pull is within 60 degrees of its "down". */
    private static final double KEEP_IN_AIR = 0.5;

    /** On the ground it is kept until the pull points well above the horizon, so a platform stays a floor beside the core. */
    private static final double KEEP_ON_GROUND = -0.4;

    /** Ticks a new direction must hold before the frame follows it. */
    private static final int DWELL_TICKS = 4;

    /**
     * How much better in the pull a new frame must be than the sitting candidate to take its place,
     * in dots. Without it a pull that wobbles across a 45-degree boundary flips the candidate every
     * tick, the dwell never finishes, and near a small core - where one block of walking swings the
     * pull hard - the player is dragged about in a frame that is never switched.
     */
    private static final double CANDIDATE_MARGIN = 0.15;

    /** How far the body is lifted along the new "up" while looking for room: an eighth of a block at a time. */
    private static final double LIFT = 0.125;

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
        if (wanted == candidate) {
            dwell++;
        } else if (pull.dot(wanted.toReal(Vec3d.DOWN)) > pull.dot(candidate.toReal(Vec3d.DOWN)) + CANDIDATE_MARGIN) {
            candidate = wanted;
            dwell = 1;
        }
        return dwell >= DWELL_TICKS ? candidate : current;
    }

    /** Turns the body around its middle, and only if the turned body has room; false means wait and try again. */
    public boolean turn(Entity self, GravityFrame from, GravityFrame to) {

        final Vec3 center = self.getBoundingBox().getCenter();
        final EntityDimensions size = self.getDimensions(self.getPose());
        final Vec3d off = to.toReal(new Vec3d(0, -size.height() / 2, 0));
        final Vec3 start = new Vec3(center.x + off.x(), center.y + off.y(), center.z + off.z());
        final Vec3 feet = fitting(self, to, size, start);
        if (feet == null) {
            return false;
        }
        dropSpeedAlong(self, from.toReal(Vec3d.DOWN));
        dwell = 0;
        self.setPos(feet);
        turnLook(self, from, to);
        return true;
    }

    /**
     * Turns the look with the body: the old look direction is
     * carried from the old frame into the new one, and the yaw and pitch are rewritten so the
     * eyes keep the same world point. Without it a frame switch swings the view to a new world
     * direction and the player is flung sideways; with it the camera eases while the gaze holds.
     */
    private static void turnLook(Entity self, GravityFrame from, GravityFrame to) {

        final float yRot = self.getYRot();
        final float xRot = self.getXRot();
        final Vec3d fromDown = from.toReal(Vec3d.DOWN);
        final Vec3d toDown = to.toReal(Vec3d.DOWN);
        final Vec3d oldLook = from.toReal(rotToVec(yRot, xRot));
        final Vec3d newLook;
        if (fromDown.dot(toDown) < -0.9995) {
            newLook = oldLook.scale(-1);
        } else {
            final Quaternionf between = new Quaternionf().rotationTo(
                    new Vector3f((float) fromDown.x(), (float) fromDown.y(), (float) fromDown.z()),
                    new Vector3f((float) toDown.x(), (float) toDown.y(), (float) toDown.z()));
            final Vector3f turned = between.transform(new Vector3f((float) oldLook.x(), (float) oldLook.y(), (float) oldLook.z()));
            newLook = new Vec3d(turned.x, turned.y, turned.z);
        }
        final Vec3d virtual = to.toVirtual(newLook);
        final float newYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-virtual.x(), virtual.z())));
        final float newPitch = (float) Math.toDegrees(Math.asin(Mth.clamp(-virtual.y(), -1.0, 1.0)));
        final float deltaYaw = newYaw - yRot;
        final float deltaPitch = newPitch - xRot;
        self.setYRot(Mth.wrapDegrees(newYaw));
        self.setXRot(newPitch);
        self.yRotO += deltaYaw;
        self.xRotO += deltaPitch;
        if (self instanceof LivingEntity living) {
            living.yBodyRot = Mth.wrapDegrees(living.yBodyRot + deltaYaw);
            living.yBodyRotO += deltaYaw;
            living.yHeadRot = Mth.wrapDegrees(living.yHeadRot + deltaYaw);
            living.yHeadRotO += deltaYaw;
        }
    }

    /** The vanilla look direction for a yaw and pitch, in whatever frame the angles live in. */
    private static Vec3d rotToVec(float yRot, float xRot) {

        final double pitch = Math.toRadians(xRot);
        final double yaw = Math.toRadians(-yRot);
        return new Vec3d(Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
    }

    /**
     * The turned body's place to stand: where it already is when that is room, otherwise lifted along the
     * new "up" until it clears an inner corner, a whole body height at most.
     */
    private static Vec3 fitting(Entity self, GravityFrame to, EntityDimensions size, Vec3 start) {

        if (self.level().noCollision(self, LivingGravity.box(to, start, size))) {
            return start;
        }
        final Vec3d up = to.toReal(new Vec3d(0, 1, 0));
        for (double lifted = LIFT; lifted <= size.height() + LIFT; lifted += LIFT) {
            final Vec3 there = new Vec3(start.x + up.x() * lifted, start.y + up.y() * lifted, start.z + up.z() * lifted);
            if (self.level().noCollision(self, LivingGravity.box(to, there, size))) {
                return there;
            }
        }
        return null;
    }

    /** What was fallen so far must not carry on as sideways flight once "down" has turned. */
    private static void dropSpeedAlong(Entity self, Vec3d down) {

        final Vec3 v = self.getDeltaMovement();
        final double along = v.x * down.x() + v.y * down.y() + v.z * down.z();
        self.setDeltaMovement(v.x - along * down.x(), v.y - along * down.y(), v.z - along * down.z());
    }
}
