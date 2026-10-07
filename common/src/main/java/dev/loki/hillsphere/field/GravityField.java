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
package dev.loki.hillsphere.field;

import dev.loki.hillsphere.field.index.FieldIndex;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;
import dev.loki.hillsphere.field.resolve.Gravity;
import dev.loki.hillsphere.field.resolve.GravityResolver;

/**
 * All cores of one world and the gravity they make. This is the single entry point the
 * game side calls: put a core when it changes, remove it when it stops, ask for gravity at a point.
 *
 * @param <K> key that identifies a core, usually its block position
 */
public final class GravityField<K> {

    private final FieldIndex<K> index;
    private final GravityResolver resolver;

    public GravityField(FieldTuning tuning, int maxCores) {

        this.index = new FieldIndex<>(tuning.maxRadius(), maxCores);
        this.resolver = new GravityResolver(tuning);
    }

    /** @return false if the core could not be added because the world is at its core limit */
    public boolean put(K key, CoreField field) {

        return index.put(key, field);
    }

    public void remove(K key) {

        index.remove(key);
    }

    public int size() {

        return index.size();
    }

    public Gravity gravityAt(Vec3d point) {

        return resolver.resolve(index.candidatesAt(point), point);
    }
}
