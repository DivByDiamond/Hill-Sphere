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
package dev.loki.hillsphere.client.goggles.shell.look;

/** Ready-made looks of the boundary layer; a value set in the config overrides the preset's. */
public enum ShellPreset {

    /** The look the layer always had. */
    NORMAL(new Values(0.45, 0.3, 12, 5, 20, 9, 0.002, true, 0.6, 8, 10, false, false, false, false)),
    THIN(new Values(0.28, 0.18, 8, 4, 12, 6, 0.002, true, 0.4, 6, 6, false, false, false, false)),
    BRIGHT(new Values(0.7, 0.5, 16, 7, 28, 12, 0.003, true, 0.85, 10, 14, true, true, false, false)),
    /** Same lines as normal, standing still and without any effect. */
    MINIMAL(new Values(0.45, 0.3, 12, 5, 20, 9, 0, true, 0.6, 8, 10, false, false, false, false));

    private final Values values;

    ShellPreset(Values values) {

        this.values = values;
    }

    public Values look() {

        return values;
    }

    /**
     * Alphas are 0..1 before the global opacity, spin is radians a tick, rings and ribs belong to the
     * balance surface of two fields.
     */
    public record Values(double edgeAlpha, double plateauAlpha, int edgeMeridians, int edgeParallels,
            int plateauMeridians, int plateauParallels, double spin, boolean showPlateau, double boundaryAlpha,
            int rings, int ribs, boolean pulse, boolean wave, boolean flicker, boolean entry) {
    }
}
