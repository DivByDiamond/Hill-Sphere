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
package dev.loki.hillsphere.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.loki.hillsphere.client.camera.FrameEyeMath;
import dev.loki.hillsphere.compat.Ships;
import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.frame.GravityFrame;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Turns the camera with the frame the field has put the player in:
 * the eye is the vanilla one rotated about the feet by an eased quarter turn, and the rotation is
 * that same turn composed onto vanilla's. The easing runs at a constant quarter turn per half
 * second, so a frame switch under the camera is a sweep instead of a hard cut; when the frame
 * turns vanilla again the turn eases back to nothing rather than snapping. In first person the eye
 * is pulled back along the frame's "up" until it is in clear air, so a wall beside the head cannot
 * swallow the view. While a ship turns the camera of a seated rider, the ship's turn wins and this
 * one eases back to nothing, so the two never stack.
 */
@Mixin(value = Camera.class, priority = 1001)
public abstract class FrameCameraMixin {

    /** Ticks one quarter turn of easing takes. */
    @Unique
    private static final float HILLSPHERE_TURN_TICKS = 10.0f;

    /** The turn the camera is showing, eased frame by frame. */
    @Unique
    private static final Quaternionf HILLSPHERE_SHOWN = new Quaternionf();

    /** What the camera showed last frame, and in which frame, so a switch can start from the very same view. */
    @Unique
    private static final Quaternionf HILLSPHERE_LAST_VIEW = new Quaternionf();

    @Unique
    private static GravityFrame hillsphereLastFrame = GravityFrame.DOWN;

    @Unique
    private static boolean hillsphereActive;

    @Shadow
    @Final
    private Quaternionf rotation;

    @Shadow
    private Entity entity;

    @Shadow
    private float eyeHeight;

    @Shadow
    private float eyeHeightOld;

    /**
     * Eases the shown turn first, because {@code setup} rotates before it positions; the position
     * wrap then composes the very same value, and both halves of the camera match within a frame.
     */
    @Inject(
            method = "setRotation(FFF)V",
            at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;",
                    shift = At.Shift.AFTER),
            remap = false)
    private void hillsphereFrameRotation(float yRot, float xRot, float roll, CallbackInfo ci) {

        final GravityFrame frame = this.hillsphereFrame();
        if (frame != hillsphereLastFrame) {
            if (hillsphereActive) {
                HILLSPHERE_SHOWN.set(HILLSPHERE_LAST_VIEW).mul(this.rotation.invert(new Quaternionf()));
            }
            hillsphereLastFrame = frame;
        }
        if (this.hillsphereShouldShow(frame, true)) {
            this.rotation.premul(HILLSPHERE_SHOWN);
        }
        HILLSPHERE_LAST_VIEW.set(this.rotation);
    }

    /**
     * The eye of a turned body is along its "up", not straight above the feet in world Y. It sits
     * there at once, like the body; only the view eases, since an eye swept along the arc would
     * pass through the block the body stands on.
     */
    @WrapOperation(
            method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", ordinal = 0))
    private void hillsphereFrameEye(
            Camera camera, double x, double y, double z, Operation<Void> original, BlockGetter level, Entity focusedEntity,
            boolean detached, boolean thirdPersonReverse, float partialTick) {

        final GravityFrame frame = this.hillsphereFrame();
        if (frame.isVanilla() || !this.hillsphereShouldShow(frame, false)) {
            original.call(camera, x, y, z);
            return;
        }
        final float eyeH = Mth.lerp(partialTick, this.eyeHeightOld, this.eyeHeight);
        final Vec3 feet = new Vec3(x, y - eyeH, z);
        final Vector3f rotated = FrameEyeMath.turnOf(frame).transform(new Vector3f(0.0f, eyeH, 0.0f));
        final Vec3 desired = new Vec3(feet.x + rotated.x, feet.y + rotated.y, feet.z + rotated.z);
        final Vec3 eye = detached ? desired : FrameEyeMath.clearEye(level, this.entity, feet, desired);
        original.call(camera, eye.x, eye.y, eye.z);
    }

    /** The frame of the camera's own entity; vanilla before the first setup and while a ship turns the camera. */
    @Unique
    private GravityFrame hillsphereFrame() {

        if (this.entity == null || Ships.turnsCamera(this.entity)) {
            return GravityFrame.DOWN;
        }
        return LivingGravity.frameOf(this.entity);
    }

    /**
     * Whether the eased turn has anything to show: a turned frame always does, and a vanilla one
     * only while the turn eases back to nothing. The shown turn is eased on every call that wants
     * to show, at a constant quarter turn per {@link #HILLSPHERE_TURN_TICKS} ticks.
     */
    @Unique
    private boolean hillsphereShouldShow(GravityFrame frame, boolean ease) {

        if (Minecraft.getInstance().player == null) {
            HILLSPHERE_SHOWN.identity();
            hillsphereActive = false;
            return false;
        }
        final Quaternionf target = frame.isVanilla() ? new Quaternionf() : FrameEyeMath.turnOf(frame);
        if (!hillsphereActive) {
            HILLSPHERE_SHOWN.identity();
            hillsphereActive = true;
        } else if (ease) {
            final float angle = 2.0f * (float) Math.acos(Math.min(1.0f, Math.abs(HILLSPHERE_SHOWN.dot(target))));
            if (angle > 1.0e-4f) {
                final float ticks = Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
                final float advance = Math.min(angle, ticks * (float) (Math.PI / 2) / HILLSPHERE_TURN_TICKS);
                HILLSPHERE_SHOWN.slerp(target, advance / angle);
            }
        }
        if (!frame.isVanilla()) {
            return true;
        }
        return 2.0f * (float) Math.acos(Math.min(1.0f, Math.abs(HILLSPHERE_SHOWN.w))) > 1.0e-3f;
    }
}
