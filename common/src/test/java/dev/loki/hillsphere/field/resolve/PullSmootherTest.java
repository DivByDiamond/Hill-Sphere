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

import dev.loki.hillsphere.field.math.Vec3d;
import org.junit.jupiter.api.Test;

class PullSmootherTest {

    private static final Gravity UP = new Gravity(new Vec3d(0, 1, 0), 2);

    @Test
    void vanillaStaysVanilla() {

        final PullSmoother smoother = new PullSmoother();
        assertEquals(Gravity.VANILLA, smoother.next(Gravity.VANILLA));
    }

    @Test
    void strengthFadesInInsteadOfJumping() {

        final PullSmoother smoother = new PullSmoother();
        smoother.next(Gravity.VANILLA);
        final Gravity first = smoother.next(new Gravity(Vec3d.DOWN, 3));
        assertTrue(first.strength() > 1 && first.strength() < 3);
    }

    @Test
    void settlesExactlyOnTheField() {

        final PullSmoother smoother = new PullSmoother();
        smoother.next(Gravity.VANILLA);
        Gravity g = Gravity.VANILLA;
        for (int i = 0; i < 200; i++) {
            g = smoother.next(UP);
        }
        assertEquals(UP, g);
    }

    @Test
    void turningAroundPassesThroughWeightlessness() {

        final PullSmoother smoother = new PullSmoother();
        smoother.next(Gravity.VANILLA);
        double least = Double.MAX_VALUE;
        for (int i = 0; i < 200; i++) {
            least = Math.min(least, smoother.next(UP).strength());
        }
        assertTrue(least < 0.2, "the pull passes near zero on the way from down to up: " + least);
    }

    @Test
    void firstPullIsTakenAsItIs() {

        assertEquals(UP, new PullSmoother().next(UP));
    }

    @Test
    void directionNeverGoesNaNAtZero() {

        final PullSmoother smoother = new PullSmoother();
        for (int i = 0; i < 300; i++) {
            final Gravity g = smoother.next(i % 2 == 0 ? UP : new Gravity(Vec3d.DOWN, 2));
            assertEquals(1.0, g.direction().length(), 1e-9);
        }
    }
}
