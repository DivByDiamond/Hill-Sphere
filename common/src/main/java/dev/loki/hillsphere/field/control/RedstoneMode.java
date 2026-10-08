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
package dev.loki.hillsphere.field.control;

import dev.loki.hillsphere.field.Polarity;

/** How a core listens to redstone. The panel gives the settings, the signal and this mode give the result. */
public enum RedstoneMode {

    /** Redstone is ignored; the panel decides. */
    MANUAL,

    /** A signal switches the core off. */
    OFF_ON_SIGNAL,

    /** The core works only while there is a signal. */
    ON_WITH_SIGNAL,

    /** The panel level is the ceiling; the signal strength picks the share of it, 0 to 15. */
    ANALOG,

    /** A signal flips attraction into repulsion and back. */
    REVERSE_ON_SIGNAL;

    /** The strongest redstone signal. */
    public static final int MAX_SIGNAL = 15;

    /**
     * @param polarity the polarity on the panel
     * @param level the level on the panel, 0 to the highest level
     * @param signal the redstone signal at the core, 0 to {@value #MAX_SIGNAL}
     */
    public Control apply(Polarity polarity, int level, int signal) {

        final boolean powered = signal > 0;
        return switch (this) {
            case MANUAL -> new Control(polarity, level);
            case OFF_ON_SIGNAL -> new Control(polarity, powered ? 0 : level);
            case ON_WITH_SIGNAL -> new Control(polarity, powered ? level : 0);
            case ANALOG -> new Control(polarity, level * (double) Math.min(signal, MAX_SIGNAL) / MAX_SIGNAL);
            case REVERSE_ON_SIGNAL -> new Control(powered ? polarity.flipped() : polarity, level);
        };
    }
}
