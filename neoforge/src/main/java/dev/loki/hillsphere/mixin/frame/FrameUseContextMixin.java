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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.field.math.Vec3d;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Placement reads the player's yaw to face stairs, doors and the like away from them. The stored
 * yaw is a virtual angle; in a turned frame the world wants the yaw of where the player actually
 * looks, or every placed block faces around a phantom corner.
 */
@Mixin(UseOnContext.class)
public abstract class FrameUseContextMixin {

    @ModifyReturnValue(method = "getRotation()F", at = @At("RETURN"))
    private float hillsphereFrameYaw(float original) {

        final Player player = ((UseOnContext) (Object) this).getPlayer();
        if (player == null) {
            return original;
        }
        final GravityFrame frame = LivingGravity.frameOf(player);
        if (frame.isVanilla()) {
            return original;
        }
        final double pitch = Math.toRadians(player.getXRot());
        final double yaw = Math.toRadians(-original);
        final Vec3d look = frame.toReal(new Vec3d(
                Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)));
        return Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-look.x(), look.z())));
    }
}
