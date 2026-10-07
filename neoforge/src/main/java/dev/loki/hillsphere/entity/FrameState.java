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

import dev.loki.hillsphere.field.frame.GravityFrame;

/** What every entity remembers about the field it is in; added to Entity by a mixin. */
public interface FrameState {

    /** Where "down" is for the entity this tick. */
    GravityFrame hillsphereFrame();

    /** How strong the pull is, in multiples of vanilla gravity. */
    double hillsphereStrength();

    /** Looks the field up again; called once per tick, so everything in the tick agrees. */
    void hillsphereRefresh();

    /** Starts or ends a stretch of vanilla code that thinks in the virtual frame. */
    void hillsphereInside(boolean inside);
}
