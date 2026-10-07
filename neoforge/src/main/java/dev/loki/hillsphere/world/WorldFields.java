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
package dev.loki.hillsphere.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/** The cores of every loaded world on the server, one {@link LevelFields} per world. */
public final class WorldFields {

    private static final Map<Level, LevelFields> FIELDS = new ConcurrentHashMap<>();

    private WorldFields() {
    }

    public static LevelFields of(Level level) {

        return FIELDS.computeIfAbsent(level, l -> new LevelFields());
    }

    /** The world's cores if it has any yet; unlike {@link #of} it never creates them. */
    public static LevelFields peek(Level level) {

        return FIELDS.get(level);
    }

    /** Forgets a world's cores when it unloads. */
    public static void forget(LevelAccessor level) {

        if (level instanceof Level l) {
            FIELDS.remove(l);
        }
    }

    /** Forgets all cores; they register themselves again on their next tick, with the new tuning. */
    public static void clear() {

        FIELDS.clear();
    }
}
