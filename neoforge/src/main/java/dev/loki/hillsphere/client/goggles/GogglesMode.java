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

import dev.loki.hillsphere.config.GogglesConfig;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * What the goggles show, switched round with a key. Each mode picks the world layers; the screen layers are on
 * in every mode. A layer also has to be enabled in the client config to be drawn.
 */
public enum GogglesMode {

    ALL(Layer.FLOW, Layer.SURFACES, Layer.BOUNDS),
    FLOW(Layer.FLOW),
    SURFACES(Layer.SURFACES),
    BOUNDS(Layer.BOUNDS),
    SCREEN_ONLY();

    /** One thing the goggles can draw. */
    public enum Layer {
        /** Moving streaks along "down" around the wearer. */
        FLOW,
        /** Block faces you could stand on. */
        SURFACES,
        /** Field edges, plateaus and the line between two fields. */
        BOUNDS,
        /** Numbers of the core under the crosshair. */
        PANEL,
        /** The "down" arrow in the corner. */
        COMPASS,
        /** Tint at the screen edges while inside a field. */
        VIGNETTE
    }

    private final Set<Layer> layers;

    GogglesMode(Layer... world) {

        final EnumSet<Layer> all = EnumSet.of(Layer.PANEL, Layer.COMPASS, Layer.VIGNETTE);
        all.addAll(Set.of(world));
        this.layers = all;
    }

    public boolean shows(Layer layer) {

        return layers.contains(layer) && GogglesConfig.enabled(layer);
    }

    public GogglesMode next() {

        final GogglesMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public String translationKey() {

        return "hillsphere.goggles.mode." + name().toLowerCase(Locale.ROOT);
    }
}
