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
package dev.loki.hillsphere.field.body;

import dev.loki.hillsphere.field.math.Vec3d;

/**
 * A rigid body the field can move, such as a ship: where its mass sits and how it falls.
 *
 * @param id what the game knows the body by
 * @param center centre of mass in world space
 * @param mass total mass
 * @param gravity the acceleration the body already falls with, in world space
 * @param <B> the game's handle of a body
 */
public record Body<B>(B id, Vec3d center, double mass, Vec3d gravity) {
}
