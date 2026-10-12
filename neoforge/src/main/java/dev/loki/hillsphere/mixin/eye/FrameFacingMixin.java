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

import java.util.Arrays;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pistons, observers, dispensers, hoppers and dripstone face the way the player looks, worked out from the
 * stored yaw and pitch. Those angles are virtual in a turned frame, so the answer is taken from the real look
 * instead: on a ceiling, looking at the ceiling is looking up.
 */
@Mixin(Direction.class)
public abstract class FrameFacingMixin {

    /** The six directions, nearest to the look first, the way vanilla orders them. */
    @Inject(method = "orderedByNearest(Lnet/minecraft/world/entity/Entity;)[Lnet/minecraft/core/Direction;",
            at = @At("HEAD"), cancellable = true)
    private static void hillsphereNearestToTheRealLook(Entity entity, CallbackInfoReturnable<Direction[]> cir) {

        if (LivingGravity.frameOf(entity).isVanilla()) {
            return;
        }
        final Vec3 look = entity.getViewVector(1.0F);
        final Direction[] near = {
            hillsphereToward(look, Direction.Axis.X), hillsphereToward(look, Direction.Axis.Y), hillsphereToward(look, Direction.Axis.Z),
        };
        Arrays.sort(near, (a, b) -> Double.compare(Math.abs(b.getAxis().choose(look.x, look.y, look.z)),
                Math.abs(a.getAxis().choose(look.x, look.y, look.z))));
        cir.setReturnValue(new Direction[] {near[0], near[1], near[2], near[2].getOpposite(), near[1].getOpposite(), near[0].getOpposite()});
    }

    /** The side of one axis the player looks toward. */
    @Inject(method = "getFacingAxis(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Direction$Axis;)Lnet/minecraft/core/Direction;",
            at = @At("HEAD"), cancellable = true)
    private static void hillsphereFacingAlongTheRealLook(Entity entity, Direction.Axis axis, CallbackInfoReturnable<Direction> cir) {

        if (!LivingGravity.frameOf(entity).isVanilla()) {
            cir.setReturnValue(hillsphereToward(entity.getViewVector(1.0F), axis));
        }
    }

    @Unique
    private static Direction hillsphereToward(Vec3 look, Direction.Axis axis) {

        return Direction.fromAxisAndDirection(axis, axis.choose(look.x, look.y, look.z) > 0
                ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
    }
}
