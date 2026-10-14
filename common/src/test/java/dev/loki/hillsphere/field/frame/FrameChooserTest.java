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

import dev.loki.hillsphere.field.math.Vec3d;
import java.util.function.ToDoubleFunction;
import org.junit.jupiter.api.Test;

class FrameChooserTest {

    private static final double NONE = Double.POSITIVE_INFINITY;

    /** A pull this many degrees from straight down toward +x (the EAST frame). */
    private static Vec3d tilted(double degrees) {

        final double a = Math.toRadians(degrees);
        return new Vec3d(Math.sin(a), -Math.cos(a), 0);
    }

    private static ToDoubleFunction<GravityFrame> floors(double down, double east) {

        return f -> f == GravityFrame.DOWN ? down : f == GravityFrame.EAST ? east : NONE;
    }

    private static GravityFrame run(FrameChooser chooser, int ticks, Vec3d pull, ToDoubleFunction<GravityFrame> floor) {

        GravityFrame frame = GravityFrame.DOWN;
        for (int i = 0; i < ticks; i++) {
            frame = chooser.next(frame, pull, floor);
        }
        return frame;
    }

    @Test
    void aFloorTheFieldLeansTowardIsTakenInTwoTicks() {

        final FrameChooser chooser = new FrameChooser();
        assertEquals(GravityFrame.DOWN, run(chooser, 1, tilted(48), floors(NONE, 0.2)));
        assertEquals(GravityFrame.EAST, chooser.next(GravityFrame.DOWN, tilted(48), floors(NONE, 0.2)));
    }

    @Test
    void aFloorIsNotTakenBeforeThePullPassesHalfway() {

        assertEquals(GravityFrame.DOWN, run(new FrameChooser(), 20, tilted(44), floors(NONE, 0)));
    }

    @Test
    void theFloorUnderTheFeetHoldsWhileThePullIsNotWellAboveTheHorizon() {

        assertEquals(GravityFrame.DOWN, run(new FrameChooser(), 20, tilted(100), floors(0, NONE)));
        assertEquals(GravityFrame.DOWN, run(new FrameChooser(), 20, tilted(60), floors(1.2, NONE)));
    }

    @Test
    void theFloorUnderTheFeetGivesUpWhenThePullIsFarAboveTheHorizon() {

        assertEquals(GravityFrame.EAST, run(new FrameChooser(), 20, tilted(120), floors(0, NONE)));
    }

    @Test
    void aWallWinsOverTheFloorOnceThePullLeansToIt() {

        assertEquals(GravityFrame.EAST, run(new FrameChooser(), 2, tilted(50), floors(0, 0)));
    }

    @Test
    void inFreeFlightTheFrameFollowsThePullJustPastHalfway() {

        assertEquals(GravityFrame.DOWN, run(new FrameChooser(), 20, tilted(49), floors(NONE, NONE)));
        assertEquals(GravityFrame.DOWN, run(new FrameChooser(), 2, tilted(53), floors(NONE, NONE)));
        assertEquals(GravityFrame.EAST, run(new FrameChooser(), 3, tilted(53), floors(NONE, NONE)));
    }

    @Test
    void aPullThatSwingsBackCancelsTheTurn() {

        final FrameChooser chooser = new FrameChooser();
        assertEquals(GravityFrame.DOWN, chooser.next(GravityFrame.DOWN, tilted(48), floors(NONE, 0)));
        assertEquals(GravityFrame.DOWN, chooser.next(GravityFrame.DOWN, tilted(10), floors(NONE, 0)));
        assertEquals(GravityFrame.DOWN, chooser.next(GravityFrame.DOWN, tilted(48), floors(NONE, 0)));
    }

    @Test
    void theFloorIsAskedForOnlyWhenATurnIsInQuestion() {

        final int[] asked = {0};
        new FrameChooser().next(GravityFrame.DOWN, tilted(10), f -> {
            asked[0]++;
            return NONE;
        });
        assertEquals(0, asked[0]);
    }
}
