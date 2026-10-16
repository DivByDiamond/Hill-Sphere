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
import dev.loki.hillsphere.field.resolve.GravityResolver;

import java.util.Collection;

/**
 * What a point looks like through the goggles: the polarity of the core that pulls hardest there, how deep
 * inside a field the point lies (1 on a plateau, falling to 0 at the edge) and how close it is to the seam
 * between two pulling cores ({@code seam}: 1 where the two strongest pull equally hard, 0 where one clearly wins).
 * For drawing only, physics uses {@link dev.loki.hillsphere.field.GravityField}.
 */
public record FieldProbe(Polarity polarity, double influence, double seam) {

    /** A point no field reaches. */
    public static final FieldProbe NONE = new FieldProbe(Polarity.ATTRACT, 0, 0);

    public boolean inField() {

        return influence > 0;
    }

    public static FieldProbe at(FieldTuning tuning, Collection<CoreField> cores, Vec3d point) {

        Polarity strongest = null;
        double strongestPull = 0;
        double influence = 0;
        double best = 0;
        double second = 0;
        for (final CoreField core : cores) {
            final double fade = fade(tuning, core, point);
            final double pull = fade * core.strength();
            influence = Math.max(influence, fade);
            if (pull > strongestPull) {
                strongestPull = pull;
                strongest = core.polarity();
            }
            if (core.polarity() != Polarity.LEVITATE) {
                second = Math.max(second, Math.min(best, pull));
                best = Math.max(best, pull);
            }
        }
        return strongest == null ? NONE : new FieldProbe(strongest, influence, GravityResolver.share(tuning, best, second));
    }

    /** The core's falloff in the point: 1 on the plateau, 0 outside. */
    public static double fade(FieldTuning tuning, CoreField core, Vec3d point) {

        return core.fade(tuning, point);
    }
}
