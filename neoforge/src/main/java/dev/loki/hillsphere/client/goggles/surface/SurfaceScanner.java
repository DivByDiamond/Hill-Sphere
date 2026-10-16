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
import dev.loki.hillsphere.compat.ShipPlot;
import dev.loki.hillsphere.compat.Ships;
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
import java.util.function.UnaryOperator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Finds the block faces one could stand on inside fields: a sturdy face on the side "up" points to, with room
 * above it. Points where levitation cancels gravity are skipped; nobody stands there. The blocks of ships are
 * scanned in the ship's own space and their faces carried to where the ship is. Results are sorted by distance
 * and cut to a limit.
 */
final class SurfaceScanner {

    private static final UnaryOperator<Vec3d> SAME = UnaryOperator.identity();

    /** Below this pull (in g) opposing cores all but cancel: "down" there is no floor to stand on. */
    private static final double WEIGHTLESS = 0.1;

    /**
     * A face to fill, in the world: its middle, its outward normal and two axes along it; {@code turns} if standing on it
     * would turn the player's "down", {@code seam} how close it lies to the seam of two cores (see {@link FieldProbe#seam()}).
     */
    record Face(Vec3 centre, Vec3 normal, Vec3 u, Vec3 v, Polarity polarity, boolean turns, double seam, double distance) {
    }

    private SurfaceScanner() {
    }

    static List<Face> scan(ClientLevel level, GogglesState state, GravityFrame wearer, double range, int limit) {

        final List<Face> found = new ArrayList<>();
        final Vec3d eye = state.eye();
        final BlockPos origin = BlockPos.containing(eye.x(), eye.y(), eye.z());
        final int r = (int) Math.ceil(range);
        scan(level, state, wearer, range, BlockPos.betweenClosed(origin.offset(-r, -r, -r), origin.offset(r, r, r)), SAME, SAME,
                SAME, found);
        for (final ShipPlot ship : Ships.shipsNear(level, eye, range)) {
            scan(level, state, wearer, range, near(ship, eye, r), ship.toWorld(), ship.dirToWorld(),
                    ship.dirToLocal(), found);
        }
        found.sort(Comparator.comparingDouble(Face::distance));
        return found.size() > limit ? List.copyOf(found.subList(0, limit)) : found;
    }

    /**
     * The blocks of a ship that can lie within the range: the ship's blocks cut to a cube around the eye in the ship's
     * own space (a turned ship's reach fits inside it), so a big ship costs no more than a small one.
     */
    private static Iterable<BlockPos> near(ShipPlot ship, Vec3d eye, int r) {

        final Vec3d at = ship.toLocal().apply(eye);
        final BlockPos from = new BlockPos(Math.max(ship.min().getX(), (int) Math.floor(at.x()) - r),
                Math.max(ship.min().getY(), (int) Math.floor(at.y()) - r), Math.max(ship.min().getZ(), (int) Math.floor(at.z()) - r));
        final BlockPos to = new BlockPos(Math.min(ship.max().getX(), (int) Math.floor(at.x()) + r),
                Math.min(ship.max().getY(), (int) Math.floor(at.y()) + r), Math.min(ship.max().getZ(), (int) Math.floor(at.z()) + r));
        return from.getX() > to.getX() || from.getY() > to.getY() || from.getZ() > to.getZ() ? List.of() : BlockPos.betweenClosed(from, to);
    }

    /** Looks at the blocks of one space; {@code toWorld} and {@code dirToWorld} carry them to the world, the identity for the world's own. */
    private static void scan(ClientLevel level, GogglesState state, GravityFrame wearer, double range, Iterable<BlockPos> blocks,
            UnaryOperator<Vec3d> toWorld, UnaryOperator<Vec3d> dirToWorld, UnaryOperator<Vec3d> dirToLocal, List<Face> found) {

        final Vec3d eye = state.eye();
        for (final BlockPos pos : blocks) {
            final Vec3d local = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            final Vec3d centre = toWorld.apply(local);
            final double distance = centre.sub(eye).length();
            final FieldProbe probe = distance > range ? null : state.probe(centre);
            final Vec3d down = probe != null && probe.inField() ? downAt(centre) : null;
            if (down == null) {
                continue;
            }
            final Vec3d wantedUp = dirToLocal.apply(down.scale(-1));
            final Direction up = Direction.getNearest((float) wantedUp.x(), (float) wantedUp.y(), (float) wantedUp.z());
            if (standable(level, pos, up)) {
                found.add(face(local, up, toWorld, dirToWorld, probe, GravityFrame.of(down) != wearer, distance));
            }
        }
    }

    private static Face face(Vec3d local, Direction side, UnaryOperator<Vec3d> toWorld, UnaryOperator<Vec3d> dirToWorld,
            FieldProbe probe, boolean turns, double distance) {

        final Vec3d n = new Vec3d(side.getStepX(), side.getStepY(), side.getStepZ());
        final Vec3d[] t = tangents(side);
        final Vec3d middle = toWorld.apply(local.add(n.scale(0.5)));
        return new Face(vec3(middle), vec3(dirToWorld.apply(n)), vec3(dirToWorld.apply(t[0])), vec3(dirToWorld.apply(t[1])),
                probe.polarity(), turns, probe.seam(), distance);
    }

    /** The two unit axes lying in the face. */
    private static Vec3d[] tangents(Direction side) {

        return switch (side.getAxis()) {
            case X -> new Vec3d[] {new Vec3d(0, 1, 0), new Vec3d(0, 0, 1)};
            case Y -> new Vec3d[] {new Vec3d(1, 0, 0), new Vec3d(0, 0, 1)};
            case Z -> new Vec3d[] {new Vec3d(1, 0, 0), new Vec3d(0, 1, 0)};
        };
    }

    private static Vec3 vec3(Vec3d v) {

        return new Vec3(v.x(), v.y(), v.z());
    }

    /**
     * Where "down" points to in the point, as the world axis of the frame it falls to; null where the field leaves gravity
     * alone, where levitation cancels it, or where opposing cores all but cancel each other.
     */
    private static Vec3d downAt(Vec3d point) {

        final GravityField<?> field = ClientFields.field();
        final Gravity planet = field.planetAt(point);
        if (Gravity.VANILLA.equals(planet) || planet.strength() < WEIGHTLESS || field.liftAt(point) >= 1) {
            return null;
        }
        return GravityFrame.of(planet.direction()).toReal(Vec3d.DOWN);
    }

    private static boolean standable(ClientLevel level, BlockPos pos, Direction up) {

        final BlockPos above = pos.relative(up);
        return level.getBlockState(pos).isFaceSturdy(level, pos, up)
                && level.getBlockState(above).getCollisionShape(level, above).isEmpty();
    }
}
