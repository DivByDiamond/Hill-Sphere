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
package dev.loki.hillsphere.mixin.debug;

import dev.loki.hillsphere.debug.Debug;
import dev.loki.hillsphere.entity.LivingGravity;

import java.util.Arrays;
import java.util.stream.Collectors;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Debug only: a player in a turned frame who is set down somewhere far from where they were gets the call stack that did it logged. */
@Mixin(Entity.class)
public abstract class MoveTraceMixin {

    private static final double JUMP = 0.5;

    @org.spongepowered.asm.mixin.Unique
    private net.minecraft.world.phys.Vec3 hillsphereAsked = net.minecraft.world.phys.Vec3.ZERO;

    @Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void hillsphereRememberAsk(net.minecraft.world.entity.MoverType type, net.minecraft.world.phys.Vec3 movement, CallbackInfo ci) {

        hillsphereAsked = movement;
    }

    @Inject(method = "setPosRaw(DDD)V", at = @At("HEAD"))
    private void hillsphereTraceBigMoves(double x, double y, double z, CallbackInfo ci) {

        if (!Debug.on() || !((Object) this instanceof Player player) || LivingGravity.frameOf(player).isVanilla()) {
            return;
        }
        final double moved = player.position().distanceTo(new net.minecraft.world.phys.Vec3(x, y, z));
        if (moved > JUMP && moved < 50) {
            final String stack = Arrays.stream(new Throwable().getStackTrace()).skip(1).limit(14)
                    .map(e -> e.getClassName().replaceAll("^.*\\.", "") + "." + e.getMethodName() + ":" + e.getLineNumber())
                    .collect(Collectors.joining(" < "));
            Debug.every("move" + player.getId() + player.level().isClientSide, player.level().getGameTime(), 3,
                    "{} BIG MOVE {} by {} from y={} to y={} asked={} vel={} ground={} via {}", player.level().isClientSide ? "C" : "S",
                    player.getName().getString(), String.format(java.util.Locale.ROOT, "%.3f", moved), player.getY(), y, hillsphereAsked, player.getDeltaMovement(), player.onGround(), stack);
        }
    }
}
