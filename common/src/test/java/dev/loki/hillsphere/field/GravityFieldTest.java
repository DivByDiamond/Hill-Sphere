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
package dev.loki.hillsphere.field;

import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;
import dev.loki.hillsphere.field.resolve.Gravity;
import dev.loki.hillsphere.field.resolve.GravityResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class GravityFieldTest {

    private static final FieldTuning T = FieldTuning.DEFAULT;

    @Test
    void matchesTheResolverOverAllCores() {

        final GravityField<Integer> field = new GravityField<>(T, 16);
        final List<CoreField> cores = List.of(
                new CoreField(new Vec3d(0, 0, 0), 20, 1, Polarity.ATTRACT),
                new CoreField(new Vec3d(30, 4, -6), 25, 1.5, Polarity.ATTRACT),
                new CoreField(new Vec3d(-50, 10, 0), 12, 0.5, Polarity.REPEL));
        for (int i = 0; i < cores.size(); i++) {
            field.put(i, cores.get(i));
        }
        final GravityResolver reference = new GravityResolver(T);
        for (double x = -60; x <= 60; x += 3.7) {
            for (double y = -8; y <= 14; y += 4.1) {
                final Vec3d p = new Vec3d(x, y, 1.3);
                final Gravity expected = reference.resolve(cores, p);
                final Gravity actual = field.gravityAt(p);
                assertEquals(expected.strength(), actual.strength(), 1e-9, "at " + p);
                assertEquals(expected.direction(), actual.direction(), "at " + p);
            }
        }
    }

    @Test
    void liftIgnoresPlanetsAndCountsOnlyLevitators() {

        final GravityField<Integer> field = new GravityField<>(T, 4);
        field.put(1, new CoreField(new Vec3d(0, 10, 0), 20, 1.5, Polarity.ATTRACT));
        assertEquals(0, field.liftAt(new Vec3d(0, 0, 0)), 1e-9);
        field.put(2, new CoreField(new Vec3d(0, 0, 0), 20, 1.0, Polarity.LEVITATE));
        assertEquals(1.0, field.liftAt(new Vec3d(0, 3, 0)), 1e-9);
        assertEquals(0, field.liftAt(new Vec3d(100, 0, 0)), 1e-9);
    }

    @Test
    void planetPartIgnoresLevitation() {

        final GravityField<Integer> field = new GravityField<>(T, 4);
        field.put(1, new CoreField(new Vec3d(0, 10, 0), 20, 1.5, Polarity.ATTRACT));
        field.put(2, new CoreField(new Vec3d(0, 0, 0), 20, 1.0, Polarity.LEVITATE));
        final Gravity planet = field.planetAt(new Vec3d(0, 0, 0));
        assertEquals(1.5, planet.strength(), 1e-9);
        assertEquals(1, planet.direction().y(), 1e-9);
        assertEquals(1.0, field.liftAt(new Vec3d(0, 0, 0)), 1e-9);
        assertEquals(0, field.gravityAt(new Vec3d(0, 0, 0)).strength(), 1e-9);
    }

    @Test
    void emptyWorldIsVanilla() {

        assertEquals(Gravity.VANILLA, new GravityField<Integer>(T, 4).gravityAt(new Vec3d(1, 2, 3)));
    }

    @Test
    void removedCoreNoLongerPulls() {

        final GravityField<Integer> field = new GravityField<>(T, 4);
        field.put(1, new CoreField(new Vec3d(0, 10, 0), 20, 1, Polarity.ATTRACT));
        field.remove(1);
        assertEquals(Gravity.VANILLA, field.gravityAt(new Vec3d(0, 0, 0)));
    }
}
