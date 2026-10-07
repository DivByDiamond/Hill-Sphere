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
import dev.loki.hillsphere.world.FieldLookup;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A player held up by a levitation core must not be kicked for "flying", so the server does not count it as floating. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPacketMixin {

    private static final double HOVER_LIFT = 0.5;

    @Inject(method = "noBlocksAround", at = @At("HEAD"), cancellable = true)
    private void hillsphereNotFloating(Entity entity, CallbackInfoReturnable<Boolean> cir) {

        if (FieldLookup.liftAt(entity) > HOVER_LIFT || LivingGravity.factor(entity) < 0) {
            cir.setReturnValue(false);
        }
    }
}
