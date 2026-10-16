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
package dev.loki.hillsphere.field.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.loki.hillsphere.field.FieldTuning;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;
import dev.loki.hillsphere.field.resolve.GravityResolver;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class BoundaryProfileTest {

    private static final FieldTuning T = FieldTuning.DEFAULT;
    private static final CoreField A = new CoreField(Vec3d.ZERO, 8, 1, Polarity.ATTRACT);

    private static CoreField at(double x, double radius, double strength, Polarity polarity) {

        return new CoreField(new Vec3d(x, 0, 0), radius, strength, polarity);
    }

    private static Stream<BoundaryProfile.Point> points(BoundaryProfile.Profile profile) {

        return Stream.concat(profile.rings().stream(), profile.segments().stream().flatMap(s -> Stream.of(s.from(), s.to())));
    }

    /** Where the profile meets the axis, nearest first to {@code near}. */
    private static BoundaryProfile.Point onAxis(BoundaryProfile.Profile profile, double near) {

        return profile.rings().stream().filter(p -> p.radial() == 0)
                .min((p, q) -> Double.compare(Math.abs(p.along() - near), Math.abs(q.along() - near))).orElseThrow();
    }

    @Test
    void equalAttractorsSplitHalfway() {

        final BoundaryProfile.Profile profile = BoundaryProfile.between(T, A, at(10, 8, 1, Polarity.ATTRACT), 6);
        assertFalse(profile.segments().isEmpty());
        assertTrue(profile.rings().stream().anyMatch(p -> p.radial() > 0));
        points(profile).forEach(p -> assertEquals(5, p.along(), 1e-6));
        assertEquals(1, onAxis(profile, 5).weight(), 0.05);
    }

    @Test
    void overlappingPlateausOfEqualFieldsSplitHalfwayToo() {

        final BoundaryProfile.Profile profile = BoundaryProfile.between(T, at(0, 16, 1, Polarity.ATTRACT), at(10, 16, 1, Polarity.ATTRACT), 8);
        assertFalse(profile.segments().isEmpty());
        points(profile).forEach(p -> assertEquals(5, p.along(), 1e-6));
    }

    @Test
    void strongerFieldPushesTheBoundaryAway() {

        assertTrue(onAxis(BoundaryProfile.between(T, A, at(10, 8, 2, Polarity.ATTRACT), 4), 5).along() < 5);
    }

    @Test
    void fieldsThatDoNotTouchHaveNoBoundary() {

        assertTrue(BoundaryProfile.between(T, A, at(20, 8, 1, Polarity.ATTRACT), 4).isEmpty());
    }

    @Test
    void theSurfaceLiesWhereTheResolverSplitsHalfAndHalf() {

        final CoreField a = at(0, 16, 1, Polarity.ATTRACT);
        final CoreField b = at(10, 16, 1.5, Polarity.ATTRACT);
        final List<BoundaryProfile.Point> all = points(BoundaryProfile.between(T, a, b, 8)).toList();
        assertFalse(all.isEmpty());
        for (final BoundaryProfile.Point p : all) {
            final Vec3d point = new Vec3d(p.along(), p.radial(), 0);
            assertTrue(point.length() < a.radius() && point.sub(b.center()).length() < b.radius(), "outside a field: " + p);
            final double pa = a.strength() * a.fade(T, point);
            final double pb = b.strength() * b.fade(T, point);
            assertEquals(1, GravityResolver.share(T, Math.max(pa, pb), Math.min(pa, pb)), 0.05, "not on the seam: " + p);
        }
    }

    @Test
    void attractorAndRepellerAgreeBetweenThemSoTheSeamThereIsFaint() {

        final BoundaryProfile.Profile profile = BoundaryProfile.between(T, A, at(10, 8, 1, Polarity.REPEL), 6);
        assertEquals(5, onAxis(profile, 5).along(), 1e-6);
        assertEquals(0, onAxis(profile, 5).weight(), 1e-6);
    }

    @Test
    void aWeakerRepellerHasItsBalancePointBehindIt() {

        final BoundaryProfile.Point balance = onAxis(BoundaryProfile.between(T, at(0, 16, 2, Polarity.ATTRACT), at(10, 16, 1, Polarity.REPEL), 16), 13);
        assertTrue(balance.along() > 12 && balance.along() < 14.5, "at " + balance);
        assertTrue(balance.weight() > 0.5, "weight " + balance.weight());
    }

    @Test
    void aFieldInsideABigWeakOneIsWrappedRound() {

        final BoundaryProfile.Profile profile = BoundaryProfile.between(T, A, at(12, 30, 0.5, Polarity.ATTRACT), 12);
        final BoundaryProfile.Point behind = onAxis(profile, -7);
        final BoundaryProfile.Point between = onAxis(profile, 7);
        assertTrue(behind.along() < -5 && between.along() > 5, behind + " " + between);
        assertTrue(behind.weight() < 0.05, "both pull the same way behind the small core");
        assertTrue(between.weight() > 0.2, "they pull apart between the cores");
    }
}
