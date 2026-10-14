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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.loki.hillsphere.field.FieldTuning;
import dev.loki.hillsphere.field.GravityField;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.shape.Deck;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeckFieldTest {

    private static final double EPS = 1e-9;
    private static final FieldTuning TUNING = FieldTuning.DEFAULT;
    private static final GravityResolver RESOLVER = new GravityResolver(TUNING);

    /** A ship lying on its side: its own "up" is the world's +x. */
    private static final Deck ON_ITS_SIDE = new Deck(new Vec3d(0, 0, 0), new Vec3d(0, -1, 0), new Vec3d(1, 0, 0),
            new Vec3d(0, 0, 1), new Vec3d(3, 20, 3));

    private static CoreField deck(Polarity polarity, Deck box) {

        return new CoreField(Vec3d.ZERO, 8, 1, polarity).withDeck(box);
    }

    @Test
    void anAttractingDeckPullsTowardTheFloorOfWhatCarriesIt() {

        final Gravity g = RESOLVER.resolve(List.of(deck(Polarity.ATTRACT, ON_ITS_SIDE)), new Vec3d(15, 1, 0));
        assertEquals(-1, g.direction().x(), EPS);
        assertEquals(1, g.strength(), EPS);
    }

    @Test
    void aRepellingDeckPullsTowardItsCeiling() {

        final Gravity g = RESOLVER.resolve(List.of(deck(Polarity.REPEL, Deck.cube(Vec3d.ZERO, 8))), new Vec3d(2, -7, 3));
        assertEquals(1, g.direction().y(), EPS);
    }

    @Test
    void aDeckReachesTheCornersASphereOfTheSameSizeMisses() {

        final Vec3d corner = new Vec3d(7.5, 7.5, 7.5);
        final CoreField sphere = new CoreField(Vec3d.ZERO, 8, 1, Polarity.REPEL);
        assertEquals(Gravity.VANILLA, RESOLVER.resolve(List.of(sphere), corner));
        assertEquals(1, RESOLVER.resolve(List.of(sphere.withDeck(Deck.cube(Vec3d.ZERO, 8))), corner).direction().y(), EPS);
    }

    @Test
    void aDeckThatIsOffPullsNothing() {

        final CoreField off = new CoreField(Vec3d.ZERO, 0, 1, Polarity.ATTRACT).withDeck(Deck.cube(Vec3d.ZERO, 8));
        assertEquals(Gravity.VANILLA, RESOLVER.resolve(List.of(off), Vec3d.ZERO));
    }

    @Test
    void aLevitatingDeckLiftsAllOverItsBox() {

        final CoreField lift = new CoreField(Vec3d.ZERO, 8, 1, Polarity.LEVITATE).withDeck(ON_ITS_SIDE);
        assertEquals(1, RESOLVER.liftAt(List.of(lift), new Vec3d(19, 2, 2)), EPS);
    }

    @Test
    void theWorldFindsADeckFarFromItsCore() {

        final GravityField<String> field = new GravityField<>(TUNING, 16);
        final Deck hull = new Deck(new Vec3d(60, 0, 0), new Vec3d(1, 0, 0), new Vec3d(0, 1, 0), new Vec3d(0, 0, 1),
                new Vec3d(60, 4, 4));
        field.put("ship", new CoreField(Vec3d.ZERO, 8, 1, Polarity.REPEL).withDeck(hull));
        assertTrue(field.gravityAt(new Vec3d(118, 0, 0)).direction().y() > 0.99);
    }
}
