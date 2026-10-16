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

import dev.loki.hillsphere.client.goggles.shell.look.ShellPreset.Values;

/** The boundary look for this frame: the preset with the config's own values laid over it. */
public record ShellStyle(Values look, int edgeColour, int plateauColour, int boundaryColour, Fx fx) {

    /** Colour value meaning "take the colour of the field's sign". */
    public static final int BY_SIGN = -1;

    /** Effect settings; an effect is applied only when its flag is set. */
    public record Fx(boolean pulse, double pulseDepth, double pulsePeriod, boolean wave, double waveDepth,
            double waveLength, double waveSpeed, boolean flicker, double flickerDepth, boolean entry, double entryTicks) {
    }

    /** Reads the config now; cheap enough to call every frame. */
    public static ShellStyle read() {

        final Values p = ShellConfig.PRESET.get().look();
        final Values look = new Values(
                decimal(ShellConfig.EDGE_ALPHA.get(), p.edgeAlpha()), decimal(ShellConfig.PLATEAU_ALPHA.get(), p.plateauAlpha()),
                count(ShellConfig.EDGE_MERIDIANS.get(), p.edgeMeridians()), count(ShellConfig.EDGE_PARALLELS.get(), p.edgeParallels()),
                count(ShellConfig.PLATEAU_MERIDIANS.get(), p.plateauMeridians()),
                count(ShellConfig.PLATEAU_PARALLELS.get(), p.plateauParallels()),
                decimal(ShellConfig.SPIN.get(), p.spin()), ShellConfig.SHOW_PLATEAU.get().or(p.showPlateau()),
                decimal(ShellConfig.BOUNDARY_ALPHA.get(), p.boundaryAlpha()),
                count(ShellConfig.RINGS.get(), p.rings()), count(ShellConfig.RIBS.get(), p.ribs()),
                false, false, false, false);
        final Fx fx = new Fx(ShellConfig.PULSE.get().or(p.pulse()), ShellConfig.PULSE_DEPTH.get(), ShellConfig.PULSE_PERIOD.get(),
                ShellConfig.WAVE.get().or(p.wave()), ShellConfig.WAVE_DEPTH.get(), ShellConfig.WAVE_LENGTH.get(),
                ShellConfig.WAVE_SPEED.get(), ShellConfig.FLICKER.get().or(p.flicker()), ShellConfig.FLICKER_DEPTH.get(),
                ShellConfig.ENTRY.get().or(p.entry()), ShellConfig.ENTRY_TICKS.get());
        return new ShellStyle(look, colour(ShellConfig.EDGE_COLOUR.get()), colour(ShellConfig.PLATEAU_COLOUR.get()),
                colour(ShellConfig.BOUNDARY_COLOUR.get()), fx);
    }

    /** Only the rings and ribs, for the tick that builds the boundary surfaces. */
    public static int rings() {

        return count(ShellConfig.RINGS.get(), ShellConfig.PRESET.get().look().rings());
    }

    /** 0xRRGGBB of {@code #RRGGBB} or {@code RRGGBB}, or -1 when it is not one. */
    static int parse(String text) {

        final String hex = text.startsWith("#") ? text.substring(1) : text;
        if (hex.length() != 6) {
            return BY_SIGN;
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return BY_SIGN;
        }
    }

    private static int colour(String text) {

        return text.isEmpty() ? BY_SIGN : parse(text);
    }

    private static double decimal(double set, double preset) {

        return set < 0 ? preset : set;
    }

    private static int count(int set, int preset) {

        return set <= 0 ? preset : set;
    }
}
