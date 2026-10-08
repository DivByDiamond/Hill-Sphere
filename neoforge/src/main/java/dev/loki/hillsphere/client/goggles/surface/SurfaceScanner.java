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
package dev.loki.hillsphere.client.goggles.surface;

import dev.loki.hillsphere.client.goggles.GogglesState;
import dev.loki.hillsphere.field.GravityField;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.Gravity;
import dev.loki.hillsphere.field.view.FieldProbe;
import dev.loki.hillsphere.world.ClientFields;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Finds the block faces one could stand on inside fields: a sturdy face on the side "up" points to, with room
 * above it. Points where levitation cancels gravity are skipped; nobody stands there. Results are sorted by
 * distance and cut to a limit.
 */
final class SurfaceScanner {

    /** A face to fill; {@code turns} if standing on it would turn the player's "down". */
    record Face(BlockPos pos, Direction side, Polarity polarity, boolean turns, double distance) {
    }

    private SurfaceScanner() {
    }

    static List<Face> scan(ClientLevel level, GogglesState state, GravityFrame wearer, double range, int limit) {

        final Vec3d eye = state.eye();
        final BlockPos origin = BlockPos.containing(eye.x(), eye.y(), eye.z());
        final int r = (int) Math.ceil(range);
        final List<Face> found = new ArrayList<>();
        for (final BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -r, -r), origin.offset(r, r, r))) {
            final Vec3d centre = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            final double distance = centre.sub(eye).length();
            if (distance > range) {
                continue;
            }
            final FieldProbe probe = state.probe(centre);
            final Direction up = probe.inField() ? upAt(centre) : null;
            if (up != null && standable(level, pos, up)) {
                found.add(new Face(pos.immutable(), up, probe.polarity(), frameOf(up) != wearer, distance));
            }
        }
        found.sort(Comparator.comparingDouble(Face::distance));
        return found.size() > limit ? List.copyOf(found.subList(0, limit)) : found;
    }

    /** The side "up" points to in the point, or null where the field leaves gravity alone or cancels it. */
    private static Direction upAt(Vec3d point) {

        final GravityField<?> field = ClientFields.field();
        final Gravity planet = field.planetAt(point);
        if (Gravity.VANILLA.equals(planet) || field.liftAt(point) >= 1) {
            return null;
        }
        final Vec3d down = GravityFrame.of(planet.direction()).toReal(Vec3d.DOWN);
        return Direction.getNearest((float) -down.x(), (float) -down.y(), (float) -down.z());
    }

    private static GravityFrame frameOf(Direction up) {

        return GravityFrame.of(new Vec3d(-up.getStepX(), -up.getStepY(), -up.getStepZ()));
    }

    private static boolean standable(ClientLevel level, BlockPos pos, Direction up) {

        final BlockPos above = pos.relative(up);
        return level.getBlockState(pos).isFaceSturdy(level, pos, up)
                && level.getBlockState(above).getCollisionShape(level, above).isEmpty();
    }
}
