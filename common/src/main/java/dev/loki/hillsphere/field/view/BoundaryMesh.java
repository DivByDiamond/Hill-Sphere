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
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The seam of two fields in the world, ready to draw: rings round the line between the cores and ribs along the
 * seam's outline. Each end of a line has a weight from the {@link BoundaryProfile}, and 0 where some other core pulls
 * harder than the two, because the resolver then follows that core and the seam is not there.
 */
public final class BoundaryMesh {

    /** A line of the seam with the weight (0..1) of each end. */
    public record Line(Vec3d from, Vec3d to, double fromWeight, double toWeight) {
    }

    private BoundaryMesh() {
    }

    /**
     * The seam of {@code a} and {@code b} among {@code all} cores, traced {@code rows} cells high, with {@code ribs}
     * ribs and rings of {@code segments} pieces.
     */
    public static List<Line> of(FieldTuning tuning, CoreField a, CoreField b, Collection<CoreField> all, int rows, int ribs, int segments) {

        final BoundaryProfile.Profile profile = BoundaryProfile.between(tuning, a, b, rows);
        if (profile.isEmpty()) {
            return List.of();
        }
        final Seam seam = new Seam(tuning, a, b, all);
        final List<Line> lines = new ArrayList<>();
        for (final BoundaryProfile.Point p : profile.rings()) {
            if (p.radial() > 0) {
                for (int s = 0; s < segments; s++) {
                    lines.add(seam.line(p, p, 2 * Math.PI * s / segments, 2 * Math.PI * (s + 1) / segments));
                }
            }
        }
        for (final BoundaryProfile.Segment piece : profile.segments()) {
            for (int r = 0; r < ribs; r++) {
                final double angle = 2 * Math.PI * r / ribs;
                lines.add(seam.line(piece.from(), piece.to(), angle, angle));
            }
        }
        return lines;
    }

    /** Carries profile points into the world and weighs them. */
    private record Seam(FieldTuning tuning, CoreField a, CoreField b, Collection<CoreField> all, Vec3d axis, Vec3d u, Vec3d v) {

        Seam(FieldTuning tuning, CoreField a, CoreField b, Collection<CoreField> all) {

            this(tuning, a, b, all, b.center().sub(a.center()).normalized());
        }

        private Seam(FieldTuning tuning, CoreField a, CoreField b, Collection<CoreField> all, Vec3d axis) {

            this(tuning, a, b, all, axis, across(axis), axis.cross(across(axis)).normalized());
        }

        Line line(BoundaryProfile.Point from, BoundaryProfile.Point to, double fromAngle, double toAngle) {

            final Vec3d start = place(from, fromAngle);
            final Vec3d end = place(to, toAngle);
            return new Line(start, end, weigh(from, start), weigh(to, end));
        }

        private Vec3d place(BoundaryProfile.Point p, double angle) {

            return a.center().add(axis.scale(p.along())).add(u.scale(p.radial() * Math.cos(angle))).add(v.scale(p.radial() * Math.sin(angle)));
        }

        /** The point's weight, or 0 where a third core pulls harder than the two. */
        private double weigh(BoundaryProfile.Point p, Vec3d at) {

            final double pull = Math.max(pull(a, at), pull(b, at));
            for (final CoreField other : all) {
                if (other != a && other != b && other.polarity() != Polarity.LEVITATE && pull(other, at) > pull) {
                    return 0;
                }
            }
            return p.weight();
        }

        private double pull(CoreField core, Vec3d at) {

            return core.strength() * core.fade(tuning, at);
        }

        private static Vec3d across(Vec3d axis) {

            final Vec3d helper = Math.abs(axis.y()) < 0.9 ? new Vec3d(0, 1, 0) : new Vec3d(1, 0, 0);
            return axis.cross(helper).normalized();
        }
    }
}
