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
package dev.loki.hillsphere.field.resolve;

import dev.loki.hillsphere.field.FieldTuning;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.math.Vec3d;

import java.util.Collection;

/**
 * Picks the gravity at a point from all active cores.
 *
 * <p>Levitation cores do not take part in the choice: they only weaken the result.
 * The core with the largest pull in the point wins. Near the boundary where two cores
 * are about equal the two directions are blended so "down" never jumps. Outside every
 * field the result is vanilla gravity, and at the edge of a field it fades into it.
 */
public final class GravityResolver {

    private final FieldTuning tuning;

    public GravityResolver(FieldTuning tuning) {

        this.tuning = tuning;
    }

    public Gravity resolve(Collection<CoreField> cores, Vec3d point) {

        return planetAt(cores, point).withLift(liftAt(cores, point));
    }

    /** Total share of gravity cancelled by levitation cores in the point; they add up. */
    private double liftAt(Collection<CoreField> cores, Vec3d point) {

        double lift = 0;
        for (final CoreField core : cores) {
            if (core.polarity() == Polarity.LEVITATE) {
                lift += core.strength() * core.fade(tuning, point);
            }
        }
        return lift;
    }

    private Gravity planetAt(Collection<CoreField> cores, Vec3d point) {

        Candidate best = null;
        Candidate second = null;
        for (final CoreField core : cores) {
            if (core.polarity() == Polarity.LEVITATE) {
                continue;
            }
            final Candidate c = candidate(core, point);
            if (c == null) {
                continue;
            }
            if (best == null || c.pull > best.pull) {
                second = best;
                best = c;
            } else if (second == null || c.pull > second.pull) {
                second = c;
            }
        }
        if (best == null) {
            return Gravity.VANILLA;
        }
        if (second == null) {
            return toward(best.direction, best.strength, best.fade);
        }

        final double ratio = best.pull / second.pull;
        final double w = 0.5 + 0.5 * smoothstep(tuning.blendStart(), tuning.blendEnd(), ratio);
        final Vec3d direction = Vec3d.slerp(second.direction, best.direction, w);
        double strength = lerp(second.strength, best.strength, w);
        if (best.direction.dot(second.direction) < -0.9995) {
            // opposite equal pulls cancel in the middle instead of picking an arbitrary side
            strength *= Math.abs(2 * w - 1);
        }
        return toward(direction, strength, lerp(second.fade, best.fade, w));
    }

    private Candidate candidate(CoreField core, Vec3d point) {

        final double fade = core.fade(tuning, point);
        final Vec3d direction = core.direction(point);
        final double pull = core.strength() * fade;
        if (pull <= 0 || direction.equals(Vec3d.ZERO)) {
            return null;
        }
        return new Candidate(direction, core.strength(), fade, pull);
    }

    /** Blend vanilla gravity into the core's pull by how deep inside the field the point is. */
    private static Gravity toward(Vec3d direction, double strength, double fade) {

        return new Gravity(Vec3d.slerp(Vec3d.DOWN, direction, fade), lerp(1, strength, fade));
    }

    private static double lerp(double a, double b, double t) {

        return a + (b - a) * t;
    }

    private static double smoothstep(double edge0, double edge1, double x) {

        final double t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }

    private record Candidate(Vec3d direction, double strength, double fade, double pull) {
    }
}
