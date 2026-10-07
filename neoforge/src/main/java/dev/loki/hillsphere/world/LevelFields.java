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
import dev.loki.hillsphere.field.sync.FieldChanges;

import net.minecraft.core.BlockPos;

/** The cores of one world: where gravity comes from, and what changed since the last send. */
public final class LevelFields {

    private final GravityField<BlockPos> field =
            new GravityField<>(HillSphereConfig.tuning(), HillSphereConfig.maxCores());
    private final FieldChanges<BlockPos> changes = new FieldChanges<>();

    public GravityField<BlockPos> field() {

        return field;
    }

    public FieldChanges<BlockPos> changes() {

        return changes;
    }

    public void update(BlockPos pos, CoreField core) {

        if (field.put(pos, core)) {
            changes.put(pos, core);
        }
    }

    public void remove(BlockPos pos) {

        field.remove(pos);
        changes.remove(pos);
    }
}
