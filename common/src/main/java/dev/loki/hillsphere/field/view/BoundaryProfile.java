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
package dev.loki.hillsphere.field.view;

import dev.loki.hillsphere.field.FieldTuning;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.resolve.CoreField;

import java.util.List;

/**
 * The seam of two overlapping sphere fields: the surface where they pull equally hard, which is where the resolver
 * splits "down" half and half between them (see {@code docs/FIELDS.md}). It is round about the line between the two
 * centres, so it is traced in a half-plane: {@code along} that line from the first core and {@code radial} away from
 * it. The trace is a grid contour, so the seam may be closed, cross the axis twice or come in several pieces. Where
 * the plateaus of two equal fields overlap they pull equally hard all over; there the nearer core is taken to win, so
 * the seam is the plane halfway between them.
 */
public final class BoundaryProfile {

    private static final double TINY = 1e-6;
    /** Breaks ties on overlapping plateaus by distance; far below any real difference of two pulls. */
    private static final double TIE = 1e-9;

    /**
     * One point of the seam, in blocks. {@code weight} (0..1) is how much happens there: how far "down" turns crossing
     * it (0 where both pulls point the same way, 1 where they are opposite, so they cancel) times how deep inside the
     * fields it lies (1 on the plateaus, 0 at their edges, where vanilla gravity takes over).
     */
    public record Point(double along, double radial, double weight) {
    }

    /** A piece of the seam's outline. */
    public record Segment(Point from, Point to) {
    }

    /** The outline in pieces, and the points where it crosses each grid row, from which rings are drawn. */
    public record Profile(List<Segment> segments, List<Point> rings) {

        public boolean isEmpty() {

            return segments.isEmpty() && rings.isEmpty();
        }
    }

    private BoundaryProfile() {
    }

    /** The seam traced on a grid {@code rows} cells high, up to the smaller radius; empty when the fields do not overlap. */
    public static Profile between(FieldTuning tuning, CoreField a, CoreField b, int rows) {

        final double gap = b.center().sub(a.center()).length();
        if (gap < TINY || gap >= a.radius() + b.radius() || rows <= 0) {
            return new Profile(List.of(), List.of());
        }
        return new BoundaryGrid(new Pair(tuning, a, b, gap), rows).trace();
    }

    /** Two cores placed on an axis: the first at 0, the second at {@code gap}. */
    record Pair(FieldTuning tuning, CoreField a, CoreField b, double gap) {

        /** Positive where the first core wins. */
        double value(double along, double radial) {

            final double da = Math.hypot(along, radial);
            final double db = Math.hypot(gap - along, radial);
            return pull(a, da) - pull(b, db) + TIE * (db - da);
        }

        /** The seam point, or null where both fields have faded out and nothing balances. */
        Point point(double along, double radial) {

            final double fa = tuning.fade(Math.hypot(along, radial) / a.radius());
            final double fb = tuning.fade(Math.hypot(gap - along, radial) / b.radius());
            if (a.strength() * fa <= TINY) {
                return null;
            }
            final double[] ua = toward(a, 0, along, radial);
            final double[] ub = toward(b, gap, along, radial);
            final double turn = (1 - (ua[0] * ub[0] + ua[1] * ub[1])) / 2;
            return new Point(along, radial, turn * (fa + fb) / 2);
        }

        private double pull(CoreField core, double distance) {

            return core.strength() * tuning.fade(distance / core.radius());
        }

        /** The core's pull direction in the half-plane, as a unit (along, radial) pair. */
        private static double[] toward(CoreField core, double at, double along, double radial) {

            final double length = Math.hypot(at - along, radial);
            final double sign = core.polarity() == Polarity.REPEL ? -1 : 1;
            return length < TINY ? new double[] {0, 0} : new double[] {sign * (at - along) / length, -sign * radial / length};
        }
    }
}
