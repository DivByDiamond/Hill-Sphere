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

import java.util.function.Predicate;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client config of the boundary layer ({@code hillsphere-shell.toml}). Settings that follow the preset hold a
 * marker instead of a value: below 0 for decimals, 0 for counts, {@code PRESET} for switches, empty for colours.
 */
public final class ShellConfig {

    /** A switch that either follows the preset or is forced. */
    public enum Switch {
        PRESET, ON, OFF;

        boolean or(boolean preset) {

            return this == PRESET ? preset : this == ON;
        }
    }

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.EnumValue<ShellPreset> PRESET;
    public static final ModConfigSpec.ConfigValue<String> EDGE_COLOUR;
    public static final ModConfigSpec.ConfigValue<String> PLATEAU_COLOUR;
    public static final ModConfigSpec.ConfigValue<String> BOUNDARY_COLOUR;
    public static final ModConfigSpec.DoubleValue EDGE_ALPHA;
    public static final ModConfigSpec.DoubleValue PLATEAU_ALPHA;
    public static final ModConfigSpec.DoubleValue BOUNDARY_ALPHA;
    public static final ModConfigSpec.IntValue EDGE_MERIDIANS;
    public static final ModConfigSpec.IntValue EDGE_PARALLELS;
    public static final ModConfigSpec.IntValue PLATEAU_MERIDIANS;
    public static final ModConfigSpec.IntValue PLATEAU_PARALLELS;
    public static final ModConfigSpec.DoubleValue SPIN;
    public static final ModConfigSpec.EnumValue<Switch> SHOW_PLATEAU;
    public static final ModConfigSpec.IntValue RINGS;
    public static final ModConfigSpec.IntValue RIBS;
    public static final ModConfigSpec.EnumValue<Switch> PULSE;
    public static final ModConfigSpec.DoubleValue PULSE_DEPTH;
    public static final ModConfigSpec.DoubleValue PULSE_PERIOD;
    public static final ModConfigSpec.EnumValue<Switch> WAVE;
    public static final ModConfigSpec.DoubleValue WAVE_DEPTH;
    public static final ModConfigSpec.DoubleValue WAVE_LENGTH;
    public static final ModConfigSpec.DoubleValue WAVE_SPEED;
    public static final ModConfigSpec.EnumValue<Switch> FLICKER;
    public static final ModConfigSpec.DoubleValue FLICKER_DEPTH;
    public static final ModConfigSpec.EnumValue<Switch> ENTRY;
    public static final ModConfigSpec.DoubleValue ENTRY_TICKS;

    private static final Predicate<Object> COLOUR = o -> o instanceof String s && (s.isEmpty() || ShellStyle.parse(s) >= 0);

    static {
        final ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Look of the Boundaries layer of the goggles: field edges and where two fields meet. "
                + "Changes apply at once, without a restart").push("shell");
        PRESET = b.comment("Base look: NORMAL (as always), THIN, BRIGHT, MINIMAL (standing still, no effects). "
                + "Any value below that is not left at its marker overrides the preset").defineEnum("preset", ShellPreset.NORMAL);

        b.comment("Outer sphere: the edge of the field").push("edge");
        EDGE_COLOUR = b.comment("Empty: colour of the field's sign. Or a custom colour such as #FF8800")
                .define("colour", "", COLOUR);
        EDGE_ALPHA = b.comment("Brightness of the lines, 0..1; -1 follows the preset").defineInRange("alpha", -1.0, -1.0, 1.0);
        EDGE_MERIDIANS = b.comment("Lines from pole to pole; 0 follows the preset").defineInRange("meridians", 0, 0, 64);
        EDGE_PARALLELS = b.comment("Rings around the vertical axis; 0 follows the preset").defineInRange("parallels", 0, 0, 64);
        b.pop();

        b.comment("Inner sphere: where the field is at full strength (the plateau)").push("plateau");
        SHOW_PLATEAU = b.comment("Draw it at all").defineEnum("show", Switch.PRESET);
        PLATEAU_COLOUR = b.comment("Empty: a darker shade of the field's sign. Or a custom colour such as #FF8800")
                .define("colour", "", COLOUR);
        PLATEAU_ALPHA = b.comment("Brightness of the lines, 0..1; -1 follows the preset").defineInRange("alpha", -1.0, -1.0, 1.0);
        PLATEAU_MERIDIANS = b.comment("Lines from pole to pole; 0 follows the preset").defineInRange("meridians", 0, 0, 64);
        PLATEAU_PARALLELS = b.comment("Rings around the vertical axis; 0 follows the preset").defineInRange("parallels", 0, 0, 64);
        b.pop();

        SPIN = b.comment("Turning speed of both spheres in radians a tick (0 stands still); -1 follows the preset")
                .defineInRange("spin", -1.0, -1.0, 0.05);

        b.comment("Rings and ribs where two pulling fields meet").push("boundary");
        BOUNDARY_COLOUR = b.comment("Empty: pale. Or a custom colour such as #FFFFFF").define("colour", "", COLOUR);
        BOUNDARY_ALPHA = b.comment("Brightness of the lines, 0..1; -1 follows the preset").defineInRange("alpha", -1.0, -1.0, 1.0);
        RINGS = b.comment("Rings across the surface; 0 follows the preset").defineInRange("rings", 0, 0, 32);
        RIBS = b.comment("Ribs along the surface; 0 follows the preset").defineInRange("ribs", 0, 0, 48);
        b.pop();

        b.comment("Effects; each one is switched on or off on its own").push("effects");
        PULSE = b.comment("Brightness breathes").defineEnum("pulse", Switch.PRESET);
        PULSE_DEPTH = b.comment("How far the pulse dims the lines, 0..1").defineInRange("pulseDepth", 0.35, 0.0, 1.0);
        PULSE_PERIOD = b.comment("Ticks of one breath").defineInRange("pulsePeriod", 60.0, 5.0, 600.0);
        WAVE = b.comment("A bright band runs up the lines").defineEnum("wave", Switch.PRESET);
        WAVE_DEPTH = b.comment("How far the wave dims the lines outside the band, 0..1").defineInRange("waveDepth", 0.6, 0.0, 1.0);
        WAVE_LENGTH = b.comment("Blocks between two bands").defineInRange("waveLength", 4.0, 0.5, 64.0);
        WAVE_SPEED = b.comment("Bands passing a point per tick; negative runs down").defineInRange("waveSpeed", 0.02, -0.5, 0.5);
        FLICKER = b.comment("Lines flicker when you stand near the edge of a field").defineEnum("flicker", Switch.PRESET);
        FLICKER_DEPTH = b.comment("How far the flicker dims the lines, 0..1").defineInRange("flickerDepth", 0.5, 0.0, 1.0);
        ENTRY = b.comment("A field's lines fade in when it comes into range").defineEnum("entry", Switch.PRESET);
        ENTRY_TICKS = b.comment("Ticks the fade-in lasts").defineInRange("entryTicks", 20.0, 1.0, 200.0);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private ShellConfig() {
    }
}
