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
package dev.loki.hillsphere.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.loki.hillsphere.entity.FrameState;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.field.math.Vec3d;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs the parts of living movement that assume "down" is -y inside the entity's frame. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void hillsphereLookUpTheField(CallbackInfo ci) {

        ((FrameState) this).hillsphereRefresh();
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void hillsphereEnterOnTravel(Vec3 input, CallbackInfo ci) {

        enter();
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void hillsphereLeaveOnTravel(Vec3 input, CallbackInfo ci) {

        leave();
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void hillsphereEnterOnJump(CallbackInfo ci) {

        enter();
    }

    @Inject(method = "jumpFromGround", at = @At("RETURN"))
    private void hillsphereLeaveOnJump(CallbackInfo ci) {

        leave();
    }

    /**
     * A pull of twice the vanilla gravity must not halve the hop. Height goes as the square of the
     * power over gravity, so power scales with the square root of the pull: the hop clears a block
     * at any pull, only the time in the air changes (shorter in a strong pull, longer in a weak one).
     */
    @ModifyReturnValue(method = "getJumpPower()F", at = @At("RETURN"))
    private float hillsphereJumpPower(float original) {

        final double strength = ((FrameState) this).hillsphereStrength();
        if (strength <= 0.0 || strength == 1.0) {
            return original;
        }
        return (float) (original * Math.sqrt(strength));
    }

    /** Velocity becomes virtual: what vanilla thinks of as straight down is where the field pulls. */
    private void enter() {

        final LivingEntity self = (LivingEntity) (Object) this;
        final GravityFrame frame = ((FrameState) this).hillsphereFrame();
        if (!frame.isVanilla()) {
            ((FrameState) this).hillsphereInside(true);
            final Vec3d v = frame.toVirtual(new Vec3d(self.getDeltaMovement().x, self.getDeltaMovement().y, self.getDeltaMovement().z));
            self.setDeltaMovement(v.x(), v.y(), v.z());
        }
    }

    private void leave() {

        final LivingEntity self = (LivingEntity) (Object) this;
        final GravityFrame frame = ((FrameState) this).hillsphereFrame();
        if (!frame.isVanilla()) {
            final Vec3d v = frame.toReal(new Vec3d(self.getDeltaMovement().x, self.getDeltaMovement().y, self.getDeltaMovement().z));
            self.setDeltaMovement(v.x(), v.y(), v.z());
            ((FrameState) this).hillsphereInside(false);
        }
    }
}
