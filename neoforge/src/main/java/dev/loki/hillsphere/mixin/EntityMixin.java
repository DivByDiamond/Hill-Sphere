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

import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.Gravity;
import dev.loki.hillsphere.world.FieldLookup;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Levitation cores weaken the gravity every entity feels: players, mobs, items, falling blocks, arrows. */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "getGravity", at = @At("RETURN"), cancellable = true)
    private void hillsphereWeakenGravity(CallbackInfoReturnable<Double> cir) {

        final double gravity = cir.getReturnValueD();
        if (gravity == 0) {
            return;
        }
        final Entity self = (Entity) (Object) this;
        final double scaled = gravity * (1 - FieldLookup.liftAt(self)) * LivingGravity.factor(self);
        if (scaled != gravity) {
            cir.setReturnValue(scaled);
        }
    }

    /**
     * Items, falling blocks and projectiles fall toward an attracting core, or away from a repelling one,
     * instead of straight down. Living things use their own movement and are not changed here.
     */
    @Inject(method = "applyGravity", at = @At("HEAD"), cancellable = true)
    private void hillsphereFallTowardCores(CallbackInfo ci) {

        final Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity) {
            return;
        }
        final Gravity planet = FieldLookup.planetAt(self);
        if (Gravity.VANILLA.equals(planet)) {
            return;
        }
        final double pull = self.getGravity() * planet.strength();
        final Vec3d down = planet.direction();
        if (pull != 0) {
            self.setDeltaMovement(self.getDeltaMovement().add(down.x() * pull, down.y() * pull, down.z() * pull));
        }
        ci.cancel();
    }

    /** Hitting the ceiling is standing, when the ceiling is the floor. */
    @Inject(method = "move", at = @At("TAIL"))
    private void hillsphereStandOnTheCeiling(MoverType type, Vec3 movement, CallbackInfo ci) {

        final Entity self = (Entity) (Object) this;
        if (movement.y > 0 && self.verticalCollision && LivingGravity.factor(self) < 0) {
            self.setOnGround(true);
            if (self.fallDistance > 0) {
                self.causeFallDamage(self.fallDistance, 1f, self.damageSources().fall());
                self.resetFallDistance();
            }
        }
    }

    /** Falling up counts as falling: the distance adds up the same way and is paid for on the ceiling. */
    @Inject(method = "checkFallDamage", at = @At("HEAD"))
    private void hillsphereFallUp(double y, boolean onGround, BlockState state, BlockPos pos, CallbackInfo ci) {

        final Entity self = (Entity) (Object) this;
        if (y > 0 && !onGround && LivingGravity.factor(self) < 0) {
            self.fallDistance += (float) y;
        }
    }
}
