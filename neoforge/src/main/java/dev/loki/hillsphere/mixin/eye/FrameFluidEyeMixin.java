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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.loki.hillsphere.entity.LivingGravity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Which fluid covers the eyes (drowning, the underwater view, swimming) is read from the block at the feet's x
 * and z and the eye's y. On a wall that block is the one at the feet, so the head could be under water while
 * the body breathed, or the reverse. In a turned frame the block is taken at the real eye; the height of the
 * fluid's surface is still compared with the eye's y, because water stays level whatever the frame says.
 */
@Mixin(Entity.class)
public abstract class FrameFluidEyeMixin {

    @WrapOperation(method = "updateFluidOnEyes", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;containing(DDD)Lnet/minecraft/core/BlockPos;"))
    private BlockPos hillsphereFluidAtTheEye(double x, double y, double z, Operation<BlockPos> original) {

        final Entity self = (Entity) (Object) this;
        return LivingGravity.frameOf(self).isVanilla() ? original.call(x, y, z) : BlockPos.containing(self.getEyePosition());
    }
}
