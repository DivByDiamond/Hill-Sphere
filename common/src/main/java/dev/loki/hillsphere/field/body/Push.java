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
 * One force for one physics step: what it pushes, where and how hard, all in world space.
 *
 * @param body the body it acts on
 * @param point where it acts
 * @param force force in mass times blocks per second squared
 * @param kind whether it cancels weight or pulls toward or away from a core
 * @param <B> the game's handle of a body
 */
public record Push<B>(B body, Vec3d point, Vec3d force, Kind kind) {

    /** What made the force, so the game can show and group it. */
    public enum Kind {

        /** A levitation core cancels part of the body's weight. */
        LIFT,

        /** An attracting or repelling core pulls the body, or the pulled body pulls back on the core. */
        PULL
    }
}
