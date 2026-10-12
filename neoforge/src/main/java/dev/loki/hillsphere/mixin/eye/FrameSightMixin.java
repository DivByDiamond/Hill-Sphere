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
package dev.loki.hillsphere.mixin.eye;

import dev.loki.hillsphere.entity.LivingGravity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mobs see a target along the line from eye to eye, but vanilla builds both eyes from the feet's x and z.
 * When either body is turned the line is drawn between the real eyes, so a player on a wall is not hidden
 * behind the very block they stand on.
 */
@Mixin(LivingEntity.class)
public abstract class FrameSightMixin {

    /** Vanilla's limit: nothing farther than this is in sight. */
    private static final double SIGHT = 128.0;

    @Inject(method = "hasLineOfSight(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void hillsphereSightBetweenEyes(Entity entity, CallbackInfoReturnable<Boolean> cir) {

        final LivingEntity self = (LivingEntity) (Object) this;
        if (entity.level() != self.level()
                || LivingGravity.frameOf(self).isVanilla() && LivingGravity.frameOf(entity).isVanilla()) {
            return;
        }
        final Vec3 from = self.getEyePosition();
        final Vec3 to = entity.getEyePosition();
        cir.setReturnValue(to.distanceTo(from) <= SIGHT && self.level().clip(
                new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self)).getType() == HitResult.Type.MISS);
    }
}
