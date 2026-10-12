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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.field.math.Vec3d;

import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Three of Player's helpers are written against world axes: the fit test builds an upright box around
 * the feet, and the sneak edge guard looks for ground below in world -y. On a wall or a ceiling
 * that made the player fail to "fit" as standing (pose fell through to swimming) and the edge
 * guard zeroed every step. Creative flight holds the vertical speed of the tick before, along world Y.
 * All three take the turned frame instead.
 */
@Mixin(Player.class)
public abstract class PlayerFrameMixin {

    /** Vanilla flight keeps this much of the speed along "up" each tick. */
    private static final double FLIGHT_HOLD = 0.6;

    /** The real velocity as the flying tick began, while the frame is turned; null otherwise. */
    @Unique private Vec3 hillsphereFlightStart;

    /** Whether the body fits as the given pose, measured in the frame the field has put it in. */
    @Inject(method = "canPlayerFitWithinBlocksAndEntitiesWhen(Lnet/minecraft/world/entity/Pose;)Z",
            at = @At("HEAD"), cancellable = true)
    private void hillsphereFitInFrame(Pose pose, CallbackInfoReturnable<Boolean> cir) {

        final Player self = (Player) (Object) this;
        final GravityFrame frame = LivingGravity.frameOf(self);
        if (frame.isVanilla()) {
            return;
        }
        cir.setReturnValue(self.level().noCollision(self,
                LivingGravity.box(frame, self.position(), self.getDimensions(pose)).deflate(1.0E-7)));
    }

    /** The sneak edge guard falls along world -y, which freezes the player on a wall or ceiling. */
    @Inject(method = "maybeBackOffFromEdge(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/MoverType;)Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"), cancellable = true)
    private void hillsphereNoWorldEdgeBackoff(Vec3 vec, MoverType mover, CallbackInfoReturnable<Vec3> cir) {

        final Player self = (Player) (Object) this;
        if (!LivingGravity.frameOf(self).isVanilla()) {
            cir.setReturnValue(vec);
        }
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void hillsphereRememberFlight(Vec3 input, CallbackInfo ci) {

        final Player self = (Player) (Object) this;
        final boolean turnedFlight = self.getAbilities().flying && !LivingGravity.frameOf(self).isVanilla();
        hillsphereFlightStart = turnedFlight ? self.getDeltaMovement() : null;
    }

    /**
     * Flight cancels gravity by putting back 0.6 of the vertical speed it started the tick with. That speed
     * is world Y, which on a wall is a sideways one: the pull along "down" was never cancelled and the hold
     * landed on the wrong axis. In a turned frame the hold is taken along the frame's "up" instead.
     */
    @WrapOperation(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(DDD)V"))
    private void hillsphereHoldFlightInFrame(Player self, double x, double y, double z, Operation<Void> original) {

        final GravityFrame frame = LivingGravity.frameOf(self);
        if (hillsphereFlightStart == null || frame.isVanilla()) {
            original.call(self, x, y, z);
            return;
        }
        final Vec3 now = self.getDeltaMovement();
        final Vec3d after = frame.toVirtual(new Vec3d(now.x, now.y, now.z));
        final Vec3d before = frame.toVirtual(new Vec3d(hillsphereFlightStart.x, hillsphereFlightStart.y, hillsphereFlightStart.z));
        final Vec3 held = LivingGravity.toReal(frame, new Vec3(after.x(), before.y() * FLIGHT_HOLD, after.z()));
        original.call(self, held.x, held.y, held.z);
    }
}
