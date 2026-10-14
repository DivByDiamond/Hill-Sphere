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
package dev.loki.hillsphere.field.control;

import dev.loki.hillsphere.field.math.Vec3d;

/**
 * How a player steers and slows down in a field with nothing to push off. Thrust is weakest in the middle of a
 * field and grows toward its edge, so any field can be left; drag grows toward the middle, so swinging dies out.
 * Everything is per tick and in the real world. It is meant for free flight only, with nothing under the feet.
 *
 * @param edgeThrust thrust at the edge of a field
 * @param coreShare share of {@code edgeThrust} left in the middle, 0 to 1
 * @param drag share of the speed lost per tick in the middle, 0 to 1
 */
public record AirControl(double edgeThrust, double coreShare, double drag) {

    public static final AirControl DEFAULT = new AirControl(0.03, 0.3, 0.04);

    public AirControl {

        if (edgeThrust < 0 || coreShare < 0 || coreShare > 1 || drag < 0 || drag > 1) {
            throw new IllegalArgumentException("air control values out of range");
        }
    }

    /** Thrust strength where the field is {@code influence} deep: 1 in the middle, 0 at the edge. */
    public double thrustAt(double influence) {

        return edgeThrust * (coreShare + (1 - coreShare) * (1 - clamp(influence)));
    }

    /**
     * The thrust for the keys held, in the real world. Forward runs along the look, so a player flies where they
     * look whatever frame they are in; strafe (-1 to 1, positive to the left as in vanilla) runs across the look,
     * and diagonals are no stronger. Vertical (-1 to 1) runs along the frame's {@code up}.
     *
     * @param look unit look direction
     * @param up unit "up" of the frame the player is in
     */
    public Vec3d thrust(double strafe, double vertical, double forward, Vec3d look, Vec3d up, double influence) {

        final double len = Math.max(1, Math.hypot(strafe, forward));
        final Vec3d left = up.cross(look).normalized();
        final Vec3d flat = look.scale(forward / len).add(left.scale(strafe / len));
        return flat.add(up.scale(vertical)).scale(thrustAt(influence));
    }

    /** What is left of the speed after a tick of drag where the field is {@code influence} deep. */
    public double keptSpeed(double influence) {

        return 1 - drag * clamp(influence);
    }

    private static double clamp(double v) {

        return Math.max(0, Math.min(1, v));
    }
}
