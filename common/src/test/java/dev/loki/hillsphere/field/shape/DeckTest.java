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
package dev.loki.hillsphere.field.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.loki.hillsphere.field.math.Vec3d;
import org.junit.jupiter.api.Test;

class DeckTest {

    private static final double EPS = 1e-9;
    private static final Deck CUBE = Deck.cube(Vec3d.ZERO, 4);

    /** A long box turned a quarter round the vertical: its own x runs along the world's z. */
    private static final Deck TURNED = new Deck(Vec3d.ZERO, new Vec3d(0, 0, 1), new Vec3d(0, 1, 0), new Vec3d(-1, 0, 0),
            new Vec3d(10, 2, 3));

    @Test
    void insideIsNowhereOutside() {

        assertEquals(0, CUBE.outside(new Vec3d(3.9, -3.9, 3.9)), EPS);
        assertEquals(1, CUBE.fade(new Vec3d(3.9, -3.9, 3.9)), EPS);
    }

    @Test
    void outsideIsTheDistanceToTheNearestPointOfTheBox() {

        assertEquals(1, CUBE.outside(new Vec3d(5, 0, 0)), EPS);
        assertEquals(Math.sqrt(2), CUBE.outside(new Vec3d(5, 5, 0)), EPS);
    }

    @Test
    void thePullFadesOutOverTheEdge() {

        assertEquals(0.5, CUBE.fade(new Vec3d(4 + Deck.EDGE / 2, 0, 0)), EPS);
        assertEquals(0, CUBE.fade(new Vec3d(4 + Deck.EDGE, 0, 0)), EPS);
    }

    @Test
    void aTurnedBoxIsMeasuredAlongItsOwnAxes() {

        assertEquals(0, TURNED.outside(new Vec3d(2.5, 0, 9.5)), EPS);
        assertEquals(1, TURNED.outside(new Vec3d(4, 0, 0)), EPS);
    }

    @Test
    void theReachCoversTheFarthestCornerAndTheEdge() {

        final Vec3d from = new Vec3d(3, 0, 0);
        final Vec3d corner = new Vec3d(-3, 2, -10);
        assertTrue(TURNED.reachFrom(from) >= corner.sub(from).length() + Deck.EDGE);
    }
}
