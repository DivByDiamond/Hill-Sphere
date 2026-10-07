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

import dev.loki.hillsphere.field.FieldTuning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RampTest {

    @Test
    void movesByAtMostOneStep() {

        assertEquals(1, Ramp.towards(0, 10, 1), 1e-12);
        assertEquals(9, Ramp.towards(10, 0, 1), 1e-12);
    }

    @Test
    void landsExactlyOnTheTarget() {

        assertEquals(5, Ramp.towards(4.5, 5, 1), 1e-12);
        assertEquals(5, Ramp.towards(5, 5, 1), 1e-12);
    }

    @Test
    void fullRampTakesRampTicks() {

        final FieldTuning t = FieldTuning.DEFAULT;
        double radius = 0;
        int ticks = 0;
        while (radius < t.maxRadius()) {
            radius = Ramp.towards(radius, t.maxRadius(), t.radiusStep());
            ticks++;
        }
        // float accumulation may cost one extra tick
        assertTrue(ticks >= t.rampTicks() && ticks <= t.rampTicks() + 1, "took " + ticks);
    }
}
