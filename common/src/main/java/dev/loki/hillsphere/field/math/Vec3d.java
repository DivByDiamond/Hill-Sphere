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
package dev.loki.hillsphere.field.math;

/** Immutable 3D vector. The field math is kept free of Minecraft types so it can be unit tested. */
public record Vec3d(double x, double y, double z) {

    public static final Vec3d ZERO = new Vec3d(0, 0, 0);

    /** Vanilla gravity direction. */
    public static final Vec3d DOWN = new Vec3d(0, -1, 0);

    public Vec3d add(Vec3d o) {

        return new Vec3d(x + o.x, y + o.y, z + o.z);
    }

    public Vec3d sub(Vec3d o) {

        return new Vec3d(x - o.x, y - o.y, z - o.z);
    }

    public Vec3d scale(double k) {

        return new Vec3d(x * k, y * k, z * k);
    }

    public double dot(Vec3d o) {

        return x * o.x + y * o.y + z * o.z;
    }

    public Vec3d cross(Vec3d o) {

        return new Vec3d(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }

    public double length() {

        return Math.sqrt(dot(this));
    }

    /** Unit vector, or {@link #ZERO} if this vector has no length. */
    public Vec3d normalized() {

        final double len = length();
        return len < 1e-12 ? ZERO : scale(1 / len);
    }

    /**
     * Spherical interpolation between two unit vectors. For (nearly) opposite vectors the
     * arc is ambiguous, so a fixed perpendicular is chosen to keep the result deterministic.
     */
    public static Vec3d slerp(Vec3d a, Vec3d b, double t) {

        if (t <= 0) {
            return a;
        }
        if (t >= 1) {
            return b;
        }
        final double dot = Math.max(-1, Math.min(1, a.dot(b)));
        if (dot > 0.9995) {
            return a.scale(1 - t).add(b.scale(t)).normalized();
        }
        final Vec3d side;
        if (dot < -0.9995) {
            side = perpendicular(a);
        } else {
            side = b.sub(a.scale(dot)).normalized();
        }
        final double theta = (dot < -0.9995 ? Math.PI : Math.acos(dot)) * t;
        return a.scale(Math.cos(theta)).add(side.scale(Math.sin(theta)));
    }

    private static Vec3d perpendicular(Vec3d v) {

        final Vec3d axis = Math.abs(v.x) < 0.9 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
        return v.cross(axis).normalized();
    }
}
