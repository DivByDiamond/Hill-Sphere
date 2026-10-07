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
package dev.loki.hillsphere.field.sync;

import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FieldChangesTest {

    private static CoreField core(double radius) {

        return new CoreField(Vec3d.ZERO, radius, 1, Polarity.ATTRACT);
    }

    @Test
    void reportsANewCoreOnce() {

        final FieldChanges<String> c = new FieldChanges<>();
        c.put("a", core(5));
        assertEquals(1, c.drain().size());
        assertTrue(c.drain().isEmpty());
    }

    @Test
    void doesNotReportAnUnchangedCore() {

        final FieldChanges<String> c = new FieldChanges<>();
        c.put("a", core(5));
        c.drain();
        c.put("a", core(5));
        assertTrue(c.drain().isEmpty());
    }

    @Test
    void reportsAChangedCore() {

        final FieldChanges<String> c = new FieldChanges<>();
        c.put("a", core(5));
        c.drain();
        c.put("a", core(6));
        assertEquals(6, c.drain().get("a").radius());
    }

    @Test
    void reportsARemovalAsZeroRadius() {

        final FieldChanges<String> c = new FieldChanges<>();
        c.put("a", core(5));
        c.drain();
        c.remove("a");
        assertEquals(0, c.drain().get("a").radius());
        assertTrue(c.snapshot().isEmpty());
    }

    @Test
    void aCoreThatStoppedCountsAsRemoved() {

        final FieldChanges<String> c = new FieldChanges<>();
        c.put("a", core(5));
        c.drain();
        c.put("a", core(0));
        assertEquals(0, c.drain().get("a").radius());
    }

    @Test
    void removingAnUnknownCoreSendsNothing() {

        final FieldChanges<String> c = new FieldChanges<>();
        c.remove("nope");
        assertTrue(c.drain().isEmpty());
    }

    @Test
    void snapshotHoldsOnlyActiveCores() {

        final FieldChanges<String> c = new FieldChanges<>();
        c.put("a", core(5));
        c.put("b", core(7));
        c.remove("a");
        assertEquals(1, c.snapshot().size());
        assertTrue(c.snapshot().containsKey("b"));
    }
}
