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
package dev.loki.hillsphere.field.body;

import dev.loki.hillsphere.field.FieldTuning;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.body.Push.Kind;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * The forces cores put on rigid bodies such as ships in one physics step.
 *
 * <p>A body already falls with its own gravity, so the field adds to it instead of replacing it.
 * Levitation cancels a share of the weight, never more than the core can carry and never more than all of it: a core mounted on the body lifts it by its full level,
 * any other levitation core by its level faded at the centre of mass. An attracting or repelling
 * core pulls the centre of mass with its own pull there, times the body's weight. A core never
 * pulls the body it is mounted on, and a core mounted on another body gets the opposite force at
 * the core, so pulls between bodies always cancel out.
 */
public final class BodyForces {

    private final FieldTuning tuning;

    public BodyForces(FieldTuning tuning) {

        this.tuning = tuning;
    }

    /**
     * The forces of all cores on all bodies for one step.
     *
     * @param pullScale multiplier for pulls, like the dimension's scale; levitation ignores it
     * @return every force of this step; nothing for bodies the field does not reach
     */
    public <B> List<Push<B>> of(Collection<Body<B>> bodies, Collection<Mounted<B>> cores, double pullScale) {

        final List<Push<B>> pushes = new ArrayList<>();
        for (final Body<B> body : bodies) {
            double lift = 0;
            for (final Mounted<B> mounted : cores) {
                if (Objects.equals(body.id(), mounted.carrier())) {
                    lift += held(ownLift(mounted.core(), body.center().y()), mounted.core(), body);
                } else {
                    lift += held(mounted.core().liftAt(tuning, body.center()), mounted.core(), body);
                    pull(body, mounted, pullScale, pushes);
                }
            }
            lift = Math.min(1, lift);
            if (lift != 0) {
                pushes.add(new Push<>(body.id(), body.center(), body.gravity().scale(-body.mass() * lift), Kind.LIFT));
            }
        }
        return pushes;
    }

    /**
     * A core only holds up what its field is big enough for: a share of the weight no larger than the mass it can carry
     * (per block of radius) over the body's mass. A heavy ship needs a fast core, or several; a light one is held fully.
     */
    private <B> double held(double lift, CoreField core, Body<B> body) {

        final double carry = tuning.liftMassPerRadius();
        if (lift <= 0 || carry <= 0 || body.mass() <= 0) {
            return lift;
        }
        return Math.min(lift, carry * core.radius() / body.mass());
    }

    /** A levitation core sits in its own field, so it lifts what carries it by its whole level, as far as the air allows. */
    private double ownLift(CoreField core, double height) {

        return core.polarity() == Polarity.LEVITATE && core.radius() > 0 ? core.strength() * tuning.altitudeFactor(height) : 0;
    }

    private <B> void pull(Body<B> body, Mounted<B> mounted, double scale, List<Push<B>> pushes) {

        final Vec3d accel = mounted.core().pullAt(tuning, body.center());
        if (accel.length() < 1e-12) {
            return;
        }
        final Vec3d force = accel.scale(scale * body.mass() * body.gravity().length());
        pushes.add(new Push<>(body.id(), body.center(), force, Kind.PULL));
        if (mounted.carrier() != null) {
            pushes.add(new Push<>(mounted.carrier(), mounted.core().center(), force.scale(-1), Kind.PULL));
        }
    }
}
