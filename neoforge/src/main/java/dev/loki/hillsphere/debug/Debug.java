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
package dev.loki.hillsphere.debug;

import dev.loki.hillsphere.Constants;
import dev.loki.hillsphere.config.HillSphereConfig;

import java.util.HashMap;
import java.util.Map;

/** The detailed log of the {@code debug} config switch: every line starts with [HSDBG]. */
public final class Debug {

    private static final Map<String, Long> LAST = new HashMap<>();

    private Debug() {
    }

    public static boolean on() {

        return HillSphereConfig.debug();
    }

    /** Writes one line, formatted like slf4j does with {}. */
    public static void log(String message, Object... args) {

        if (on()) {
            Constants.LOG.info("[HSDBG] " + message, args);
        }
    }

    /** Like {@link #log} but at most once every {@code period} ticks for the same key, so a retry each tick does not flood the log. */
    public static void every(String key, long time, int period, String message, Object... args) {

        if (!on()) {
            return;
        }
        synchronized (LAST) {
            final Long last = LAST.get(key);
            if (last != null && time - last < period && time >= last) {
                return;
            }
            LAST.put(key, time);
        }
        log(message, args);
    }
}
