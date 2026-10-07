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
import dev.loki.hillsphere.block.HillCoreBlock;
import dev.loki.hillsphere.field.FieldTuning;

import dev.loki.hillsphere.world.WorldFields;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config: every number that shapes a field. NeoForge syncs it to clients, so
 * players predict movement with the same values as the server.
 */
public final class HillSphereConfig {

    public static final ModConfigSpec SPEC;

    private static final int DEFAULT_MAX_CORES = 256;
    private static final AtomicReference<FieldTuning> CURRENT = new AtomicReference<>(FieldTuning.DEFAULT);
    private static final AtomicInteger MAX_CORES_NOW = new AtomicInteger(DEFAULT_MAX_CORES);

    private static final ModConfigSpec.DoubleValue MIN_RPM;
    private static final ModConfigSpec.DoubleValue MAX_RPM;
    private static final ModConfigSpec.DoubleValue MIN_RADIUS;
    private static final ModConfigSpec.DoubleValue MAX_RADIUS;
    private static final ModConfigSpec.DoubleValue PLATEAU;
    private static final ModConfigSpec.DoubleValue STRESS;
    private static final ModConfigSpec.DoubleValue BLEND_START;
    private static final ModConfigSpec.DoubleValue BLEND_END;
    private static final ModConfigSpec.IntValue RAMP_TICKS;
    private static final ModConfigSpec.IntValue MAX_CORES;
    private static final ModConfigSpec.ConfigValue<List<? extends Double>> STRENGTHS;
    private static final ModConfigSpec.ConfigValue<List<? extends Double>> LEVITATION;

    static {
        final FieldTuning d = FieldTuning.DEFAULT;
        final ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Rotation speed and the size of the field").push("speed");
        MIN_RPM = b.comment("The field is off below this speed (RPM)").defineInRange("minRpm", d.minRpm(), 0.1, 256);
        MAX_RPM = b.comment("Speed at which the radius stops growing (RPM)").defineInRange("maxRpm", d.maxRpm(), 1, 1024);
        MIN_RADIUS = b.comment("Radius at the minimum speed, in blocks").defineInRange("minRadius", d.minRadius(), 0.5, 256);
        MAX_RADIUS = b.comment("Radius at the maximum speed, in blocks").defineInRange("maxRadius", d.maxRadius(), 0.5, 256);
        b.pop();

        b.comment("Pull and levitation levels. Each list entry is one level the player can pick (at most 4 are used).").push("levels");
        STRENGTHS = b.comment("Pull per level, in multiples of vanilla gravity")
                .defineList("strengths", d.levelStrengths(), () -> 1.0, o -> o instanceof Double v && v > 0);
        LEVITATION = b.comment("Share of gravity cancelled per level (1 = weightless, above 1 = lifts)")
                .defineList("levitation", d.levitationLevels(), () -> 1.0, o -> o instanceof Double v && v > 0);
        STRESS = b.comment("Stress impact per RPM and per level (SU)").defineInRange("stressPerRpmPerLevel", d.stressPerRpmPerLevel(), 0, 1024);
        b.pop();

        b.comment("Field shape and transitions").push("shape");
        PLATEAU = b.comment("Part of the radius with full pull; the pull fades out beyond it")
                .defineInRange("plateau", d.plateau(), 0, 0.99);
        BLEND_START = b.comment("Pull ratio where two overlapping fields start to blend").defineInRange("blendStart", d.blendStart(), 1, 4);
        BLEND_END = b.comment("Pull ratio where the stronger field wins completely").defineInRange("blendEnd", d.blendEnd(), 1.01, 8);
        RAMP_TICKS = b.comment("Ticks a field takes to grow or fade").defineInRange("rampTicks", d.rampTicks(), 1, 1200);
        b.pop();

        MAX_CORES = b.comment("Most active cores per world; more are ignored").defineInRange("maxCores", DEFAULT_MAX_CORES, 1, 100_000);
        SPEC = b.build();
    }

    private HillSphereConfig() {
    }

    /** The tuning the game uses now: the defaults until the config loads, then rebuilt on every (re)load. */
    public static FieldTuning tuning() {

        return CURRENT.get();
    }

    /** Called when the config loads or reloads. Not when it unloads: its values cannot be read then. */
    public static void onConfig(ModConfigEvent event) {

        if (event.getConfig().getSpec() == SPEC) {
            CURRENT.set(build());
            MAX_CORES_NOW.set(MAX_CORES.get());
            WorldFields.clear();
        }
    }

    /** Builds the tuning; falls back to the defaults if the values contradict each other. */
    private static FieldTuning build() {

        try {
            return new FieldTuning(MIN_RPM.get(), MAX_RPM.get(), MIN_RADIUS.get(), MAX_RADIUS.get(), PLATEAU.get(),
                    levels(STRENGTHS.get()), levels(LEVITATION.get()), STRESS.get(),
                    BLEND_START.get(), BLEND_END.get(), RAMP_TICKS.get());
        } catch (IllegalArgumentException e) {
            Constants.LOG.warn("Hill Sphere config is inconsistent ({}), using defaults", e.getMessage());
            return FieldTuning.DEFAULT;
        }
    }

    /** A core has {@link HillCoreBlock#MAX_LEVEL} levels at most, so longer lists are cut. */
    private static List<Double> levels(List<? extends Double> values) {

        return values.stream().limit(HillCoreBlock.MAX_LEVEL).map(Double::doubleValue).toList();
    }

    public static int maxCores() {

        return MAX_CORES_NOW.get();
    }
}
