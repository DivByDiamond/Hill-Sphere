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
package dev.loki.hillsphere.field.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.loki.hillsphere.field.math.Vec3d;

import org.junit.jupiter.api.Test;

class AirControlTest {

    private static final double EPS = 1e-9;
    private static final AirControl AIR = new AirControl(0.1, 0.25, 0.2);

    @Test
    void thrustGrowsFromTheMiddleToTheEdge() {

        assertEquals(0.025, AIR.thrustAt(1), EPS);
        assertEquals(0.1, AIR.thrustAt(0), EPS);
        assertTrue(AIR.thrustAt(0.5) > AIR.thrustAt(0.9));
    }

    @Test
    void influenceOutsideZeroToOneIsClamped() {

        assertEquals(AIR.thrustAt(0), AIR.thrustAt(-3), EPS);
        assertEquals(AIR.thrustAt(1), AIR.thrustAt(3), EPS);
    }

    private static final Vec3d UP = new Vec3d(0, 1, 0);
    private static final Vec3d SOUTH = new Vec3d(0, 0, 1);

    @Test
    void forwardPushesAlongTheLook() {

        final Vec3d look = new Vec3d(0, 0.6, 0.8);
        final Vec3d t = AIR.thrust(0, 0, 1, look, UP, 0);
        assertEquals(0, t.x(), EPS);
        assertEquals(0.06, t.y(), EPS);
        assertEquals(0.08, t.z(), EPS);
    }

    @Test
    void strafeLeftIsLeftOfTheLook() {

        final Vec3d t = AIR.thrust(1, 0, 0, SOUTH, UP, 0);
        assertEquals(0.1, t.x(), EPS);
        assertEquals(0, t.z(), EPS);
    }

    @Test
    void onACeilingLeftAndRightSwap() {

        assertEquals(-0.1, AIR.thrust(1, 0, 0, SOUTH, Vec3d.DOWN, 0).x(), EPS);
    }

    @Test
    void aDiagonalIsNoStrongerThanStraight() {

        assertEquals(0.1, AIR.thrust(1, 0, 1, SOUTH, UP, 0).length(), EPS);
    }

    @Test
    void verticalKeysPushAlongTheFramesUp() {

        assertEquals(0.1, AIR.thrust(0, 1, 0, SOUTH, UP, 0).y(), EPS);
        assertEquals(-0.1, AIR.thrust(0, 1, 0, SOUTH, Vec3d.DOWN, 0).y(), EPS);
        assertEquals(Vec3d.ZERO, AIR.thrust(0, 0, 0, SOUTH, UP, 0));
    }

    @Test
    void dragIsStrongestInTheMiddleAndGoneAtTheEdge() {

        assertEquals(0.8, AIR.keptSpeed(1), EPS);
        assertEquals(1, AIR.keptSpeed(0), EPS);
    }

    @Test
    void valuesOutOfRangeAreRejected() {

        assertThrows(IllegalArgumentException.class, () -> new AirControl(-1, 0.5, 0.1));
        assertThrows(IllegalArgumentException.class, () -> new AirControl(0.1, 1.5, 0.1));
        assertThrows(IllegalArgumentException.class, () -> new AirControl(0.1, 0.5, 2));
    }
}
