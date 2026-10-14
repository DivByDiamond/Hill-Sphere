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
import dev.loki.hillsphere.field.shape.Deck;

/**
 * Current state of one core as the resolver sees it.
 *
 * @param center core position
 * @param radius current radius in blocks (0 means the field is off); a deck field uses it only to be on or off
 * @param strength current pull in multiples of vanilla gravity; for {@link Polarity#LEVITATE}
 *     the share of gravity the core cancels (1 cancels it fully)
 * @param polarity what the core does
 * @param deck the box of a deck field, or {@code null} for a sphere around the core
 */
public record CoreField(Vec3d center, double radius, double strength, Polarity polarity, Deck deck) {

    /** A sphere field. */
    public CoreField(Vec3d center, double radius, double strength, Polarity polarity) {

        this(center, radius, strength, polarity, null);
    }

    /** The same core with its field in the given box instead of a sphere. */
    public CoreField withDeck(Deck box) {

        return new CoreField(center, radius, strength, polarity, box);
    }

    /** How far from the core its field can reach at all, fading edge included. */
    public double reach() {

        return deck == null ? radius : Math.max(radius, deck.reachFrom(center));
    }

    /** Pull direction at a point (unit vector), or {@link Vec3d#ZERO} at the exact center of a sphere. */
    Vec3d direction(Vec3d point) {

        final Vec3d toCore = deck == null ? center.sub(point).normalized() : deck.yAxis().scale(-1);
        return polarity == Polarity.REPEL ? toCore.scale(-1) : toCore;
    }

    /** Share of gravity a levitation core cancels in the point, thinner with height; 0 for any other core. */
    public double liftAt(FieldTuning tuning, Vec3d point) {

        return polarity == Polarity.LEVITATE ? strength * fade(tuning, point) * tuning.altitudeFactor(point.y()) : 0;
    }

    /** This core's own pull in the point, in multiples of vanilla gravity; zero for levitation. */
    public Vec3d pullAt(FieldTuning tuning, Vec3d point) {

        return polarity == Polarity.LEVITATE ? Vec3d.ZERO : direction(point).scale(strength * fade(tuning, point));
    }

    /** Falloff factor in the point, 0 when it is outside the field. */
    public double fade(FieldTuning tuning, Vec3d point) {

        if (radius <= 0) {
            return 0;
        }
        return deck == null ? tuning.fade(center.sub(point).length() / radius) : deck.fade(point);
    }
}
