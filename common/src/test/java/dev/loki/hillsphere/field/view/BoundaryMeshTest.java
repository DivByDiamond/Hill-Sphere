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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.loki.hillsphere.field.FieldTuning;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;

import java.util.List;
import org.junit.jupiter.api.Test;

class BoundaryMeshTest {

    private static final FieldTuning T = FieldTuning.DEFAULT;
    private static final CoreField A = new CoreField(Vec3d.ZERO, 16, 1, Polarity.ATTRACT);
    private static final CoreField B = new CoreField(new Vec3d(0, 0, 10), 16, 1, Polarity.ATTRACT);

    @Test
    void theSeamOfTwoCoresIsAPlaneHalfwayBetweenThem() {

        final List<BoundaryMesh.Line> lines = BoundaryMesh.of(T, A, B, List.of(A, B), 8, 6, 24);
        assertFalse(lines.isEmpty());
        for (final BoundaryMesh.Line line : lines) {
            assertTrue(Math.abs(line.from().z() - 5) < 1e-6 && Math.abs(line.to().z() - 5) < 1e-6, line.toString());
        }
        assertTrue(lines.stream().anyMatch(l -> l.fromWeight() > 0.5));
    }

    @Test
    void aThirdCoreThatPullsHarderHidesThePartItOwns() {

        final CoreField strong = new CoreField(new Vec3d(8, 0, 5), 12, 3, Polarity.ATTRACT);
        final List<BoundaryMesh.Line> lines = BoundaryMesh.of(T, A, B, List.of(A, B, strong), 8, 6, 24);
        for (final BoundaryMesh.Line line : lines) {
            if (line.from().sub(strong.center()).length() < 6) {
                assertTrue(line.fromWeight() == 0, "drawn where the third core wins: " + line);
            }
        }
        assertTrue(lines.stream().anyMatch(l -> l.from().x() < -3 && l.fromWeight() > 0.3), "the far side stays");
    }
}
