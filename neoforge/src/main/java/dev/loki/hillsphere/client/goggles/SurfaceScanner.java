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
package dev.loki.hillsphere.client.goggles;

import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.Gravity;
import dev.loki.hillsphere.world.ClientFields;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Finds the block faces a player could stand on where a field has turned "down": a solid face on the side
 * that "up" points to, with free space in front of it. The scan is cached for a few ticks.
 */
final class SurfaceScanner {

    private static final int REACH_XZ = 12;
    private static final int REACH_Y = 8;
    private static final int RESCAN_TICKS = 10;

    /** A face to highlight: the block and the side of it you would stand on. */
    record Surface(BlockPos pos, Direction face) {

        Vec3 center() {

            return Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5 + 0.02));
        }
    }

    private List<Surface> cached = List.of();
    private long scannedAt = Long.MIN_VALUE;
    private BlockPos scannedFrom = BlockPos.ZERO;

    /** Surfaces around the position; the same list is returned until it is old or the player has moved. */
    List<Surface> around(ClientLevel level, BlockPos origin) {

        final long now = level.getGameTime();
        if (now - scannedAt >= RESCAN_TICKS || now < scannedAt || !origin.closerThan(scannedFrom, 3)) {
            cached = scan(level, origin);
            scannedAt = now;
            scannedFrom = origin;
        }
        return cached;
    }

    private static List<Surface> scan(ClientLevel level, BlockPos origin) {

        final List<Surface> found = new ArrayList<>();
        for (final BlockPos pos : BlockPos.betweenClosed(origin.offset(-REACH_XZ, -REACH_Y, -REACH_XZ),
                origin.offset(REACH_XZ, REACH_Y, REACH_XZ))) {
            final BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            final Direction up = upAt(pos);
            if (up != null && state.isFaceSturdy(level, pos, up)
                    && level.getBlockState(pos.relative(up)).getCollisionShape(level, pos.relative(up)).isEmpty()) {
                found.add(new Surface(pos.immutable(), up));
            }
        }
        return found;
    }

    /** The side of the block facing away from the pull, or null where no field changes gravity. */
    private static Direction upAt(BlockPos pos) {

        final Gravity gravity = ClientFields.field().planetAt(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        if (Gravity.VANILLA.equals(gravity)) {
            return null;
        }
        final Vec3d down = gravity.direction();
        return Direction.getNearest((float) -down.x(), (float) -down.y(), (float) -down.z());
    }
}
