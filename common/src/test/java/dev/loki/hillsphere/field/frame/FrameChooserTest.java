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
    void standingOnTheNewFloorTheFrameJustLeftNeedsAPullWellPastHalfway() {

        final FrameChooser chooser = new FrameChooser();
        assertEquals(GravityFrame.EAST, run(chooser, 2, tilted(48), floors(0, 0)));
        GravityFrame frame = GravityFrame.EAST;
        for (int i = 0; i < 20; i++) {
            frame = chooser.next(frame, tilted(40), floors(0, 0));
        }
        assertEquals(GravityFrame.EAST, frame, "a pull a few degrees back over halfway is the turn's own wobble");
        frame = chooser.next(frame, tilted(25), floors(0, 0));
        assertEquals(GravityFrame.DOWN, chooser.next(frame, tilted(25), floors(0, 0)), "a pull well back is a real way back");
    }

    @Test
    void offTheNewFloorTheFrameJustLeftIsTakenAsSoonAsAnyOther() {

        final FrameChooser chooser = new FrameChooser();
        assertEquals(GravityFrame.EAST, run(chooser, 2, tilted(48), floors(0, 0)));
        chooser.next(GravityFrame.EAST, tilted(40), floors(0, NONE));
        assertEquals(GravityFrame.DOWN, chooser.next(GravityFrame.EAST, tilted(40), floors(0, NONE)));
    }

    @Test
    void walkingAlongTheShoulderOfAStairStepBallNeverTurnsStraightBack() {

        final StringBuilder back = new StringBuilder();
        for (final double radius : new double[] {4, 5, 6, 7}) {
            for (final double speed : new double[] {0.1, 0.15, 0.22}) {
                for (final double tilt : new double[] {40, 45, 50}) {
                    final double a = Math.toRadians(tilt);
                    final StairPlanet walk = new StairPlanet(radius, speed, new Vec3d(Math.sin(a), Math.cos(a), 0.01)).walkAround();
                    if (walk.turnsBack() > 0) {
                        back.append(String.format(java.util.Locale.ROOT, "%n radius %.0f, %.2f blocks a tick, %.0f degrees from the top: %d of %d turns",
                                radius, speed, tilt, walk.turnsBack(), walk.turns()));
                    }
                }
            }
        }
        assertEquals("", back.toString(), "walks around the ball that turned straight back");
    }

    @Test
    void walkingOverTheShoulderOfAStairStepBallTurnsOncePerFace() {

        for (final double radius : new double[] {4, 5, 6, 7}) {
            assertEquals(1, new StairPlanet(radius, 0.15, new Vec3d(0.01, 1, 0)).walkOver(new Vec3d(1, 0, 0)).turns(), "radius " + radius);
            assertTrue(new StairPlanet(radius, 0.15, new Vec3d(0.01, 1, 0)).walkOver(new Vec3d(1, 0, 1)).turns() <= 2, "radius " + radius);
        }
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
