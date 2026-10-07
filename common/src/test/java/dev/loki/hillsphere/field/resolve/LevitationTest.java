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
package dev.loki.hillsphere.field.resolve;

import dev.loki.hillsphere.field.FieldTuning;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.math.Vec3d;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LevitationTest {

    private static final double EPS = 1e-9;
    private static final GravityResolver R = new GravityResolver(FieldTuning.DEFAULT);

    private static CoreField levitator(double x, double y, double radius, double lift) {

        return new CoreField(new Vec3d(x, y, 0), radius, lift, Polarity.LEVITATE);
    }

    private static CoreField planet(double x, double y, double radius, double strength) {

        return new CoreField(new Vec3d(x, y, 0), radius, strength, Polarity.ATTRACT);
    }

    @Test
    void fullLiftCancelsGravityOnThePlateau() {

        final Gravity g = R.resolve(List.of(levitator(0, 0, 20, 1)), new Vec3d(3, 4, 0));
        assertEquals(0, g.strength(), EPS);
        assertEquals(Vec3d.DOWN, g.direction());
    }

    @Test
    void liftAboveOnePushesAgainstDown() {

        final Gravity g = R.resolve(List.of(levitator(0, 0, 20, 1.25)), new Vec3d(0, 5, 0));
        assertEquals(-0.25, g.strength(), EPS);
    }

    @Test
    void noEffectOutsideTheField() {

        assertEquals(Gravity.VANILLA, R.resolve(List.of(levitator(0, 0, 10, 1)), new Vec3d(10, 0, 0)));
    }

    @Test
    void liftFadesSmoothlyToVanilla() {

        final CoreField c = levitator(0, 0, 10, 1);
        double previous = 0;
        for (double x = 0; x < 10; x += 0.05) {
            final double s = R.resolve(List.of(c), new Vec3d(x, 0, 0)).strength();
            assertTrue(s >= previous - EPS && s <= 1 + EPS, "strength not monotonic at " + x);
            previous = s;
        }
    }

    @Test
    void levitatorsAddUp() {

        final List<CoreField> cores = List.of(levitator(0, 0, 20, 0.5), levitator(1, 0, 20, 0.5));
        assertEquals(0, R.resolve(cores, new Vec3d(0, 3, 0)).strength(), EPS);
    }

    @Test
    void levitationWeakensAPlanetWithoutChangingItsDirection() {

        final List<CoreField> cores = List.of(planet(0, 10, 20, 1.5), levitator(0, 0, 20, 0.5));
        final Gravity g = R.resolve(cores, new Vec3d(0, 0, 0));
        assertEquals(1, g.direction().y(), EPS);
        assertEquals(0.75, g.strength(), EPS);
    }

    @Test
    void aBodyLiftedAboveOneFindsAStableHeightAtTheEdge() {

        final CoreField c = levitator(0, 0, 20, 1.25);
        final double inside = R.resolve(List.of(c), new Vec3d(0, 8, 0)).strength();
        final double outside = R.resolve(List.of(c), new Vec3d(0, 19.5, 0)).strength();
        assertTrue(inside < 0, "should be pushed up on the plateau");
        assertTrue(outside > 0, "should fall back near the edge");
    }
}
