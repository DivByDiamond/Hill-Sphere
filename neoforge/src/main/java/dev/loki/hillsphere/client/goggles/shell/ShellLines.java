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
package dev.loki.hillsphere.client.goggles.shell;

import dev.loki.hillsphere.client.goggles.Palette;
import dev.loki.hillsphere.client.goggles.render.Brush;

import net.minecraft.world.phys.Vec3;

/** Lines that fade with distance from the camera and with an optional gain that depends on where they are. */
final class ShellLines {

    private static final int SEGMENTS = 48;

    /** Brightness multiplier of a point on a line. */
    @FunctionalInterface
    interface Gain {

        Gain NONE = point -> 1;

        double at(Vec3 point);
    }

    private final Brush brush;
    private final double view;
    private Gain gain = Gain.NONE;

    ShellLines(Brush brush, double view) {

        this.brush = brush;
        this.view = view;
    }

    /** Applies to every line drawn after this call. */
    void gain(Gain gain) {

        this.gain = gain;
    }

    /** Two unit vectors at right angles to the axis and each other. */
    static Vec3[] across(Vec3 axis) {

        final Vec3 helper = Math.abs(axis.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        final Vec3 u = axis.cross(helper).normalize();
        return new Vec3[] {u, axis.cross(u).normalize()};
    }

    void sphere(Vec3 centre, double radius, int meridians, int parallels, double turn, int colour) {

        final Vec3[] flat = {new Vec3(1, 0, 0), new Vec3(0, 0, 1)};
        for (int k = 1; k <= parallels; k++) {
            final double theta = Math.PI * k / (parallels + 1);
            circle(centre.add(0, radius * Math.cos(theta), 0), flat, radius * Math.sin(theta), turn, colour);
        }
        for (int m = 0; m < meridians; m++) {
            final double phi = turn + 2 * Math.PI * m / meridians;
            final Vec3 out = new Vec3(Math.cos(phi), 0, Math.sin(phi));
            Vec3 from = centre.add(0, radius, 0);
            for (int s = 1; s <= SEGMENTS / 2; s++) {
                final double theta = 2 * Math.PI * s / SEGMENTS;
                final Vec3 to = centre.add(out.scale(radius * Math.sin(theta))).add(0, radius * Math.cos(theta), 0);
                line(from, to, colour);
                from = to;
            }
        }
    }

    void circle(Vec3 centre, Vec3[] plane, double radius, double turn, int colour) {

        Vec3 from = null;
        for (int s = 0; s <= SEGMENTS; s++) {
            final double a = turn + 2 * Math.PI * s / SEGMENTS;
            final Vec3 to = centre.add(plane[0].scale(radius * Math.cos(a))).add(plane[1].scale(radius * Math.sin(a)));
            if (from != null) {
                line(from, to, colour);
            }
            from = to;
        }
    }

    void line(Vec3 from, Vec3 to, int colour) {

        brush.line(from, to, fade(from, colour), fade(to, colour));
    }

    private int fade(Vec3 point, int colour) {

        final double distance = point.distanceTo(brush.camera());
        final double k = (1 - Brush.smoothstep(view * 0.5, view, distance)) * Brush.smoothstep(0.5, 2.5, distance);
        return Palette.argb(colour, (colour >>> 24) / 255.0 * k * gain.at(point));
    }
}
