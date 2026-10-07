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
package dev.loki.hillsphere.field.frame;

import dev.loki.hillsphere.field.math.Vec3d;

/**
 * One of the six ways "down" can point, as a quarter-turn rotation of the world. Vanilla movement code only
 * knows "down" along -y, so it runs in the <em>virtual</em> frame, where down is always -y, and the results
 * are turned into the <em>real</em> world with {@link #toReal}. Each frame lists where the virtual x, y and
 * z axes end up in the real world; the turns are exact, with no rounding.
 */
public enum GravityFrame {

    /** Vanilla: down is -y. */
    DOWN(new Vec3d(1, 0, 0), new Vec3d(0, 1, 0), new Vec3d(0, 0, 1)),
    /** Down is +y: the player stands on a ceiling. */
    UP(new Vec3d(1, 0, 0), new Vec3d(0, -1, 0), new Vec3d(0, 0, -1)),
    /** Down is +x. */
    EAST(new Vec3d(0, 1, 0), new Vec3d(-1, 0, 0), new Vec3d(0, 0, 1)),
    /** Down is -x. */
    WEST(new Vec3d(0, -1, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1)),
    /** Down is +z. */
    SOUTH(new Vec3d(1, 0, 0), new Vec3d(0, 0, -1), new Vec3d(0, 1, 0)),
    /** Down is -z. */
    NORTH(new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), new Vec3d(0, -1, 0));

    private final Vec3d x;
    private final Vec3d y;
    private final Vec3d z;

    GravityFrame(Vec3d x, Vec3d y, Vec3d z) {

        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** The frame whose "down" is closest to the given direction; vanilla for a zero direction. */
    public static GravityFrame of(Vec3d down) {

        final double ax = Math.abs(down.x());
        final double ay = Math.abs(down.y());
        final double az = Math.abs(down.z());
        if (ay >= ax && ay >= az) {
            return down.y() > 0 ? UP : DOWN;
        }
        if (ax >= az) {
            return down.x() > 0 ? EAST : WEST;
        }
        return down.z() > 0 ? SOUTH : NORTH;
    }

    public boolean isVanilla() {

        return this == DOWN;
    }

    /** A vector in the virtual frame, as it is in the real world. */
    public Vec3d toReal(Vec3d v) {

        return x.scale(v.x()).add(y.scale(v.y())).add(z.scale(v.z()));
    }

    /** A real-world vector, as it is in the virtual frame. */
    public Vec3d toVirtual(Vec3d v) {

        return new Vec3d(x.dot(v), y.dot(v), z.dot(v));
    }

    /** Where virtual x, y and z point in the real world, for building a rotation. */
    public Vec3d[] axes() {

        return new Vec3d[] {x, y, z};
    }
}
