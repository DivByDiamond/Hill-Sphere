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

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A dropped item leaves from below the eye, built from the feet's x and z, and is tossed along the yaw and
 * pitch with a little lift. In a turned frame all of that is virtual: the item starts below the real eye
 * and the toss is turned into the world, so on a ceiling it is not thrown into the ceiling.
 */
@Mixin(Player.class)
public abstract class FrameDropMixin {

    /** How far below the eye vanilla lets go of the item. */
    private static final double BELOW_EYE = 0.3;

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("RETURN"))
    private void hillsphereDropInFrame(ItemStack stack, boolean around, boolean thrower, CallbackInfoReturnable<ItemEntity> cir) {

        final Player self = (Player) (Object) this;
        final GravityFrame frame = LivingGravity.frameOf(self);
        final ItemEntity item = cir.getReturnValue();
        if (item == null || frame.isVanilla()) {
            return;
        }
        item.setPos(LivingGravity.above(frame, self.position(), self.getEyeHeight() - BELOW_EYE));
        item.setDeltaMovement(LivingGravity.toReal(frame, item.getDeltaMovement()));
    }
}
