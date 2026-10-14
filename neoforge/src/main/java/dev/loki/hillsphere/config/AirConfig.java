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
package dev.loki.hillsphere.config;

import dev.loki.hillsphere.Constants;
import dev.loki.hillsphere.field.control.AirControl;

import java.util.concurrent.atomic.AtomicReference;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The air-control part of the server config: steering and drag for a player with nothing to push off. */
final class AirConfig {

    private static final AtomicReference<AirControl> CURRENT = new AtomicReference<>(AirControl.DEFAULT);

    private static ModConfigSpec.DoubleValue edgeThrust;
    private static ModConfigSpec.DoubleValue coreShare;
    private static ModConfigSpec.DoubleValue drag;

    private AirConfig() {
    }

    /** Adds the entries to the config being built. */
    static void define(ModConfigSpec.Builder b) {

        final AirControl d = AirControl.DEFAULT;
        b.comment("Steering in free flight inside a field (no floor within 1.5 blocks): movement keys thrust along the look, jump and sneak along up").push("airControl");
        edgeThrust = b.comment("Thrust per tick at the edge of a field (vanilla air control is 0.02)")
                .defineInRange("edgeThrust", d.edgeThrust(), 0.0, 1.0);
        coreShare = b.comment("Share of that thrust left in the middle of a field; below 1 so it is hard to leave from there")
                .defineInRange("coreShare", d.coreShare(), 0.0, 1.0);
        drag = b.comment("Share of speed lost per tick in the middle of a field, fading out to the edge; damps swinging")
                .defineInRange("drag", d.drag(), 0.0, 1.0);
        b.pop();
    }

    /** Reads the values after the config loads. */
    static void load() {

        try {
            CURRENT.set(new AirControl(edgeThrust.get(), coreShare.get(), drag.get()));
        } catch (IllegalArgumentException e) {
            Constants.LOG.warn("Hill Sphere air control config is inconsistent ({}), using defaults", e.getMessage());
            CURRENT.set(AirControl.DEFAULT);
        }
    }

    static AirControl current() {

        return CURRENT.get();
    }
}
