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
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The light a body is drawn with is read at the eye's height above the feet's column. On a wall or a ceiling that
 * point lies inside the block under the feet, where the light is zero, and the whole model turns black. In a turned
 * frame the light is read in the middle of the body, which is always in the air it stands in.
 */
@Mixin(Entity.class)
public abstract class FrameLightMixin {

    @Inject(method = "getLightProbePosition", at = @At("RETURN"), cancellable = true)
    private void hillsphereLightInTheMiddle(float partialTick, CallbackInfoReturnable<Vec3> cir) {

        final Entity self = (Entity) (Object) this;
        if (!LivingGravity.frameOf(self).isVanilla()) {
            cir.setReturnValue(self.getBoundingBox().getCenter());
        }
    }
}
