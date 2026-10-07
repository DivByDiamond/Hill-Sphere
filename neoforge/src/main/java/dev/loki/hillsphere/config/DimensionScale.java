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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** How much stronger or weaker the pull is in each dimension, from entries like {@code minecraft:the_end=1.5}. */
public final class DimensionScale {

    public static final List<String> DEFAULTS = List.of("minecraft:the_nether=0.6", "minecraft:the_end=1.5");

    private static final AtomicReference<Map<ResourceLocation, Double>> SCALES = new AtomicReference<>(parse(DEFAULTS));

    private DimensionScale() {
    }

    public static void load(List<? extends String> entries) {

        SCALES.set(parse(entries));
    }

    /** The multiplier for the level's dimension; 1 if it is not listed. */
    public static double of(Level level) {

        return SCALES.get().getOrDefault(level.dimension().location(), 1.0);
    }

    /** Bad entries are skipped with a warning, so one typo does not switch the rest off. */
    private static Map<ResourceLocation, Double> parse(List<? extends String> entries) {

        final Map<ResourceLocation, Double> result = new HashMap<>();
        for (final String entry : entries) {
            final int split = entry.lastIndexOf('=');
            final ResourceLocation id = split > 0 ? ResourceLocation.tryParse(entry.substring(0, split).trim()) : null;
            final double scale = split > 0 ? number(entry.substring(split + 1)) : Double.NaN;
            if (id == null || !(scale >= 0)) {
                Constants.LOG.warn("Hill Sphere: ignoring dimension entry '{}', expected namespace:name=multiplier", entry);
            } else {
                result.put(id, scale);
            }
        }
        return Map.copyOf(result);
    }

    private static double number(String text) {

        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
