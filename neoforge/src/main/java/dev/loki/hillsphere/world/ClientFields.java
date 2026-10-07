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

import dev.loki.hillsphere.config.HillSphereConfig;
import dev.loki.hillsphere.field.GravityField;
import dev.loki.hillsphere.field.resolve.CoreField;

import java.util.Map;
import net.minecraft.core.BlockPos;

/** The cores the client knows about, mirrored from the server so it can predict gravity. */
public final class ClientFields {

    private static GravityField<BlockPos> field;

    private ClientFields() {
    }

    public static GravityField<BlockPos> field() {

        if (field == null) {
            field = create();
        }
        return field;
    }

    /** Applies a sync from the server; a full sync replaces everything the client knew. */
    public static void apply(boolean replace, Map<BlockPos, CoreField> cores) {

        if (replace) {
            field = create();
        }
        cores.forEach(field()::put);
    }

    /** Forgets everything, e.g. when leaving a world. */
    public static void clear() {

        field = null;
    }

    private static GravityField<BlockPos> create() {

        return new GravityField<>(HillSphereConfig.tuning(), HillSphereConfig.maxCores());
    }
}
