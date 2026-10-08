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
package dev.loki.hillsphere.client.goggles.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.world.phys.Vec3;

/**
 * Draws lines and quads given in world coordinates. Positions are made relative to the camera in doubles
 * before they become floats, so drawing stays exact far from the world origin. Colours are 0xAARRGGBB.
 */
public final class Brush {

    private final VertexConsumer lines;
    private final VertexConsumer fills;
    private final PoseStack.Pose pose;
    private final Vec3 camera;
    private int budget;

    Brush(VertexConsumer lines, VertexConsumer fills, PoseStack.Pose pose, Vec3 camera, int budget) {

        this.lines = lines;
        this.fills = fills;
        this.pose = pose;
        this.camera = camera;
        this.budget = budget;
    }

    public Vec3 camera() {

        return camera;
    }

    /** False once the frame's share of shapes is used up; layers stop drawing then. */
    public boolean hasBudget() {

        return budget > 0;
    }

    /** A line whose colour runs from {@code fromColour} to {@code toColour}. */
    public void line(Vec3 from, Vec3 to, int fromColour, int toColour) {

        final Vec3 d = to.subtract(from);
        final double length = d.length();
        if (length < 1e-6 || (fromColour | toColour) >>> 24 == 0) {
            return;
        }
        budget--;
        final float nx = (float) (d.x / length);
        final float ny = (float) (d.y / length);
        final float nz = (float) (d.z / length);
        lines.addVertex(pose, rel(from.x, camera.x), rel(from.y, camera.y), rel(from.z, camera.z)).setColor(fromColour)
                .setNormal(pose, nx, ny, nz);
        lines.addVertex(pose, rel(to.x, camera.x), rel(to.y, camera.y), rel(to.z, camera.z)).setColor(toColour)
                .setNormal(pose, nx, ny, nz);
    }

    /** A flat four-cornered patch, corners in order around it. */
    public void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int colour) {

        if (colour >>> 24 == 0) {
            return;
        }
        budget--;
        for (final Vec3 p : new Vec3[] {a, b, c, d}) {
            fills.addVertex(pose, rel(p.x, camera.x), rel(p.y, camera.y), rel(p.z, camera.z)).setColor(colour);
        }
    }

    /** 0 below {@code from}, 1 above {@code to}, smooth in between. */
    public static double smoothstep(double from, double to, double x) {

        final double t = Math.max(0, Math.min(1, (x - from) / (to - from)));
        return t * t * (3 - 2 * t);
    }

    /** Fades shapes out towards the range and right in front of the eyes. */
    public static double fadeWithDistance(double distance, double range) {

        return (1 - smoothstep(range * 0.65, range, distance)) * smoothstep(0.6, 1.5, distance);
    }

    private static float rel(double value, double origin) {

        return (float) (value - origin);
    }
}
