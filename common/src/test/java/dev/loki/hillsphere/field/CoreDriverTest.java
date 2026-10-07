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
package dev.loki.hillsphere.field;

import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CoreDriverTest {

    private static final FieldTuning T = FieldTuning.DEFAULT;
    private static final Vec3d AT = Vec3d.ZERO;

    private static CoreField run(CoreDriver d, int ticks, Polarity p, int level, double rpm, boolean enabled) {

        CoreField last = null;
        for (int i = 0; i < ticks; i++) {
            last = d.step(T, AT, p, level, rpm, enabled);
        }
        return last;
    }

    @Test
    void startsOff() {

        assertFalse(new CoreDriver().isActive());
    }

    @Test
    void growsToTheTargetInRampTicks() {

        final CoreDriver d = new CoreDriver();
        final CoreField f = run(d, T.rampTicks() + 1, Polarity.ATTRACT, 3, 256, true);
        assertEquals(32, f.radius(), 1e-9);
        assertEquals(1.0, f.strength(), 1e-9);
        assertTrue(d.isActive());
    }

    @Test
    void growsGraduallyNotInstantly() {

        final CoreField f = run(new CoreDriver(), 1, Polarity.ATTRACT, 4, 256, true);
        assertTrue(f.radius() > 0 && f.radius() < 32);
    }

    @Test
    void fadesOutWhenDisabled() {

        final CoreDriver d = new CoreDriver();
        run(d, 100, Polarity.ATTRACT, 3, 256, true);
        final CoreField f = run(d, 100, Polarity.ATTRACT, 3, 256, false);
        assertEquals(0, f.radius(), 1e-9);
        assertFalse(d.isActive());
    }

    @Test
    void staysOffBelowMinimumSpeed() {

        final CoreDriver d = new CoreDriver();
        run(d, 100, Polarity.ATTRACT, 3, T.minRpm() - 1, true);
        assertFalse(d.isActive());
    }

    @Test
    void levitationUsesItsOwnLevels() {

        final CoreField f = run(new CoreDriver(), 100, Polarity.LEVITATE, 4, 128, true);
        assertEquals(1.25, f.strength(), 1e-9);
        assertEquals(Polarity.LEVITATE, f.polarity());
    }
}
