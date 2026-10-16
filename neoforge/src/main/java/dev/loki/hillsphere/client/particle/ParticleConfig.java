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
package dev.loki.hillsphere.client.particle;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client config: how much particles lean toward the field, and how far from the player they do. */
public final class ParticleConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue STRENGTH;
    public static final ModConfigSpec.BooleanValue RAIN_ENABLED;
    public static final ModConfigSpec.DoubleValue RAIN;
    public static final ModConfigSpec.IntValue RANGE;

    static {
        final ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Particles in gravity fields").push("particles");
        ENABLED = b.comment("Particles fall and rise along the field instead of the world's up and down")
                .define("enabled", true);
        STRENGTH = b.comment("How far they follow the field: 0 not at all, 1 fully")
                .defineInRange("strength", 0.6, 0.0, 1.0);
        RAIN_ENABLED = b.comment("Rain and snow lean along the field; false draws them straight down as in vanilla")
                .define("rainEnabled", true);
        RAIN = b.comment("How far rain and snow lean along the field: 0 falls straight down, 1 falls along the field")
                .defineInRange("rain", 0.8, 0.0, 1.0);
        RANGE = b.comment("Particles farther than this from the player are left alone, in blocks")
                .defineInRange("range", 32, 4, 128);
        b.pop();
        SPEC = b.build();
    }

    private ParticleConfig() {
    }
}
