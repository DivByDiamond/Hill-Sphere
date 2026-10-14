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
package dev.loki.hillsphere.field.shape;

import dev.loki.hillsphere.field.math.Vec3d;

/**
 * The volume of a deck field: a box that may be turned, such as the hull of a ship. Inside it "down" is the same
 * everywhere, along {@code -yAxis}; past its faces the pull fades out over {@link #EDGE} blocks.
 *
 * @param center middle of the box in the world
 * @param xAxis unit vector of the box's own x in the world
 * @param yAxis unit vector of the box's own y in the world: "up" of whatever carries the core
 * @param zAxis unit vector of the box's own z in the world
 * @param half half the size of the box along its own x, y and z
 */
public record Deck(Vec3d center, Vec3d xAxis, Vec3d yAxis, Vec3d zAxis, Vec3d half) {

    /** Blocks past the faces over which the pull fades to nothing. */
    public static final double EDGE = 2;

    private static final Vec3d X = new Vec3d(1, 0, 0);
    private static final Vec3d Y = new Vec3d(0, 1, 0);
    private static final Vec3d Z = new Vec3d(0, 0, 1);

    /** A cube standing square in the world, as a deck core makes on the ground. */
    public static Deck cube(Vec3d center, double half) {

        return new Deck(center, X, Y, Z, new Vec3d(half, half, half));
    }

    /** How far the point is from the box, in blocks; 0 inside it. */
    public double outside(Vec3d point) {

        final Vec3d d = point.sub(center);
        final double x = Math.max(0, Math.abs(d.dot(xAxis)) - half.x());
        final double y = Math.max(0, Math.abs(d.dot(yAxis)) - half.y());
        final double z = Math.max(0, Math.abs(d.dot(zAxis)) - half.z());
        return Math.sqrt(x * x + y * y + z * z);
    }

    /** Falloff in the point: 1 inside, smoothly down to 0 at {@link #EDGE} blocks out. */
    public double fade(Vec3d point) {

        final double t = Math.min(1, outside(point) / EDGE);
        return 1 - t * t * (3 - 2 * t);
    }

    /** How far from {@code from} the field can reach at most, fading edge included: for a spatial index. */
    public double reachFrom(Vec3d from) {

        return center.sub(from).length() + half.length() + EDGE;
    }
}
