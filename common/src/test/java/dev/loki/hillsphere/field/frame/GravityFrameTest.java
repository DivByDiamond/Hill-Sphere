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
package dev.loki.hillsphere.field.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.loki.hillsphere.field.math.Vec3d;

import org.junit.jupiter.api.Test;

class GravityFrameTest {

    private static final double EPS = 1e-12;
    private static final Vec3d SAMPLE = new Vec3d(0.3, -1.7, 2.9);

    private static void same(Vec3d expected, Vec3d actual, String message) {

        assertEquals(expected.x(), actual.x(), EPS, message);
        assertEquals(expected.y(), actual.y(), EPS, message);
        assertEquals(expected.z(), actual.z(), EPS, message);
    }

    @Test
    void everyFrameTurnsVirtualDownToItsOwnDirection() {

        same(Vec3d.DOWN, GravityFrame.DOWN.toReal(Vec3d.DOWN), "down");
        same(new Vec3d(0, 1, 0), GravityFrame.UP.toReal(Vec3d.DOWN), "down");
        same(new Vec3d(1, 0, 0), GravityFrame.EAST.toReal(Vec3d.DOWN), "down");
        same(new Vec3d(-1, 0, 0), GravityFrame.WEST.toReal(Vec3d.DOWN), "down");
        same(new Vec3d(0, 0, 1), GravityFrame.SOUTH.toReal(Vec3d.DOWN), "down");
        same(new Vec3d(0, 0, -1), GravityFrame.NORTH.toReal(Vec3d.DOWN), "down");
    }

    @Test
    void toVirtualUndoesToReal() {

        for (final GravityFrame frame : GravityFrame.values()) {
            final Vec3d back = frame.toVirtual(frame.toReal(SAMPLE));
            assertEquals(SAMPLE.x(), back.x(), EPS, frame.name());
            assertEquals(SAMPLE.y(), back.y(), EPS, frame.name());
            assertEquals(SAMPLE.z(), back.z(), EPS, frame.name());
        }
    }

    @Test
    void framesAreTurnsNotMirrorsAndKeepLengths() {

        for (final GravityFrame frame : GravityFrame.values()) {
            final Vec3d[] a = frame.axes();
            same(a[2], a[0].cross(a[1]), frame.name() + " must keep handedness");
            assertEquals(SAMPLE.length(), frame.toReal(SAMPLE).length(), EPS, frame.name());
        }
    }

    @Test
    void ofSnapsToTheClosestAxis() {

        assertEquals(GravityFrame.DOWN, GravityFrame.of(new Vec3d(0.3, -0.9, 0.1)));
        assertEquals(GravityFrame.UP, GravityFrame.of(new Vec3d(0.3, 0.9, 0.1)));
        assertEquals(GravityFrame.EAST, GravityFrame.of(new Vec3d(0.9, -0.3, 0.1)));
        assertEquals(GravityFrame.WEST, GravityFrame.of(new Vec3d(-0.9, 0.3, 0.1)));
        assertEquals(GravityFrame.SOUTH, GravityFrame.of(new Vec3d(0.1, 0.2, 0.9)));
        assertEquals(GravityFrame.NORTH, GravityFrame.of(new Vec3d(0.1, 0.2, -0.9)));
    }

    @Test
    void aZeroDirectionIsVanilla() {

        assertTrue(GravityFrame.of(Vec3d.ZERO).isVanilla());
    }
}
