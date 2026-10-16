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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Picks the gravity at a point from all active cores.
 *
 * <p>Levitation cores do not take part in the choice: they only weaken the result.
 * The core with the largest pull in the point wins. Near a boundary where other cores pull about as hard,
 * their pulls are averaged in as vectors with weights that fade to zero as the winner pulls away, so "down"
 * never jumps and two opposing pulls cancel smoothly instead of turning to some side. Outside every field the
 * result is vanilla gravity, and at the edge of a field it fades into it. The formulas are in {@code docs/FIELDS.md}.
 */
public final class GravityResolver {

    /** Below this the averaged pulls are taken to cancel exactly. */
    private static final double CANCELLED = 1e-12;

    private final FieldTuning tuning;

    public GravityResolver(FieldTuning tuning) {

        this.tuning = tuning;
    }

    public Gravity resolve(Collection<CoreField> cores, Vec3d point) {

        return planetAt(cores, point).withLift(liftAt(cores, point));
    }

    /** Total share of gravity cancelled by levitation cores in the point; they add up. */
    public double liftAt(Collection<CoreField> cores, Vec3d point) {

        double lift = 0;
        for (final CoreField core : cores) {
            lift += core.liftAt(tuning, point);
        }
        return lift;
    }

    /**
     * How much a core pulling {@code pull} takes part next to the winner pulling {@code best}: 1 when they pull
     * equally hard, falling to 0 where the winner is {@code blendEnd} times stronger. With two cores this gives the
     * winner the weight {@code 0.5 + 0.5 * smoothstep(blendStart, blendEnd, best / pull)}.
     */
    public static double share(FieldTuning tuning, double best, double pull) {

        if (pull <= 0) {
            return 0;
        }
        final double s = smoothstep(tuning.blendStart(), tuning.blendEnd(), best / pull);
        return (1 - s) / (1 + s);
    }

    /** Gravity from attracting and repelling cores only, without levitation. */
    public Gravity planetAt(Collection<CoreField> cores, Vec3d point) {

        final List<Candidate> found = new ArrayList<>();
        Candidate best = null;
        for (final CoreField core : cores) {
            final Candidate c = core.polarity() == Polarity.LEVITATE ? null : candidate(core, point);
            if (c != null) {
                found.add(c);
                best = best == null || c.pull > best.pull ? c : best;
            }
        }
        if (best == null) {
            return Gravity.VANILLA;
        }
        Vec3d sum = Vec3d.ZERO;
        double weights = 0;
        double strength = 0;
        double fade = 0;
        for (final Candidate c : found) {
            final double q = share(tuning, best.pull, c.pull);
            sum = sum.add(c.direction.scale(q));
            weights += q;
            strength += q * c.strength;
            fade += q * c.fade;
        }
        final Vec3d mean = sum.scale(1 / weights);
        final double agree = mean.length();
        final Gravity full = toward(agree < CANCELLED ? best.direction : mean.scale(1 / agree), strength / weights, fade / weights);
        return agree > 1 - CANCELLED ? full : cancelled(full, fade / weights, agree);
    }

    /**
     * Pulls that partly cancel: the field's part of gravity shrinks with {@code agree} (the length of the mean pull
     * direction) as a vector, so it passes through zero and only the vanilla share of the edge is left there.
     */
    private static Gravity cancelled(Gravity full, double fade, double agree) {

        final Vec3d v = Vec3d.DOWN.scale((1 - fade) * (1 - agree)).add(full.direction().scale(full.strength() * agree));
        final double length = v.length();
        return new Gravity(length < CANCELLED ? full.direction() : v.scale(1 / length), length);
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
