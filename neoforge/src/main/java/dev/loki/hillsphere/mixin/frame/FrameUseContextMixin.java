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

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Placement reads the player's yaw to face stairs, doors, furnaces, signs and the like toward or away from
 * them. The stored yaw is a virtual angle; in a turned frame the world wants the yaw of where the player
 * actually looks, or every placed block faces around a phantom corner.
 */
@Mixin(UseOnContext.class)
public abstract class FrameUseContextMixin {

    /** Below this squared length the look is too close to world vertical to give a heading. */
    private static final double FLAT = 1.0E-4;

    /** Signs, banners and skulls: the yaw of the real look. */
    @ModifyReturnValue(method = "getRotation()F", at = @At("RETURN"))
    private float hillsphereFrameYaw(float original) {

        final Vec3 look = hillsphereHeading();
        return look == null ? original : Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-look.x, look.z)));
    }

    /** Stairs, doors, furnaces, chests and beds: the compass direction of the real look. */
    @ModifyReturnValue(method = "getHorizontalDirection()Lnet/minecraft/core/Direction;", at = @At("RETURN"))
    private Direction hillsphereFrameFacing(Direction original) {

        final Vec3 look = hillsphereHeading();
        return look == null ? original : Direction.getNearest(look.x, 0, look.z);
    }

    /**
     * Where the player looks, in the world, with a usable horizontal part; null in vanilla. Looking straight
     * at a ceiling has no heading, so the facing of the body (the yaw alone) is used then.
     */
    @Unique
    private Vec3 hillsphereHeading() {

        final Player player = ((UseOnContext) (Object) this).getPlayer();
        if (player == null || LivingGravity.frameOf(player).isVanilla()) {
            return null;
        }
        Vec3 look = player.getViewVector(1.0F);
        if (look.x * look.x + look.z * look.z < FLAT) {
            look = player.calculateViewVector(0.0F, player.getYRot());
        }
        return look.x * look.x + look.z * look.z < FLAT ? null : look;
    }
}
