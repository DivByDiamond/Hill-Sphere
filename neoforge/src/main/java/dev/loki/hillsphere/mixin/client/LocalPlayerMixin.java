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
package dev.loki.hillsphere.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.field.math.Vec3d;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LocalPlayer helpers that believe in one world "up", and their answers once the body has turned.
 *
 * <p>{@code moveTowardsClosestSpace} runs four probes a tick around the feet, but it tests a
 * world-upright column at each corner: with the body turned, the corner that hangs over the support
 * reads as a suffocating block and the helper shoves a raw 0.1 of world horizontal speed each tick.
 * Standing on a wall that shove pointed away from the wall, i.e. along the frame's "up": the player
 * was pushed upward, jittered between the shove and the pull, and could not step onto anything.
 * Penetration is already resolved by the collision pass, so the helper simply stands down in a
 * turned frame.
 *
 * <p>{@code aiStep} adds the creative-flight vertical along world Y whatever the frame says, so
 * space and shift did nothing while lying on a side. That add is neutralized by zeroing the flying
 * speed it multiplies (Sable already owns the {@code Vec3.add} call itself, a redirect there would
 * conflict), and the same amount is applied along the frame's "up" at the end of the tick instead,
 * which is where the view's up is too.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "moveTowardsClosestSpace(DD)V", at = @At("HEAD"), cancellable = true)
    private void hillsphereSkipWorldProbe(double x, double z, CallbackInfo ci) {

        final LocalPlayer self = (LocalPlayer) (Object) this;
        if (!LivingGravity.frameOf(self).isVanilla()) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(
            method = "aiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Abilities;getFlyingSpeed()F"))
    private float hillsphereZeroWorldFlightSpeed(float original) {

        final LocalPlayer self = (LocalPlayer) (Object) this;
        return LivingGravity.frameOf(self).isVanilla() ? original : 0.0F;
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void hillsphereFlyAlongFrame(CallbackInfo ci) {

        final LocalPlayer self = (LocalPlayer) (Object) this;
        final GravityFrame frame = LivingGravity.frameOf(self);
        if (frame.isVanilla()
                || !self.getAbilities().flying
                || Minecraft.getInstance().getCameraEntity() != self) {
            return;
        }
        int j = self.input.jumping ? 1 : 0;
        if (self.input.shiftKeyDown) {
            j--;
        }
        if (j == 0) {
            return;
        }
        final double amount = j * self.getAbilities().getFlyingSpeed() * 3.0;
        final Vec3d up = frame.toReal(new Vec3d(0, 1, 0));
        final Vec3 delta = self.getDeltaMovement();
        self.setDeltaMovement(delta.add(up.x() * amount, up.y() * amount, up.z() * amount));
    }
}
