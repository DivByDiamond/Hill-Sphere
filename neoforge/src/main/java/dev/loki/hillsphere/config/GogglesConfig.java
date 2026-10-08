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

import dev.loki.hillsphere.client.goggles.GogglesMode;
import dev.loki.hillsphere.client.goggles.GogglesMode.Layer;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Client config: what the goggles draw, how far, how densely and how strongly. */
public final class GogglesConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.EnumValue<GogglesMode> MODE;
    public static final ModConfigSpec.IntValue RANGE;
    public static final ModConfigSpec.DoubleValue DENSITY;
    public static final ModConfigSpec.DoubleValue OPACITY;
    public static final ModConfigSpec.IntValue SCAN_TICKS;
    public static final ModConfigSpec.IntValue MAX_FACES;
    public static final ModConfigSpec.IntValue MAX_SHELLS;

    private static final Map<Layer, ModConfigSpec.BooleanValue> LAYERS = new EnumMap<>(Layer.class);

    static {
        final ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Hill Goggles").push("goggles");
        MODE = b.comment("Mode the goggles start in; the mode key cycles it and the choice is saved").defineEnum("mode", GogglesMode.ALL);
        b.comment("Layers that may be drawn at all; a mode only shows the enabled ones").push("layers");
        for (final Layer layer : Layer.values()) {
            LAYERS.put(layer, b.define(layer.name().toLowerCase(Locale.ROOT), true));
        }
        b.pop();
        RANGE = b.comment("How far from the wearer streaks and surfaces are drawn, in blocks").defineInRange("range", 12, 4, 32);
        DENSITY = b.comment("How many streaks to draw: 1 is normal, 0.25 sparse, 3 dense").defineInRange("density", 1.0, 0.25, 3.0);
        OPACITY = b.comment("Opacity of everything drawn in the world").defineInRange("opacity", 0.8, 0.1, 1.0);
        SCAN_TICKS = b.comment("Ticks between two scans for surfaces").defineInRange("scanTicks", 10, 2, 100);
        MAX_FACES = b.comment("Most surfaces shown at once").defineInRange("maxFaces", 512, 16, 4096);
        MAX_SHELLS = b.comment("Most cores whose field edge is drawn").defineInRange("maxShells", 4, 1, 16);
        b.pop();
        SPEC = b.build();
    }

    private GogglesConfig() {
    }

    public static boolean enabled(Layer layer) {

        return LAYERS.get(layer).get();
    }
}
