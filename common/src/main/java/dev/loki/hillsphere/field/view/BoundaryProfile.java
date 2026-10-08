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
import dev.loki.hillsphere.field.resolve.CoreField;

import java.util.ArrayList;
import java.util.List;

/**
 * The surface where two overlapping fields pull equally hard. Both fields are spheres, so the surface is round
 * about the line between the two centres; it is described by its profile: for growing distances from that line
 * ({@code radial}), how far along the line from the first core ({@code along}) the two pulls balance.
 */
public final class BoundaryProfile {

    private static final int SCAN_STEPS = 48;
    private static final int BISECT_STEPS = 24;
    private static final double TINY = 1e-6;

    /** One point of the profile, in blocks. */
    public record Point(double along, double radial) {
    }

    private BoundaryProfile() {
    }

    /**
     * The profile at {@code rings + 1} evenly spaced distances from the axis, from the axis outwards. It stops
     * where the pulls no longer balance inside both fields, and is empty when the fields do not overlap.
     */
    public static List<Point> between(FieldTuning tuning, CoreField a, CoreField b, int rings) {

        final double gap = b.center().sub(a.center()).length();
        if (gap < TINY || gap >= a.radius() + b.radius() || rings <= 0) {
            return List.of();
        }
        final Pair pair = new Pair(tuning, a, b, gap);
        final double reach = Math.min(a.radius(), b.radius());
        final List<Point> points = new ArrayList<>();
        for (int i = 0; i <= rings; i++) {
            final double radial = reach * i / rings;
            final double along = pair.balance(radial);
            if (Double.isNaN(along)) {
                break;
            }
            points.add(new Point(along, radial));
        }
        return points;
    }

    /** Two cores placed on an axis: the first at 0, the second at {@code gap}. */
    private record Pair(FieldTuning tuning, CoreField a, CoreField b, double gap) {

        /** Where along the axis the pulls balance at this distance from it, or NaN if nowhere. */
        double balance(double radial) {

            final double from = gap - b.radius();
            final double to = a.radius();
            double low = from;
            double before = difference(low, radial);
            for (int i = 1; i <= SCAN_STEPS; i++) {
                final double high = from + (to - from) * i / SCAN_STEPS;
                final double now = difference(high, radial);
                if (before * now < 0) {
                    return checked(bisect(low, high, radial), radial);
                }
                if (now == 0 && before != 0) {
                    return checked(high, radial);
                }
                low = high;
                before = now;
            }
            return Double.NaN;
        }

        private double bisect(double low, double high, double radial) {

            double l = low;
            double h = high;
            final boolean firstWinsLow = difference(l, radial) > 0;
            for (int i = 0; i < BISECT_STEPS; i++) {
                final double mid = (l + h) / 2;
                if (difference(mid, radial) > 0 == firstWinsLow) {
                    l = mid;
                } else {
                    h = mid;
                }
            }
            return (l + h) / 2;
        }

        /** The root, unless both fields have faded out there and nothing actually balances. */
        private double checked(double root, double radial) {

            return pull(a, Math.hypot(root, radial)) > TINY ? root : Double.NaN;
        }

        /** Positive where the first core pulls harder. */
        private double difference(double along, double radial) {

            return pull(a, Math.hypot(along, radial)) - pull(b, Math.hypot(gap - along, radial));
        }

        private double pull(CoreField core, double distance) {

            return core.strength() * tuning.fade(distance / core.radius());
        }
    }
}
