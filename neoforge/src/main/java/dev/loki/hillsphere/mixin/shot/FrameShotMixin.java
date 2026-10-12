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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.frame.GravityFrame;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Bows, tridents, snowballs and pearls are aimed from the shooter's yaw and pitch. In a turned frame those
 * angles are virtual, so the launch direction is turned into the world the way the view is: on a ceiling the
 * arrow flies where the crosshair is, not into its mirror image.
 */
@Mixin(Projectile.class)
public abstract class FrameShotMixin {

    @WrapOperation(method = "shootFromRotation", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/Projectile;shoot(DDDFF)V"))
    private void hillsphereShootInFrame(Projectile self, double x, double y, double z, float velocity, float inaccuracy,
            Operation<Void> original, Entity shooter) {

        final GravityFrame frame = LivingGravity.frameOf(shooter);
        if (frame.isVanilla()) {
            original.call(self, x, y, z, velocity, inaccuracy);
            return;
        }
        final Vec3 aim = LivingGravity.toReal(frame, new Vec3(x, y, z));
        original.call(self, aim.x, aim.y, aim.z, velocity, inaccuracy);
    }
}
