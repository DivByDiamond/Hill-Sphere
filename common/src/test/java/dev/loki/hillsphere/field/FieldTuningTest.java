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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FieldTuningTest {

    private static final FieldTuning T = FieldTuning.DEFAULT;
    private static final double EPS = 1e-9;

    @Test
    void levitationLevelsAreClamped() {

        assertEquals(0.25, T.levitation(0), EPS);
        assertEquals(1.0, T.levitation(3), EPS);
        assertEquals(1.25, T.levitation(99), EPS);
    }

    @Test
    void rejectsImpossibleTuning() {

        final java.util.List<Double> l = java.util.List.of(1.0);
        assertThrows(IllegalArgumentException.class, () -> new FieldTuning(8, 256, 2, 32, 1.0, l, l, 4, 1.0, 1.25, 30));
        assertThrows(IllegalArgumentException.class, () -> new FieldTuning(8, 256, 2, 32, 0.6, l, l, 4, 1.25, 1.0, 30));
        assertThrows(IllegalArgumentException.class, () -> new FieldTuning(8, 256, 40, 32, 0.6, l, l, 4, 1.0, 1.25, 30));
        assertThrows(IllegalArgumentException.class, () -> new FieldTuning(8, 256, 2, 32, 0.6, java.util.List.of(), l, 4, 1.0, 1.25, 30));
    }

    @Test
    void fieldIsOffBelowMinimumSpeed() {

        assertEquals(0, T.radius(7.9), EPS);
        assertFalse(T.isSpinningFastEnough(7.9));
        assertTrue(T.isSpinningFastEnough(8));
    }

    @Test
    void radiusGrowsWithSpeedUpToTheCap() {

        assertEquals(32, T.radius(256), EPS);
        assertEquals(32, T.radius(512), EPS);
        assertTrue(T.radius(64) < T.radius(128));
        assertEquals(2 + 30 * 64.0 / 256, T.radius(64), EPS);
    }

    @Test
    void directionOfRotationDoesNotMatter() {

        assertEquals(T.radius(100), T.radius(-100), EPS);
        assertEquals(T.stress(100, 2), T.stress(-100, 2), EPS);
    }

    @Test
    void strengthLevelsAreClamped() {

        assertEquals(0.25, T.strength(1), EPS);
        assertEquals(1.5, T.strength(4), EPS);
        assertEquals(0.25, T.strength(0), EPS);
        assertEquals(1.5, T.strength(9), EPS);
    }

    @Test
    void stressScalesWithSpeedAndLevel() {

        assertEquals(4096, T.stress(256, 4), EPS);
        assertEquals(256, T.stress(64, 1), EPS);
    }

    @Test
    void fadeIsFullOnThePlateauAndZeroAtTheEdge() {

        assertEquals(1, T.fade(0), EPS);
        assertEquals(1, T.fade(0.6), EPS);
        assertEquals(0.5, T.fade(0.8), EPS);
        assertEquals(0, T.fade(1), EPS);
        assertEquals(0, T.fade(3), EPS);
    }

    @Test
    void fadeNeverIncreasesWithDistance() {

        double previous = 1;
        for (double x = 0; x <= 1.2; x += 0.01) {
            final double f = T.fade(x);
            assertTrue(f <= previous + EPS, "fade rose at " + x);
            previous = f;
        }
    }
}
