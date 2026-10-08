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
package dev.loki.hillsphere.field.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.loki.hillsphere.field.FieldTuning;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;

import java.util.List;
import org.junit.jupiter.api.Test;

class BoundaryProfileTest {

    private static final FieldTuning T = FieldTuning.DEFAULT;
    private static final CoreField A = new CoreField(Vec3d.ZERO, 8, 1, Polarity.ATTRACT);

    @Test
    void equalFieldsSplitHalfway() {

        final CoreField b = new CoreField(new Vec3d(10, 0, 0), 8, 1, Polarity.REPEL);
        final List<BoundaryProfile.Point> points = BoundaryProfile.between(T, A, b, 6);
        assertFalse(points.isEmpty());
        assertEquals(0, points.get(0).radial(), 1e-9);
        points.forEach(p -> assertEquals(5, p.along(), 1e-4));
    }

    @Test
    void strongerFieldPushesTheBoundaryAway() {

        final CoreField b = new CoreField(new Vec3d(10, 0, 0), 8, 2, Polarity.ATTRACT);
        assertTrue(BoundaryProfile.between(T, A, b, 4).get(0).along() < 5);
    }

    @Test
    void fieldsThatDoNotTouchHaveNoBoundary() {

        final CoreField b = new CoreField(new Vec3d(20, 0, 0), 8, 1, Polarity.ATTRACT);
        assertTrue(BoundaryProfile.between(T, A, b, 4).isEmpty());
    }
}
