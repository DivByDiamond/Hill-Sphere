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
package dev.loki.hillsphere.field.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Vec3dTest {

    private static final double EPS = 1e-9;
    private static final Vec3d X = new Vec3d(1, 0, 0);
    private static final Vec3d Y = new Vec3d(0, 1, 0);

    @Test
    void normalizedOfZeroIsZero() {

        assertEquals(Vec3d.ZERO, Vec3d.ZERO.normalized());
    }

    @Test
    void slerpOfOrthogonalVectorsMeetsInTheMiddle() {

        final Vec3d mid = Vec3d.slerp(X, Y, 0.5);
        assertEquals(Math.sqrt(0.5), mid.x(), EPS);
        assertEquals(Math.sqrt(0.5), mid.y(), EPS);
        assertEquals(1, mid.length(), EPS);
    }

    @Test
    void slerpEndpointsAreExact() {

        assertEquals(X, Vec3d.slerp(X, Y, 0));
        assertEquals(Y, Vec3d.slerp(X, Y, 1));
    }

    @Test
    void slerpOfOppositeVectorsStaysUnitAndPerpendicular() {

        final Vec3d mid = Vec3d.slerp(X, X.scale(-1), 0.5);
        assertEquals(1, mid.length(), EPS);
        assertEquals(0, mid.dot(X), 1e-6);
        assertEquals(mid, Vec3d.slerp(X, X.scale(-1), 0.5));
    }

    @Test
    void slerpOfAlmostEqualVectorsIsUnit() {

        final Vec3d a = new Vec3d(1, 1e-6, 0).normalized();
        assertTrue(Math.abs(Vec3d.slerp(a, X, 0.3).length() - 1) < EPS);
    }
}
