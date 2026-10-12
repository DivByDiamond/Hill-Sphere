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

import dev.loki.hillsphere.entity.FrameState;
import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.field.math.Vec3d;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The head of a turned body is along its "up": eyes for seeing and the thin plate that checks walls. */
@Mixin(Entity.class)
public abstract class FrameWallMixin {

    /** The eye above the feet, along the frame's "up" instead of +y. */
    @Inject(method = "getEyeY", at = @At("HEAD"), cancellable = true)
    private void hillsphereFrameEyeY(CallbackInfoReturnable<Double> cir) {

        final GravityFrame frame = frame();
        if (frame == null || frame.isVanilla()) {
            return;
        }
        final Entity self = (Entity) (Object) this;
        cir.setReturnValue(self.position().y + hillsphereUp(frame).y() * self.getEyeHeight());
    }

    /** The interpolated eye: the same head offset, at the interpolated position. */
    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void hillsphereFrameEye(float partialTick, CallbackInfoReturnable<Vec3> cir) {

        final GravityFrame frame = frame();
        if (frame == null || frame.isVanilla()) {
            return;
        }
        final Entity self = (Entity) (Object) this;
        final Vec3 feet = new Vec3(Mth.lerp(partialTick, self.xo, self.getX()), Mth.lerp(partialTick, self.yo, self.getY()),
                Mth.lerp(partialTick, self.zo, self.getZ()));
        cir.setReturnValue(LivingGravity.above(frame, feet, self.getEyeHeight()));
    }

    /**
     * The eye of this tick. Vanilla builds it from the feet's x and z and only the eye's y, so on a wall it sat on
     * the face under the feet: suffocation, reach, buckets and line of sight all measured from the wall.
     */
    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void hillsphereFrameEyeNow(CallbackInfoReturnable<Vec3> cir) {

        final GravityFrame frame = frame();
        if (frame == null || frame.isVanilla()) {
            return;
        }
        final Entity self = (Entity) (Object) this;
        cir.setReturnValue(LivingGravity.above(frame, self.position(), self.getEyeHeight()));
    }

    /** The look vector is built around world "up"; in a turned frame the same angles mean the frame's axes. */
    @Inject(method = "calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void hillsphereFrameLook(float xRot, float yRot, CallbackInfoReturnable<Vec3> cir) {

        final GravityFrame frame = frame();
        if (frame == null || frame.isVanilla()) {
            return;
        }
        final Vec3 look = cir.getReturnValue();
        final Vec3d real = frame.toReal(new Vec3d(look.x, look.y, look.z));
        cir.setReturnValue(new Vec3(real.x(), real.y(), real.z()));
    }

    /** The wall plate around the eye lies across the frame's axes, so it hugs a wall beside the head. */
    @Redirect(method = "isInWall", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/AABB;ofSize(Lnet/minecraft/world/phys/Vec3;DDD)Lnet/minecraft/world/phys/AABB;"))
    private AABB hillsphereFramePlate(Vec3 center, double x, double y, double z) {

        final GravityFrame frame = frame();
        if (frame == null || frame.isVanilla()) {
            return AABB.ofSize(center, x, y, z);
        }
        final Vec3d sx = frame.toReal(new Vec3d(x, 0, 0));
        final Vec3d sy = frame.toReal(new Vec3d(0, y, 0));
        final Vec3d sz = frame.toReal(new Vec3d(0, 0, z));
        final Vec3d half = new Vec3d((sx.x() + sy.x() + sz.x()) / 2, (sx.y() + sy.y() + sz.y()) / 2,
                (sx.z() + sy.z() + sz.z()) / 2);
        final Vec3 near = new Vec3(center.x - half.x(), center.y - half.y(), center.z - half.z());
        final Vec3 far = new Vec3(center.x + half.x(), center.y + half.y(), center.z + half.z());
        return new AABB(near, far);
    }

    @Unique
    private static Vec3d hillsphereUp(GravityFrame frame) {

        return frame.toReal(new Vec3d(0, 1, 0));
    }

    @Unique
    private GravityFrame frame() {

        return ((FrameState) this).hillsphereFrame();
    }
}
