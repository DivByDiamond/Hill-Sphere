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
package dev.loki.hillsphere.client.goggles.flow;

import dev.loki.hillsphere.client.goggles.GogglesState;
import dev.loki.hillsphere.client.goggles.Palette;
import dev.loki.hillsphere.client.goggles.render.Brush;
import dev.loki.hillsphere.client.goggles.render.WorldLayer;
import dev.loki.hillsphere.config.GogglesConfig;
import dev.loki.hillsphere.field.GravityField;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.Gravity;
import dev.loki.hillsphere.field.view.FieldProbe;
import dev.loki.hillsphere.world.ClientFields;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Streaks that flow along "down" around the wearer. Space is cut into cells; in each cell a streak lives for a
 * while at a random spot, then is born again somewhere else in the cell, so there is no visible grid. Where the
 * field is strong streaks are more frequent, longer and faster; they thin out towards the field's edge and the
 * range. Levitation shows as motes drifting up instead. Streaks are worked out once a tick.
 */
public final class FlowLayer implements WorldLayer {

    private static final double CELL = 2.4;
    private static final int MAX_STREAKS = 450;
    private static final double SHORTEST = 0.7;
    private static final double LONGEST = 2.1;
    private static final double STRONG = 2.0;

    private List<Streak> streaks = List.of();

    @Override
    public void tick(GogglesState state) {

        if (state.near().isEmpty()) {
            streaks = List.of();
            return;
        }
        final double density = GogglesConfig.DENSITY.get();
        final double range = GogglesConfig.RANGE.get();
        final double cell = CELL / Math.cbrt(density);
        final int reach = (int) Math.ceil(range / cell);
        final Vec3d eye = state.eye();
        final int cx = (int) Math.floor(eye.x() / cell);
        final int cy = (int) Math.floor(eye.y() / cell);
        final int cz = (int) Math.floor(eye.z() / cell);
        final List<Streak> found = new ArrayList<>();
        final int limit = (int) (MAX_STREAKS * density);
        for (int x = cx - reach; x <= cx + reach && found.size() < limit; x++) {
            for (int y = cy - reach; y <= cy + reach; y++) {
                for (int z = cz - reach; z <= cz + reach; z++) {
                    final Streak streak = seed(state, new Cell(x, y, z, cell), range);
                    if (streak != null) {
                        found.add(streak);
                    }
                }
            }
        }
        streaks = found;
    }

    @Override
    public void render(Brush brush, GogglesState state, float partialTick) {

        final double time = state.time(partialTick);
        for (final Streak streak : streaks) {
            if (!brush.hasBudget()) {
                return;
            }
            streak.render(brush, time);
        }
    }

    /** The streak alive in the cell right now, or null if the cell has none. */
    private static Streak seed(GogglesState state, Cell cell, double range) {

        final double period = 40 + 30 * cell.random(0, 1);
        final double offset = cell.random(0, 2) * period;
        final long life = (long) Math.floor((state.ticks() + offset) / period);
        final Vec3d at = cell.point(life);
        final double distance = at.sub(state.eye()).length();
        if (distance > range) {
            return null;
        }
        final FieldProbe probe = state.probe(at);
        if (!probe.inField() || cell.random(life, 3) > Math.pow(probe.influence(), 0.7)
                || Minecraft.getInstance().level.getBlockState(BlockPos.containing(at.x(), at.y(), at.z())).canOcclude()) {
            return null;
        }
        final double alpha = GogglesConfig.OPACITY.get() * Math.pow(probe.influence(), 0.6)
                * Brush.fadeWithDistance(distance, range);
        final double born = life * period - offset;
        final GravityField<?> field = ClientFields.field();
        if (probe.polarity() == Polarity.LEVITATE) {
            final double lift = Math.min(1.5, field.liftAt(at));
            final Vec3d up = field.planetAt(at).direction().scale(-1);
            final int points = 2 + (int) Math.round(lift * 2);
            return new Streak(path(at, up, points), born, period * 1.5, Palette.LEVITATE, alpha, true);
        }
        final Gravity gravity = field.gravityAt(at);
        final double strength = Math.min(STRONG, Math.abs(gravity.strength())) / STRONG;
        if (cell.random(life, 4) > 0.3 + 0.7 * strength) {
            return null;
        }
        final double length = SHORTEST + (LONGEST - SHORTEST) * strength;
        final Vec3[] path = streamline(field, at, Math.signum(gravity.strength()), (int) Math.ceil(length / Streak.STEP) + 1);
        return new Streak(path, born, period / (0.6 + strength), Palette.of(probe.polarity()), alpha, false);
    }

    /** Follows "down" from the point, turning with the field. */
    private static Vec3[] streamline(GravityField<?> field, Vec3d from, double sign, int points) {

        final Vec3[] path = new Vec3[points];
        Vec3d p = from;
        for (int i = 0; i < points; i++) {
            path[i] = GogglesState.of(p);
            p = p.add(field.gravityAt(p).direction().scale(Streak.STEP * sign));
        }
        return path;
    }

    private static Vec3[] path(Vec3d from, Vec3d direction, int points) {

        final Vec3[] path = new Vec3[points];
        for (int i = 0; i < points; i++) {
            path[i] = GogglesState.of(from.add(direction.scale(Streak.STEP * i)));
        }
        return path;
    }

    /** A cell of space, with stable random numbers. */
    private record Cell(int x, int y, int z, double size) {

        Vec3d point(long life) {

            return new Vec3d((x + random(life, 5)) * size, (y + random(life, 6)) * size, (z + random(life, 7)) * size);
        }

        /** A number in [0, 1) that only depends on the cell, the life and the salt. */
        double random(long life, int salt) {

            long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L ^ life * 0xD6E8FEB86659FD93L ^ salt;
            h = (h ^ h >>> 30) * 0xBF58476D1CE4E5B9L;
            h = (h ^ h >>> 27) * 0x94D049BB133111EBL;
            h ^= h >>> 31;
            return (h >>> 11) * 0x1.0p-53;
        }
    }
}
