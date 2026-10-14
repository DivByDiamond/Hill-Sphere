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
package dev.loki.hillsphere.network;

import dev.loki.hillsphere.blockentity.HillCoreBlockEntity;
import dev.loki.hillsphere.compat.Ships;
import dev.loki.hillsphere.field.math.Vec3d;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Applies what a player set on the core screen, if they really stand at that core. */
final class SettingsReceiver {

    /** How far from the core a player may still change it, in blocks. */
    private static final double REACH = 8;

    private SettingsReceiver() {
    }

    static void receive(CoreSettingsPayload payload, IPayloadContext context) {

        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && reaches(player, payload.pos())
                    && player.level().getBlockEntity(payload.pos()) instanceof HillCoreBlockEntity core) {
                core.settings().configure(payload.polarity(), payload.level(), payload.mode());
                core.settings().reshape(payload.shape());
            }
        });
    }

    /** The core may sit on a ship, so its place is taken in the world before measuring. */
    private static boolean reaches(ServerPlayer player, BlockPos pos) {

        final Level level = player.level();
        if (!level.isLoaded(pos)) {
            return false;
        }
        final Vec3d at = Ships.toWorld(level, new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        return player.distanceToSqr(at.x(), at.y(), at.z()) <= REACH * REACH;
    }
}
