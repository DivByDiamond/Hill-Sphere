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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.loki.hillsphere.field.FieldTuning;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.math.Vec3d;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Two or more cores whose fields overlap: what a body feels between and around them (see docs/FIELDS.md). */
class MixedFieldsTest {

    private static final GravityResolver R = new GravityResolver(FieldTuning.DEFAULT);
    private static final List<CoreField> TWO_PULLS = List.of(core(0, 0, 1, Polarity.ATTRACT), core(10, 0, 1, Polarity.ATTRACT));
    private static final List<CoreField> PULL_PUSH = List.of(core(0, 0, 1, Polarity.ATTRACT), core(10, 0, 1, Polarity.REPEL));
    private static final List<CoreField> STRONG_PULL = List.of(core(0, 0, 2, Polarity.ATTRACT), core(10, 0, 1, Polarity.REPEL));

    private static CoreField core(double x, double y, double strength, Polarity polarity) {

        return new CoreField(new Vec3d(x, y, 0), 16, strength, polarity);
    }

    private static Gravity at(List<CoreField> cores, double x, double y) {

        return R.planetAt(cores, new Vec3d(x, y, 0));
    }

    private static double angle(Vec3d a, Vec3d b) {

        return Math.acos(Math.max(-1, Math.min(1, a.dot(b))));
    }

    @Test
    void equalAttractorsCancelAllAlongTheAxisBetweenTheirPlateaus() {

        for (double x = 1; x <= 9; x += 0.5) {
            assertEquals(0, at(TWO_PULLS, x, 0).strength(), 1e-9, "x=" + x);
        }
    }

    @Test
    void weightlessnessBetweenTwoAttractorsFadesInFromTheAxis() {

        Gravity previous = null;
        for (double y = -1; y <= 1; y += 0.01) {
            final Gravity g = at(TWO_PULLS, 5, y);
            if (previous != null) {
                assertTrue(Math.abs(g.strength() - previous.strength()) < 0.05, "strength jumped at y=" + y);
            }
            previous = g;
        }
        assertEquals(0.02, at(TWO_PULLS, 5, 0.1).strength(), 1e-3);
    }

    @Test
    void offTheAxisTwoEqualAttractorsGiveTheAverageOfTheirPulls() {

        final Gravity g = at(TWO_PULLS, 5, 3);
        assertEquals(-1, g.direction().y(), 1e-9);
        assertEquals(3 / Math.sqrt(34), g.strength(), 1e-9);
    }

    @Test
    void attractorAndRepellerPushTheSameWayBetweenThem() {

        for (double x = 1; x <= 9; x += 0.5) {
            final Gravity g = at(PULL_PUSH, x, 0);
            assertEquals(-1, g.direction().x(), 1e-9, "x=" + x);
            assertEquals(1, g.strength(), 1e-9, "x=" + x);
        }
        assertEquals(2, at(STRONG_PULL, 5, 3).strength(), 1e-9);
    }

    @Test
    void opposingPullsOnTheAxisNeverTurnSideways() {

        for (final List<CoreField> cores : List.of(PULL_PUSH, STRONG_PULL)) {
            for (double x = 10.1; x <= 18; x += 0.05) {
                final Gravity g = at(cores, x, 0);
                assertEquals(0, g.direction().z(), 1e-9, "x=" + x);
            }
        }
    }

    @Test
    void aWeakerRepellerHasANeutralPointBehindIt() {

        double lowest = Double.MAX_VALUE;
        double where = 0;
        Gravity previous = null;
        for (double x = 10.5; x <= 16; x += 0.01) {
            final Gravity g = at(STRONG_PULL, x, 0);
            if (g.strength() < lowest) {
                lowest = g.strength();
                where = x;
            }
            if (previous != null) {
                assertTrue(angle(g.direction(), previous.direction()) < 0.1, "turned at x=" + x);
            }
            previous = g;
        }
        final Gravity neutral = at(STRONG_PULL, where, 0);
        assertTrue(where > 12 && where < 14.5, "at " + where);
        assertTrue(lowest < 0.4, "lowest " + lowest);
        assertEquals(-1, neutral.direction().y(), 0.05, "only the vanilla share is left");
        assertTrue(at(STRONG_PULL, where - 0.5, 0).direction().x() < 0);
        assertTrue(at(STRONG_PULL, where + 0.5, 0).direction().x() > 0);
    }

    @Test
    void threeCoresNeverJump() {

        final List<CoreField> cores = List.of(new CoreField(new Vec3d(0, 0, 0), 20, 1, Polarity.ATTRACT),
                new CoreField(new Vec3d(10, 0, 0), 20, 1.1, Polarity.ATTRACT), new CoreField(new Vec3d(5, 9, 0), 20, 0.9, Polarity.REPEL));
        for (double y = 1; y <= 7; y += 1.5) {
            Gravity previous = null;
            for (double x = -6; x <= 16; x += 0.01) {
                final Gravity g = at(cores, x, y);
                if (previous != null) {
                    assertTrue(Math.abs(g.strength() - previous.strength()) < 0.05, "strength jumped at " + x + "," + y);
                    assertTrue(g.strength() < 0.05 || angle(g.direction(), previous.direction()) < 0.1, "turned at " + x + "," + y);
                }
                previous = g;
            }
        }
    }
}
