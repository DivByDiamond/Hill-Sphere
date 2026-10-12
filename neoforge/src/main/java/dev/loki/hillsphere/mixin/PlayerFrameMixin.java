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
import dev.loki.hillsphere.field.frame.GravityFrame;

import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two of Player's probes are written against world axes: the fit test builds an upright box around
 * the feet, and the sneak edge guard looks for ground below in world -y. On a wall or a ceiling
 * that made the player fail to "fit" as standing (pose fell through to swimming) and the edge
 * guard zeroed every step. Both take the turned frame instead.
 */
@Mixin(Player.class)
public abstract class PlayerFrameMixin {

    /** Whether the body fits as the given pose, measured in the frame the field has put it in. */
    @Inject(method = "canPlayerFitWithinBlocksAndEntitiesWhen(Lnet/minecraft/world/entity/Pose;)Z",
            at = @At("HEAD"), cancellable = true)
    private void hillsphereFitInFrame(Pose pose, CallbackInfoReturnable<Boolean> cir) {

        final Player self = (Player) (Object) this;
        final GravityFrame frame = LivingGravity.frameOf(self);
        if (frame.isVanilla()) {
            return;
        }
        cir.setReturnValue(self.level().noCollision(self,
                LivingGravity.box(frame, self.position(), self.getDimensions(pose)).deflate(1.0E-7)));
    }

    /** The sneak edge guard falls along world -y, which freezes the player on a wall or ceiling. */
    @Inject(method = "maybeBackOffFromEdge(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/MoverType;)Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"), cancellable = true)
    private void hillsphereNoWorldEdgeBackoff(Vec3 vec, MoverType mover, CallbackInfoReturnable<Vec3> cir) {

        final Player self = (Player) (Object) this;
        if (!LivingGravity.frameOf(self).isVanilla()) {
            cir.setReturnValue(vec);
        }
    }
}
