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
package dev.loki.hillsphere.field.resolve;

import dev.loki.hillsphere.field.math.Vec3d;

/**
 * Eases the pull an entity feels, one tick at a time: the pull is a vector (direction times strength)
 * and it moves a fixed share of the way to the pull the field gives now. Walking into a field, out of
 * it, or across the seam between two cores then fades the strength in and out and swings the
 * direction round, instead of jumping. The first pull an entity ever feels is taken as it is, so one that
 * appears inside a field (spawns, logs in, changes dimension) is not eased in from vanilla. Two opposing pulls pass through zero, which is weightlessness.
 */
public final class PullSmoother {

    /** Share of the remaining distance covered each tick: about half a second to settle. */
    private static final double RATE = 0.1;

    /** Close enough to the target to land on it, so a settled pull is exactly the field's. */
    private static final double SETTLED = 1e-3;

    private Vec3d pull = Vec3d.DOWN;
    private Vec3d direction = Vec3d.DOWN;
    private boolean started;

    /** The eased pull towards {@code target}; call once per tick. */
    public Gravity next(Gravity target) {

        final Vec3d goal = target.direction().scale(target.strength());
        if (!started) {
            started = true;
            pull = goal;
        }
        final Vec3d step = goal.sub(pull);
        pull = step.length() < SETTLED ? goal : pull.add(step.scale(RATE));
        if (pull.equals(goal)) {
            direction = target.direction();
            return target;
        }
        final double strength = pull.length();
        if (strength > SETTLED) {
            direction = pull.scale(1 / strength);
        }
        return new Gravity(direction, strength);
    }
}
