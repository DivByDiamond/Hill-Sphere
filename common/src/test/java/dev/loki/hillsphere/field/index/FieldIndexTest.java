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
package dev.loki.hillsphere.field.index;

import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FieldIndexTest {

    private static CoreField core(double x, double y, double z, double radius) {

        return new CoreField(new Vec3d(x, y, z), radius, 1, Polarity.ATTRACT);
    }

    @Test
    void findsACoreFromAnyCellItsFieldTouches() {

        final FieldIndex<String> index = new FieldIndex<>(16, 10);
        index.put("a", core(0, 0, 0, 20));
        for (final double x : new double[] {-19, -1, 1, 19}) {
            assertEquals(1, index.candidatesAt(new Vec3d(x, 0, 0)).size(), "x=" + x);
        }
        assertTrue(index.candidatesAt(new Vec3d(200, 0, 0)).isEmpty());
    }

    @Test
    void updatingACoreMovesItInTheIndex() {

        final FieldIndex<String> index = new FieldIndex<>(16, 10);
        index.put("a", core(0, 0, 0, 5));
        index.put("a", core(100, 0, 0, 5));
        assertEquals(1, index.size());
        assertTrue(index.candidatesAt(new Vec3d(0, 0, 0)).isEmpty());
        assertEquals(1, index.candidatesAt(new Vec3d(100, 0, 0)).size());
    }

    @Test
    void removingACoreClearsItsCells() {

        final FieldIndex<String> index = new FieldIndex<>(16, 10);
        index.put("a", core(0, 0, 0, 20));
        index.remove("a");
        assertEquals(0, index.size());
        assertTrue(index.candidatesAt(new Vec3d(0, 0, 0)).isEmpty());
    }

    @Test
    void aStoppedCoreIsDroppedFromTheIndex() {

        final FieldIndex<String> index = new FieldIndex<>(16, 10);
        index.put("a", core(0, 0, 0, 5));
        assertTrue(index.put("a", core(0, 0, 0, 0)));
        assertEquals(0, index.size());
    }

    @Test
    void refusesNewCoresOverTheLimitButStillUpdatesExistingOnes() {

        final FieldIndex<Integer> index = new FieldIndex<>(16, 2);
        assertTrue(index.put(1, core(0, 0, 0, 5)));
        assertTrue(index.put(2, core(50, 0, 0, 5)));
        assertFalse(index.put(3, core(100, 0, 0, 5)));
        assertTrue(index.put(2, core(60, 0, 0, 5)));
        assertEquals(2, index.size());
    }

    @Test
    void rejectsBadConfiguration() {

        assertThrows(IllegalArgumentException.class, () -> new FieldIndex<String>(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new FieldIndex<String>(16, 0));
    }

    @Test
    void handlesNegativeCoordinates() {

        final FieldIndex<String> index = new FieldIndex<>(16, 10);
        index.put("a", core(-100.5, -64, -3, 4));
        assertEquals(1, index.candidatesAt(new Vec3d(-100, -64, -3)).size());
    }
}
