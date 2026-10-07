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

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Where "down" is up, a jump goes down. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void hillsphereJumpAwayFromFloor(CallbackInfo ci) {

        final LivingEntity self = (LivingEntity) (Object) this;
        if (LivingGravity.factor(self) < 0) {
            final Vec3 v = self.getDeltaMovement();
            self.setDeltaMovement(v.x, -v.y, v.z);
        }
    }

    /** Upside down, the player's left is the world's right. */
    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3 hillsphereMirrorSteering(Vec3 input) {

        final LivingEntity self = (LivingEntity) (Object) this;
        return self instanceof Player && LivingGravity.factor(self) < 0 ? new Vec3(-input.x, input.y, input.z) : input;
    }
}
