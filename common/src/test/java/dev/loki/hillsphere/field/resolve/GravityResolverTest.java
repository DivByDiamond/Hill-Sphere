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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GravityResolverTest {

    private static final double EPS = 1e-9;
    private static final GravityResolver R = new GravityResolver(FieldTuning.DEFAULT);

    private static CoreField core(double x, double y, double z, double radius, double strength, boolean repel) {

        return new CoreField(new Vec3d(x, y, z), radius, strength, repel ? Polarity.REPEL : Polarity.ATTRACT);
    }

    private static double angle(Vec3d a, Vec3d b) {

        return Math.acos(Math.max(-1, Math.min(1, a.dot(b))));
    }

    @Test
    void noCoresMeansVanilla() {

        assertEquals(Gravity.VANILLA, R.resolve(List.of(), new Vec3d(5, 5, 5)));
    }

    @Test
    void outsideTheFieldMeansVanilla() {

        final Gravity g = R.resolve(List.of(core(0, 0, 0, 10, 1, false)), new Vec3d(10, 0, 0));
        assertEquals(Gravity.VANILLA.strength(), g.strength(), EPS);
        assertEquals(-1, g.direction().y(), EPS);
    }

    @Test
    void insideTheFieldDownPointsAtTheCore() {

        final Gravity g = R.resolve(List.of(core(0, 10, 0, 20, 1.5, false)), new Vec3d(0, 0, 0));
        assertEquals(1, g.direction().y(), EPS);
        assertEquals(1.5, g.strength(), EPS);
    }

    @Test
    void repellingCorePushesAway() {

        final Gravity g = R.resolve(List.of(core(0, 10, 0, 20, 1, true)), new Vec3d(0, 0, 0));
        assertEquals(-1, g.direction().y(), EPS);
    }

    @Test
    void fieldFadesIntoVanillaAtTheEdge() {

        final CoreField c = core(0, 0, 0, 10, 1.5, false);
        final Gravity edge = R.resolve(List.of(c), new Vec3d(9.999, 0, 0));
        assertEquals(1.0, edge.strength(), 1e-3);
        assertTrue(angle(edge.direction(), Vec3d.DOWN) < 1e-2);
    }

    @Test
    void stoppedCoreHasNoEffect() {

        assertEquals(Gravity.VANILLA, R.resolve(List.of(core(0, 0, 0, 0, 1, false)), new Vec3d(0, 1, 0)));
    }

    @Test
    void strongerPullWinsFarFromTheBoundary() {

        final List<CoreField> cores = List.of(core(-10, 0, 0, 20, 0.5, false), core(10, 0, 0, 20, 1.5, false));
        final Gravity g = R.resolve(cores, new Vec3d(10, 5, 0));
        assertEquals(-1, g.direction().y(), EPS);
        assertEquals(1.5, g.strength(), EPS);
    }

    @Test
    void directionNeverJumpsAcrossTheBoundaryBetweenTwoCores() {

        final List<CoreField> cores = List.of(core(-10, 0, 0, 20, 1, false), core(10, 0, 0, 20, 1.5, false));
        Gravity previous = null;
        for (double x = -9; x <= 9; x += 0.01) {
            final Gravity g = R.resolve(cores, new Vec3d(x, 5, 0));
            assertEquals(1, g.direction().length(), 1e-6);
            if (previous != null) {
                assertTrue(angle(previous.direction(), g.direction()) < 0.05, "direction jumped at x=" + x);
                assertTrue(Math.abs(previous.strength() - g.strength()) < 0.02, "strength jumped at x=" + x);
            }
            previous = g;
        }
    }

    @Test
    void equalOppositePullsCancelInTheMiddle() {

        final List<CoreField> cores = List.of(core(-10, 0, 0, 30, 1, false), core(10, 0, 0, 30, 1, false));
        assertEquals(0, R.resolve(cores, new Vec3d(0, 0, 0)).strength(), 1e-6);
    }

    @Test
    void strengthStaysContinuousBetweenOppositeCores() {

        final List<CoreField> cores = List.of(core(-10, 0, 0, 30, 1, false), core(10, 0, 0, 30, 1.2, false));
        Gravity previous = null;
        for (double x = -9; x <= 9; x += 0.01) {
            final Gravity g = R.resolve(cores, new Vec3d(x, 0, 0));
            if (previous != null) {
                assertTrue(Math.abs(previous.strength() - g.strength()) < 0.03, "strength jumped at x=" + x);
            }
            previous = g;
        }
    }

    @Test
    void walkingUnderACoreRotatesDownSmoothly() {

        final CoreField c = core(0, 12, 0, 12, 1, false);
        Gravity previous = null;
        for (double y = 12; y >= 0; y -= 0.01) {
            final Gravity g = R.resolve(List.of(c), new Vec3d(0.5, y, 0));
            if (previous != null && y < 11.99) {
                assertTrue(angle(previous.direction(), g.direction()) < 0.05, "direction jumped at y=" + y);
            }
            previous = g;
        }
    }
}
