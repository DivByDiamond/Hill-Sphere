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
package dev.loki.hillsphere.field.math;

/** Moves a value toward a target by a bounded step, so fields grow and fade instead of snapping. */
public final class Ramp {

    private Ramp() {
    }

    public static double towards(double current, double target, double maxStep) {

        final double delta = target - current;
        if (Math.abs(delta) <= maxStep) {
            return target;
        }
        return current + Math.copySign(maxStep, delta);
    }
}
