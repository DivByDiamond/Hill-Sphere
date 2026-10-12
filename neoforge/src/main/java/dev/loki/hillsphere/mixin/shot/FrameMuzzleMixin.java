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
package dev.loki.hillsphere.mixin.shot;

import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.frame.GravityFrame;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Arrows and thrown things start a little below the shooter's eye, built from the feet's x and z. On a wall
 * that is inside the wall the shooter stands on; in a turned frame they start below the real eye instead.
 */
@Mixin({AbstractArrow.class, ThrowableProjectile.class})
public abstract class FrameMuzzleMixin {

    /** How far below the eye vanilla lets go of the shot. */
    private static final double BELOW_EYE = 0.1;

    @Inject(method = {
        "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;"
                + "Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V",
        "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;)V",
    }, at = @At("RETURN"))
    private void hillsphereLeaveFromTheEye(CallbackInfo ci) {

        final Projectile self = (Projectile) (Object) this;
        if (self.getOwner() instanceof LivingEntity owner) {
            final GravityFrame frame = LivingGravity.frameOf(owner);
            if (!frame.isVanilla()) {
                self.setPos(LivingGravity.above(frame, owner.position(), owner.getEyeHeight() - BELOW_EYE));
            }
        }
    }
}
