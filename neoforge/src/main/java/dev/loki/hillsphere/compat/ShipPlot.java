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
package dev.loki.hillsphere.compat;

import dev.loki.hillsphere.field.math.Vec3d;

import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;

/**
 * The blocks of one ship as the ship mod keeps them, in the ship's own space, and how that space sits in the world
 * now: where a point goes (and comes from), and how a direction turns either way. {@code min} and {@code max} hold every block.
 */
public record ShipPlot(BlockPos min, BlockPos max, UnaryOperator<Vec3d> toWorld, UnaryOperator<Vec3d> toLocal, UnaryOperator<Vec3d> dirToLocal,
        UnaryOperator<Vec3d> dirToWorld) {
}
