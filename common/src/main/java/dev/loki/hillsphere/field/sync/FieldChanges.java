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
package dev.loki.hillsphere.field.sync;

import dev.loki.hillsphere.field.resolve.CoreField;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Remembers the cores of one world and what changed since the last send, so only changes
 * go over the network. A removed core is reported as a core with no radius.
 *
 * @param <K> key that identifies a core, usually its block position
 */
public final class FieldChanges<K> {

    private final Map<K, CoreField> current = new HashMap<>();
    private final Map<K, CoreField> pending = new LinkedHashMap<>();

    /** Records the latest state of a core; a core with no radius counts as removed. */
    public void put(K key, CoreField field) {

        if (field.radius() <= 0) {
            remove(key);
            return;
        }
        if (!field.equals(current.put(key, field))) {
            pending.put(key, field);
        }
    }

    public void remove(K key) {

        final CoreField old = current.remove(key);
        if (old != null) {
            pending.put(key, new CoreField(old.center(), 0, 0, old.polarity()));
        }
    }

    /** Takes the changes since the last call and forgets them. */
    public Map<K, CoreField> drain() {

        final Map<K, CoreField> changes = new LinkedHashMap<>(pending);
        pending.clear();
        return changes;
    }

    /** Every core that is active now, for a player who has just joined. */
    public Map<K, CoreField> snapshot() {

        return new HashMap<>(current);
    }
}
