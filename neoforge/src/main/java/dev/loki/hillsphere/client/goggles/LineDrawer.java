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
package dev.loki.hillsphere.client.goggles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Draws coloured lines and simple shapes into a line buffer. */
final class LineDrawer {

    private final VertexConsumer buffer;
    private final PoseStack.Pose pose;
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;

    LineDrawer(VertexConsumer buffer, PoseStack.Pose pose, float[] rgb, float alpha) {

        this.buffer = buffer;
        this.pose = pose;
        this.red = rgb[0];
        this.green = rgb[1];
        this.blue = rgb[2];
        this.alpha = alpha;
    }

    void line(Vec3 from, Vec3 to) {

        final Vec3 n = to.subtract(from).normalize();
        vertex(from, n);
        vertex(to, n);
    }

    /** An arrow from a point along a direction: a shaft and two short barbs. */
    void arrow(Vec3 from, Vec3 direction) {

        final Vec3 tip = from.add(direction);
        line(from, tip);
        if (direction.lengthSqr() < 1e-6) {
            return;
        }
        final Vec3 back = direction.normalize().scale(-0.25 * Math.min(1, direction.length() * 2));
        final Vec3 side = perpendicular(direction).scale(0.12);
        line(tip, tip.add(back).add(side));
        line(tip, tip.add(back).subtract(side));
    }

    /** The outline of a square lying on a block face; {@code half} is half its edge. */
    void square(Vec3 center, Direction face, double half) {

        final Vec3 u = tangent(face, 0).scale(half);
        final Vec3 v = tangent(face, 1).scale(half);
        final Vec3 a = center.add(u).add(v);
        final Vec3 b = center.add(u).subtract(v);
        final Vec3 c = center.subtract(u).subtract(v);
        final Vec3 d = center.subtract(u).add(v);
        line(a, b);
        line(b, c);
        line(c, d);
        line(d, a);
    }

    /** One of the two unit axes along the face. */
    private static Vec3 tangent(Direction face, int index) {

        final Direction.Axis[] axes = switch (face.getAxis()) {
            case X -> new Direction.Axis[] {Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Y};
        };
        return Vec3.atLowerCornerOf(Direction.get(Direction.AxisDirection.POSITIVE, axes[index]).getNormal());
    }

    private void vertex(Vec3 p, Vec3 normal) {

        buffer.addVertex(pose.pose(), (float) p.x, (float) p.y, (float) p.z).setColor(red, green, blue, alpha)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static Vec3 perpendicular(Vec3 v) {

        final Vec3 axis = Math.abs(v.normalize().y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        return v.cross(axis).normalize();
    }
}
