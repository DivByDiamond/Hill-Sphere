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
package dev.loki.hillsphere.field.view.effect;

/**
 * Brightness effects of the goggles' boundary lines. Each returns a gain from 0 to 1 that multiplies the
 * line's alpha; a depth of 0 always gives 1, so an effect that is off changes nothing.
 */
public final class ShellEffects {

    private static final double TAU = 2 * Math.PI;
    /** How far from the edge, as a share of the radius, the flicker reaches. */
    private static final double EDGE_BAND = 0.25;

    private ShellEffects() {
    }

    /** Whole-shell breathing between {@code 1 - depth} and 1 once per {@code period} ticks. */
    public static double pulse(double time, double depth, double period) {

        if (period <= 0) {
            return 1;
        }
        return 1 - clamp(depth) * (0.5 - 0.5 * Math.cos(TAU * time / period));
    }

    /**
     * A bright band running along the lines: {@code along} is the position in blocks on the axis it runs
     * on, one band per {@code wavelength} blocks, {@code speed} bands passing per tick.
     */
    public static double wave(double time, double along, double depth, double wavelength, double speed) {

        if (wavelength <= 0) {
            return 1;
        }
        final double band = 0.5 + 0.5 * Math.cos(TAU * (along / wavelength - time * speed));
        return 1 - clamp(depth) * (1 - band * band);
    }

    /**
     * Unsteady brightness close to the edge of the field: {@code edgeRatio} is the viewer's distance from the
     * core over the radius. Far from 1 it has no effect; {@code seed} keeps two fields out of step.
     */
    public static double flicker(double time, double edgeRatio, double depth, double seed) {

        final double near = Math.max(0, 1 - Math.abs(edgeRatio - 1) / EDGE_BAND);
        final double noise = 0.5 + 0.5 * Math.sin(time * 1.9 + seed) * Math.sin(time * 3.1 + seed * 2.3);
        return 1 - clamp(depth) * near * noise;
    }

    /** Fade-in over {@code duration} ticks after the field came into sight: 0 at the start, 1 at the end. */
    public static double entry(double age, double duration) {

        if (duration <= 0) {
            return 1;
        }
        final double t = clamp(age / duration);
        return t * t * (3 - 2 * t);
    }

    private static double clamp(double x) {

        return Math.max(0, Math.min(1, x));
    }
}
