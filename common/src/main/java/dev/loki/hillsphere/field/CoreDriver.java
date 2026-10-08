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
package dev.loki.hillsphere.field;

import dev.loki.hillsphere.field.math.Ramp;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;

/**
 * The live state of one core: how far its field has grown. Each tick it moves the radius and
 * strength toward what the current speed and settings ask for, so a field spins up and fades
 * out instead of snapping.
 */
public final class CoreDriver {

    private double radius;
    private double strength;

    /**
     * Advances the field by one tick.
     *
     * @param rpm signed rotation speed of the core
     * @param enabled false when something (redstone, overstress) switches the core off
     * @return the core as the resolver should see it now
     */
    public CoreField step(FieldTuning tuning, Vec3d center, Polarity polarity, double level, double rpm, boolean enabled) {

        final boolean on = enabled && tuning.isSpinningFastEnough(rpm);
        final double targetRadius = on ? tuning.radius(rpm) : 0;
        final double targetStrength = on ? power(tuning, polarity, level) : 0;
        radius = Ramp.towards(radius, targetRadius, tuning.radiusStep());
        strength = Ramp.towards(strength, targetStrength, tuning.strengthStep());
        return new CoreField(center, radius, strength, polarity);
    }

    /** True while the field reaches anywhere. */
    public boolean isActive() {

        return radius > 0 && strength > 0;
    }

    private static double power(FieldTuning tuning, Polarity polarity, double level) {

        return polarity == Polarity.LEVITATE ? tuning.levitation(level) : tuning.strength(level);
    }
}
