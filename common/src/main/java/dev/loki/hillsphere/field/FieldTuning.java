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

import java.util.List;

/**
 * Every number that shapes a hill core field. The defaults are starting values for
 * the prototype; they are meant to move into the config and be tuned in play.
 *
 * @param minRpm field is off below this speed
 * @param maxRpm speed at which the radius stops growing
 * @param minRadius radius at {@code minRpm}-ish speeds, in blocks
 * @param maxRadius radius at {@code maxRpm}, in blocks
 * @param plateau fraction of the radius where the pull is still full strength
 * @param levelStrengths pull per strength level, in multiples of vanilla gravity
 * @param levitationLevels share of gravity cancelled per level by a levitation core (1 cancels it fully)
 * @param stressPerRpmPerLevel stress impact per RPM and per strength level
 * @param blendStart magnitude ratio where two fields start to separate
 * @param blendEnd magnitude ratio where the stronger field fully wins
 * @param rampTicks ticks the field takes to grow to or shrink from its target
 */
public record FieldTuning(
        double minRpm,
        double maxRpm,
        double minRadius,
        double maxRadius,
        double plateau,
        List<Double> levelStrengths,
        List<Double> levitationLevels,
        double stressPerRpmPerLevel,
        double blendStart,
        double blendEnd,
        int rampTicks) {

    public static final FieldTuning DEFAULT = new FieldTuning(
            8, 256, 2, 32, 0.6, List.of(0.25, 0.5, 1.0, 1.5), List.of(0.25, 0.5, 1.0, 1.25), 4, 1.0, 1.25, 30);

    public FieldTuning {

        levelStrengths = List.copyOf(levelStrengths);
        levitationLevels = List.copyOf(levitationLevels);
        require(minRpm > 0 && maxRpm >= minRpm, "speed range");
        require(minRadius > 0 && maxRadius >= minRadius, "radius range");
        require(plateau >= 0 && plateau < 1, "plateau must be in [0, 1)");
        require(blendEnd > blendStart, "blendEnd must be above blendStart");
        require(rampTicks > 0, "rampTicks must be positive");
        require(!levelStrengths.isEmpty() && !levitationLevels.isEmpty(), "levels must not be empty");
    }

    private static void require(boolean ok, String what) {

        if (!ok) {
            throw new IllegalArgumentException("invalid field tuning: " + what);
        }
    }

    public int levels() {

        return levelStrengths.size();
    }

    /** True if a core spinning at this speed produces a field at all. */
    public boolean isSpinningFastEnough(double rpm) {

        return Math.abs(rpm) >= minRpm;
    }

    /** Target radius for a given speed; 0 when the core is too slow. Direction of rotation is ignored. */
    public double radius(double rpm) {

        final double speed = Math.abs(rpm);
        if (speed < minRpm) {
            return 0;
        }
        return minRadius + (maxRadius - minRadius) * Math.min(speed, maxRpm) / maxRpm;
    }

    /** Pull for a 1-based strength level, clamped into the valid range. */
    public double strength(int level) {

        return levelStrengths.get(Math.max(1, Math.min(level, levels())) - 1);
    }

    /** Levitation share for a 1-based level, clamped into the valid range. */
    public double levitation(int level) {

        return levitationLevels.get(Math.max(1, Math.min(level, levitationLevels.size())) - 1);
    }

    /** Stress the core puts on the network, in SU. */
    public double stress(double rpm, int level) {

        final int clamped = Math.max(1, Math.min(level, levels()));
        return Math.abs(rpm) * stressPerRpmPerLevel * clamped;
    }

    /** Pull falloff: 1 on the plateau, smoothly down to 0 at the radius. {@code x} is distance / radius. */
    public double fade(double x) {

        if (x <= plateau) {
            return 1;
        }
        if (x >= 1) {
            return 0;
        }
        final double t = (x - plateau) / (1 - plateau);
        return 1 - t * t * (3 - 2 * t);
    }

    /** Largest change per tick of a radius while it ramps up or down. */
    public double radiusStep() {

        return maxRadius / rampTicks;
    }

    /** Largest change per tick of a strength while it ramps up or down. */
    public double strengthStep() {

        return levelStrengths.stream().mapToDouble(Double::doubleValue).max().orElse(1) / rampTicks;
    }
}
