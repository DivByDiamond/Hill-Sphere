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

import dev.loki.hillsphere.debug.Debug;
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

    /** Every time the server puts a player back or somewhere else; a bounce that is the server's doing shows here. */
    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void hillsphereLogTeleport(double x, double y, double z, float yRot, float xRot, java.util.Set<?> relative,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {

        final net.minecraft.server.level.ServerPlayer player = ((ServerGamePacketListenerImpl) (Object) this).player;
        Debug.log("S {} TELEPORT t={} from={} to=({}, {}, {}) frame={}", player.getName().getString(), player.level().getGameTime(),
                player.position(), x, y, z, LivingGravity.frameOf(player));
    }

    @Inject(method = "noBlocksAround", at = @At("HEAD"), cancellable = true)
    private void hillsphereNotFloating(Entity entity, CallbackInfoReturnable<Boolean> cir) {

        if (FieldLookup.liftAt(entity) > HOVER_LIFT || !LivingGravity.frameOf(entity).isVanilla()) {
            cir.setReturnValue(false);
        }
    }
}
