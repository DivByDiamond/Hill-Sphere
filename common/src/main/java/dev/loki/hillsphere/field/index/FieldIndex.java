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
package dev.loki.hillsphere.field.index;

import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spatial index of cores on a uniform grid, so a query at a point looks at one cell instead
 * of every core in the world. A core is stored in every cell its bounding box touches, so a
 * point lookup never misses it. The index is not thread safe; use it from the server thread.
 *
 * @param <K> key that identifies a core, usually its block position
 */
public final class FieldIndex<K> {

    private final double cellSize;
    private final int maxCores;
    private final Map<K, Entry> entries = new HashMap<>();
    private final Map<Cell, List<K>> cells = new HashMap<>();

    /**
     * @param cellSize edge of a grid cell in blocks; the max field radius works well
     * @param maxCores most cores the index will hold
     */
    public FieldIndex(double cellSize, int maxCores) {

        if (cellSize <= 0 || maxCores <= 0) {
            throw new IllegalArgumentException("cellSize and maxCores must be positive");
        }
        this.cellSize = cellSize;
        this.maxCores = maxCores;
    }

    public int size() {

        return entries.size();
    }

    /**
     * Adds a core or updates the existing one with the same key. A core with no radius is
     * the same as a stopped one and is removed from the index.
     *
     * @return false if the core is new and the index is already full
     */
    public boolean put(K key, CoreField field) {

        if (field.radius() <= 0) {
            remove(key);
            return true;
        }
        final Entry old = entries.get(key);
        if (old == null && entries.size() >= maxCores) {
            return false;
        }
        if (old != null) {
            unlink(key, old);
        }
        final Entry entry = new Entry(field, cellsOf(field));
        entries.put(key, entry);
        for (final Cell cell : entry.cells) {
            cells.computeIfAbsent(cell, c -> new ArrayList<>()).add(key);
        }
        return true;
    }

    public void remove(K key) {

        final Entry old = entries.remove(key);
        if (old != null) {
            unlink(key, old);
        }
    }

    /** Cores whose bounding box covers the cell of the point. They may still not reach the point itself. */
    public List<CoreField> candidatesAt(Vec3d point) {

        final List<K> keys = cells.get(cellOf(point));
        if (keys == null) {
            return List.of();
        }
        final List<CoreField> result = new ArrayList<>(keys.size());
        for (final K key : keys) {
            result.add(entries.get(key).field);
        }
        return result;
    }

    private void unlink(K key, Entry entry) {

        for (final Cell cell : entry.cells) {
            final List<K> keys = cells.get(cell);
            if (keys != null) {
                keys.remove(key);
                if (keys.isEmpty()) {
                    cells.remove(cell);
                }
            }
        }
    }

    private List<Cell> cellsOf(CoreField f) {

        final Cell lo = cellOf(new Vec3d(f.center().x() - f.radius(), f.center().y() - f.radius(), f.center().z() - f.radius()));
        final Cell hi = cellOf(new Vec3d(f.center().x() + f.radius(), f.center().y() + f.radius(), f.center().z() + f.radius()));
        final List<Cell> result = new ArrayList<>();
        for (int x = lo.x; x <= hi.x; x++) {
            for (int y = lo.y; y <= hi.y; y++) {
                for (int z = lo.z; z <= hi.z; z++) {
                    result.add(new Cell(x, y, z));
                }
            }
        }
        return result;
    }

    private Cell cellOf(Vec3d p) {

        return new Cell((int) Math.floor(p.x() / cellSize), (int) Math.floor(p.y() / cellSize), (int) Math.floor(p.z() / cellSize));
    }

    private record Cell(int x, int y, int z) {
    }

    private record Entry(CoreField field, List<Cell> cells) {
    }
}
