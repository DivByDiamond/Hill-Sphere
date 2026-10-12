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
package dev.loki.hillsphere.entity;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.AABB;

/** The lifts along the frame's "up" that would sit a box on the colliders around it, as vanilla collects them. */
public final class StepHeights {

    private StepHeights() {
    }

    /**
     * How far the box must travel along {@code up} to rest on each collider face, keeping only the offers
     * vanilla keeps: at or above the box, not the lift it already has, and no higher than a step.
     *
     * @param axis  the world axis {@code up} points along; {@code sign} is its direction there
     */
    public static float[] of(AABB risen, List<VoxelShape> colliders, float step, float done, Direction.Axis axis, double sign) {

        final double low = sign > 0 ? edge(risen, axis, false) : edge(risen, axis, true);
        final Set<Float> seen = new TreeSet<>();
        for (VoxelShape shape : colliders) {
            for (double coord : shape.getCoords(axis)) {
                final float offer = (float) (sign * (coord - low));
                if (offer < 0.0F || offer == done || offer > step) {
                    continue;
                }
                seen.add(offer);
            }
        }
        final float[] offers = new float[seen.size()];
        int i = 0;
        for (Float offer : seen) {
            offers[i++] = offer;
        }
        return offers;
    }

    private static double edge(AABB box, Direction.Axis axis, boolean top) {

        return switch (axis) {
            case X -> top ? box.maxX : box.minX;
            case Y -> top ? box.maxY : box.minY;
            case Z -> top ? box.maxZ : box.minZ;
        };
    }
}
