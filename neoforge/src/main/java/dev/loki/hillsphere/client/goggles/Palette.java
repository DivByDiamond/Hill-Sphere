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
package dev.loki.hillsphere.client.goggles;

import dev.loki.hillsphere.field.Polarity;

/**
 * Colours of everything the goggles draw, as 0xRRGGBB. All of them come from the palette table in
 * {@code docs/ART.md}: attraction is teal, repulsion amber, levitation the gravite violet, "off" grey,
 * text and boundaries the pale tint of the metal ramp.
 */
public final class Palette {

    /** Attraction: dark, main, light. */
    public static final int[] ATTRACT = {0x1C8C99, 0x3FD9E8, 0xA8F5FF};
    /** Repulsion: dark, main, light. */
    public static final int[] REPEL = {0xB3601A, 0xFFA03A, 0xFFD08A};
    /** Levitation, the gravite crystal colours: dark, main, light. */
    public static final int[] LEVITATE = {0x5A3FC4, 0x8A6BFF, 0xB9A6FF};
    /** Off or idle: dark, main, light. */
    public static final int[] OFF = {0x3A3A44, 0x6B6B78, 0x8C8C99};
    /** Gravite metal: deep, dark, mid, light, highlight. */
    public static final int[] METAL = {0x1C1B2A, 0x2B2A3D, 0x3D3B57, 0x55527A, 0x7A77A3};
    /** Text, and the line where two fields meet. */
    public static final int PALE = 0xE8E6FF;

    public static final int DARK = 0;
    public static final int MAIN = 1;
    public static final int LIGHT = 2;

    private Palette() {
    }

    /** The ramp of a polarity. */
    public static int[] of(Polarity polarity) {

        return switch (polarity) {
            case ATTRACT -> ATTRACT;
            case REPEL -> REPEL;
            case LEVITATE -> LEVITATE;
        };
    }

    /** The colour with an alpha of 0..1 (clamped), as 0xAARRGGBB. */
    public static int argb(int rgb, double alpha) {

        final int a = (int) Math.round(Math.max(0, Math.min(1, alpha)) * 255);
        return a << 24 | rgb & 0xFFFFFF;
    }

    /** Every channel multiplied by {@code k} (0..1); for additive glow, where darker means fainter. */
    public static int scale(int rgb, double k) {

        final double f = Math.max(0, Math.min(1, k));
        final int r = (int) ((rgb >> 16 & 0xFF) * f);
        final int g = (int) ((rgb >> 8 & 0xFF) * f);
        final int b = (int) ((rgb & 0xFF) * f);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** Straight mix of two colours, {@code t} = 0 gives the first. */
    public static int mix(int from, int to, double t) {

        final double f = Math.max(0, Math.min(1, t));
        int out = 0;
        for (int shift = 0; shift <= 16; shift += 8) {
            final int a = from >> shift & 0xFF;
            final int b = to >> shift & 0xFF;
            out |= (int) Math.round(a + (b - a) * f) << shift;
        }
        return out;
    }
}
