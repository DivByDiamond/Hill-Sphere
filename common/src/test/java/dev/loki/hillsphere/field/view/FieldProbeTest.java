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

import dev.loki.hillsphere.field.FieldTuning;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;

import java.util.List;
import org.junit.jupiter.api.Test;

class FieldProbeTest {

    private static final FieldTuning T = FieldTuning.DEFAULT;

    @Test
    void strongestCoreGivesTheColour() {

        final CoreField weak = new CoreField(Vec3d.ZERO, 10, 0.5, Polarity.ATTRACT);
        final CoreField strong = new CoreField(new Vec3d(4, 0, 0), 10, 2, Polarity.REPEL);
        final FieldProbe probe = FieldProbe.at(T, List.of(weak, strong), new Vec3d(2, 0, 0));
        assertEquals(Polarity.REPEL, probe.polarity());
        assertEquals(1, probe.influence(), 1e-9);
    }

    @Test
    void outsideEveryFieldIsNone() {

        final CoreField core = new CoreField(Vec3d.ZERO, 4, 1, Polarity.LEVITATE);
        assertFalse(FieldProbe.at(T, List.of(core), new Vec3d(9, 0, 0)).inField());
    }

    @Test
    void influenceFadesTowardsTheEdge() {

        final CoreField core = new CoreField(Vec3d.ZERO, 10, 1, Polarity.ATTRACT);
        final double edge = FieldProbe.at(T, List.of(core), new Vec3d(9, 0, 0)).influence();
        assertEquals(T.fade(0.9), edge, 1e-9);
    }
}
