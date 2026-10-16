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
package dev.loki.hillsphere.field.view.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShellEffectsTest {

    @Test
    void zeroDepthChangesNothing() {

        assertEquals(1, ShellEffects.pulse(13, 0, 60), 1e-12);
        assertEquals(1, ShellEffects.wave(13, 2.5, 0, 4, 0.02), 1e-12);
        assertEquals(1, ShellEffects.flicker(13, 1, 0, 3), 1e-12);
    }

    @Test
    void pulseSwingsBetweenOneAndOneMinusDepth() {

        assertEquals(1, ShellEffects.pulse(0, 0.4, 60), 1e-12);
        assertEquals(0.6, ShellEffects.pulse(30, 0.4, 60), 1e-12);
        assertEquals(1, ShellEffects.pulse(5, 0.4, 0), 1e-12);
    }

    @Test
    void waveRunsAlongTheLinesAndStaysInRange() {

        assertEquals(1, ShellEffects.wave(0, 0, 0.6, 4, 0.02), 1e-12);
        assertEquals(1, ShellEffects.wave(0, 4, 0.6, 4, 0.02), 1e-12);
        assertEquals(1, ShellEffects.wave(50, 0, 0.6, 4, 0.02), 1e-9);
        assertEquals(0.4, ShellEffects.wave(0, 2, 0.6, 4, 0.02), 1e-12);
        for (double x = 0; x < 8; x += 0.37) {
            final double g = ShellEffects.wave(7, x, 0.6, 4, 0.02);
            assertTrue(g >= 0.4 - 1e-12 && g <= 1 + 1e-12);
        }
    }

    @Test
    void flickerOnlyNearTheEdge() {

        assertEquals(1, ShellEffects.flicker(7, 0.4, 0.5, 1), 1e-12);
        assertEquals(1, ShellEffects.flicker(7, 1.6, 0.5, 1), 1e-12);
        for (double t = 0; t < 50; t += 0.7) {
            final double g = ShellEffects.flicker(t, 1, 0.5, 1);
            assertTrue(g >= 0.5 - 1e-12 && g <= 1 + 1e-12);
        }
    }

    @Test
    void entryRisesSmoothlyAndHoldsAtOne() {

        assertEquals(0, ShellEffects.entry(0, 20), 1e-12);
        assertEquals(0.5, ShellEffects.entry(10, 20), 1e-12);
        assertEquals(1, ShellEffects.entry(99, 20), 1e-12);
        assertEquals(1, ShellEffects.entry(0, 0), 1e-12);
    }
}
