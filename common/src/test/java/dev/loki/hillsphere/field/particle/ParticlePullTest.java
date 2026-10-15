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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.Gravity;

import org.junit.jupiter.api.Test;

class ParticlePullTest {

    private static final double EPS = 1e-12;
    private static final Vec3d EAST = new Vec3d(1, 0, 0);

    @Test
    void fadeIsFullNearAndZeroBeyondRange() {

        assertEquals(1, ParticlePull.fade(0, 32), EPS);
        assertEquals(1, ParticlePull.fade(22.4, 32), EPS);
        assertEquals(0.5, ParticlePull.fade(27.2, 32), EPS);
        assertEquals(0, ParticlePull.fade(32, 32), EPS);
        assertEquals(0, ParticlePull.fade(5, 0), EPS);
    }

    @Test
    void vanillaGravityNeedsNoPull() {

        assertTrue(ParticlePull.isVanilla(Gravity.VANILLA));
        assertFalse(ParticlePull.isVanilla(new Gravity(Vec3d.DOWN, 0.5)));
        assertFalse(ParticlePull.isVanilla(new Gravity(EAST, 1)));
        assertEquals(Vec3d.ZERO, ParticlePull.fall(Gravity.VANILLA, 1, 1));
    }

    @Test
    void fallTurnsTheVanillaPullTowardTheField() {

        final Vec3d sideways = ParticlePull.fall(new Gravity(EAST, 1), 1, 1);
        assertEquals(0.04, sideways.x(), EPS);
        assertEquals(0.04, sideways.y(), EPS);
        assertEquals(0, sideways.z(), EPS);
        assertEquals(0.02, ParticlePull.fall(new Gravity(EAST, 1), 1, 0.5).x(), EPS);
    }

    @Test
    void fallScalesWithParticleGravityAndStaysZeroWithout() {

        assertEquals(0, ParticlePull.fall(new Gravity(EAST, 1), 0, 1).length(), EPS);
        assertEquals(0.08, ParticlePull.fall(new Gravity(EAST, 1), 2, 1).x(), EPS);
    }

    @Test
    void weightlessCancelsTheWorldPull() {

        final Vec3d v = ParticlePull.fall(new Gravity(Vec3d.DOWN, 0), 1, 1);
        assertEquals(0.04, v.y(), EPS);
    }

    @Test
    void aPushReversesTheFall() {

        final Vec3d v = ParticlePull.fall(new Gravity(EAST, -1), 1, 1);
        assertEquals(-0.04, v.x(), EPS);
    }

    @Test
    void leanTurnsAClimbAgainstTheField() {

        final Vec3d v = ParticlePull.lean(new Vec3d(0.01, 0.1, 0), new Gravity(EAST, 1), 1);
        assertEquals(-0.09, v.x(), EPS);
        assertEquals(0, v.y(), EPS);
    }

    @Test
    void leanKeepsVanillaAndZeroMix() {

        final Vec3d start = new Vec3d(0.01, 0.1, 0.02);
        assertEquals(start, ParticlePull.lean(start, Gravity.VANILLA, 1));
        assertEquals(start, ParticlePull.lean(start, new Gravity(EAST, 1), 0));
    }

    @Test
    void aPushFlipsUp() {

        assertEquals(EAST, ParticlePull.up(new Gravity(EAST, -1)));
    }
}
