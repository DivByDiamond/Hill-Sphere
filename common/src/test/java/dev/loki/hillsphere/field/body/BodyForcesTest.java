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
package dev.loki.hillsphere.field.body;

import dev.loki.hillsphere.field.FieldTuning;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.body.Push.Kind;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BodyForcesTest {

    private static final double EPS = 1e-9;
    private static final BodyForces F = new BodyForces(FieldTuning.DEFAULT);
    private static final Vec3d G = new Vec3d(0, -10, 0);

    private static Body<String> body(String id, double x, double mass) {

        return new Body<>(id, new Vec3d(x, 0, 0), mass, G);
    }

    private static Mounted<String> core(String carrier, Polarity polarity, double strength) {

        return new Mounted<>(new CoreField(Vec3d.ZERO, 32, strength, polarity), carrier);
    }

    private static void assertVec(Vec3d expected, Vec3d actual) {

        assertEquals(expected.x(), actual.x(), EPS);
        assertEquals(expected.y(), actual.y(), EPS);
        assertEquals(expected.z(), actual.z(), EPS);
    }

    @Test
    void levitationOnTheShipCancelsItsWholeWeightAndNoMoreEvenOutsideTheField() {

        final List<Push<String>> p = F.of(List.of(body("a", 50, 10)), List.of(core("a", Polarity.LEVITATE, 1.25)), 1);
        assertEquals(1, p.size());
        assertEquals(Kind.LIFT, p.get(0).kind());
        assertVec(new Vec3d(0, 100, 0), p.get(0).force());
        assertVec(new Vec3d(50, 0, 0), p.get(0).point());
    }

    @Test
    void levitationFromElsewhereLiftsOnlyInsideItsField() {

        final Mounted<String> ground = core(null, Polarity.LEVITATE, 1);
        assertVec(new Vec3d(0, 100, 0), F.of(List.of(body("a", 5, 10)), List.of(ground), 1).get(0).force());
        assertTrue(F.of(List.of(body("a", 40, 10)), List.of(ground), 1).isEmpty());
    }

    @Test
    void pullBetweenShipsIsEqualAndOpposite() {

        final List<Push<String>> p = F.of(List.of(body("b", 5, 4)), List.of(core("a", Polarity.ATTRACT, 1)), 1);
        assertEquals(2, p.size());
        assertEquals("b", p.get(0).body());
        assertVec(new Vec3d(-40, 0, 0), p.get(0).force());
        assertEquals("a", p.get(1).body());
        assertVec(new Vec3d(40, 0, 0), p.get(1).force());
        assertVec(Vec3d.ZERO, p.get(1).point());
        assertVec(Vec3d.ZERO, p.get(0).force().add(p.get(1).force()));
    }

    @Test
    void repelPushesAwayAndScales() {

        final List<Push<String>> p = F.of(List.of(body("b", 5, 4)), List.of(core("a", Polarity.REPEL, 2)), 0.5);
        assertVec(new Vec3d(40, 0, 0), p.get(0).force());
        assertVec(new Vec3d(-40, 0, 0), p.get(1).force());
    }

    @Test
    void aCoreDoesNotPullItsOwnShip() {

        assertTrue(F.of(List.of(body("a", 3, 4)), List.of(core("a", Polarity.ATTRACT, 1)), 1).isEmpty());
    }

    @Test
    void aGroundCoreHasNothingToPushBack() {

        final List<Push<String>> p = F.of(List.of(body("b", 5, 4)), List.of(core(null, Polarity.ATTRACT, 1)), 1);
        assertEquals(1, p.size());
        assertEquals(Kind.PULL, p.get(0).kind());
    }

    @Test
    void twoShipsWithCoresPullEachOtherAndEverythingCancels() {

        final Mounted<String> onA = core("a", Polarity.ATTRACT, 1);
        final Mounted<String> onB = new Mounted<>(new CoreField(new Vec3d(6, 0, 0), 20, 1, Polarity.ATTRACT), "b");
        final List<Push<String>> p = F.of(List.of(body("a", 1, 3), body("b", 5, 7)), List.of(onA, onB), 1);
        assertEquals(4, p.size());
        Vec3d sum = Vec3d.ZERO;
        for (final Push<String> push : p) {
            sum = sum.add(push.force());
        }
        assertVec(Vec3d.ZERO, sum);
    }

    @Test
    void aShipsOwnLiftThinsOutWithHeightSoThrustCannotClimbForEver() {

        final List<Mounted<String>> core = List.of(core("a", Polarity.LEVITATE, 1));
        final Body<String> mid = new Body<>("a", new Vec3d(0, 160, 0), 10, G);
        final Body<String> top = new Body<>("a", new Vec3d(0, 300, 0), 10, G);
        assertVec(new Vec3d(0, 50, 0), F.of(List.of(mid), core, 1).get(0).force());
        assertTrue(F.of(List.of(top), core, 1).isEmpty());
    }

    @Test
    void aSmallerFieldLiftsEvenALightShipLess() {

        final Body<String> light = body("a", 0, 10);
        final double small = F.of(List.of(light), List.of(new Mounted<>(new CoreField(Vec3d.ZERO, 2, 1, Polarity.LEVITATE), "a")), 1)
                .get(0).force().y();
        final double big = F.of(List.of(light), List.of(core("a", Polarity.LEVITATE, 1)), 1).get(0).force().y();
        assertEquals(20, small, EPS);
        assertEquals(100, big, EPS);
    }

    @Test
    void aHeavyShipIsHeldOnlyAsFarAsTheCoreCanCarry() {

        // radius 32 carries 40 * 32 = 1280; a ship of 2000 gets 64 percent of its weight cancelled, however high the level
        final Body<String> giant = new Body<>("a", new Vec3d(0, 0, 0), 2000, G);
        assertVec(new Vec3d(0, 12800, 0), F.of(List.of(giant), List.of(core("a", Polarity.LEVITATE, 1)), 1).get(0).force());
    }

    @Test
    void severalCoresNeverLiftMoreThanTheWholeWeight() {

        final List<Mounted<String>> three = List.of(core("a", Polarity.LEVITATE, 0.9), core("a", Polarity.LEVITATE, 0.9),
                core("a", Polarity.LEVITATE, 0.9));
        assertVec(new Vec3d(0, 100, 0), F.of(List.of(body("a", 0, 10)), three, 1).get(0).force());
    }
}
