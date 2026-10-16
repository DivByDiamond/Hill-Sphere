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
package dev.loki.hillsphere.field.particle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.Gravity;

import org.junit.jupiter.api.Test;

class RainTiltTest {

    private static final double EPS = 1e-9;

    @Test
    void worldDownAndNoMixKeepRainVertical() {

        assertEquals(0, RainTilt.slope(new Gravity(Vec3d.DOWN, 1), 1).x(), EPS);
        assertEquals(0, RainTilt.slope(new Gravity(new Vec3d(1, 0, 0), 1), 0).x(), EPS);
    }

    @Test
    void aFallTowardTheEastLeansTheStripsEast() {

        final Vec3d slope = RainTilt.slope(new Gravity(new Vec3d(1, -1, 0).normalized(), 1), 1);
        assertEquals(-1, slope.x(), EPS);
        assertEquals(0, slope.z(), EPS);
    }

    @Test
    void aSidewaysFallLeansNoMoreThanTheLimit() {

        final Vec3d slope = RainTilt.slope(new Gravity(new Vec3d(1, 0, 0), 1), 1);
        assertEquals(-1 / RainTilt.MIN_DOWN, slope.x(), EPS);
        assertTrue(Math.abs(slope.z()) < EPS);
    }

    @Test
    void aPushingFieldLeansTheStripsTheSameWayAsThePullingOne() {

        final Vec3d toward = RainTilt.slope(new Gravity(new Vec3d(1, -1, 0).normalized(), 1), 1);
        final Vec3d away = RainTilt.slope(new Gravity(new Vec3d(1, -1, 0).normalized(), -1), 1);
        assertEquals(toward.x(), away.x(), EPS);
    }
}
