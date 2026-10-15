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
package dev.loki.hillsphere.field.frame;

import dev.loki.hillsphere.field.math.Vec3d;
import java.util.function.ToDoubleFunction;

/**
 * Decides when "down" turns. The pull alone only allows a turn; what decides it is whether there is something to
 * stand on. A frame with a floor close under the feet wins as soon as the pull leans its way, so a body that walks
 * off the edge of a small planet lands on its side at once, and one that walks up to a wall the field leans
 * toward stands on it. A body standing (or hopping) on its floor keeps it until the pull is well above the
 * horizon. With nothing to stand on either way, the frame follows the pull once it is a little past the halfway
 * line between two frames. Turning straight back into the frame just left, while standing on the new floor, takes a
 * pull well past halfway: the turn itself moves the body (it settles onto the new floor, and on a stepped ball the
 * next step is a block away), which swings the pull back by more than the small lean that started the turn.
 */
public final class FrameChooser {

    /** How far under the feet, in blocks, a floor still counts as one to stand on. */
    public static final double REACH = 1.5;

    /** A body on its floor keeps it until the pull points this far (in dot) above the horizon. */
    static final double KEEP_ON_GROUND = -0.4;

    /** How much better (in dot) the pull must fit a frame with a floor before it is taken: just past halfway. */
    static final double LEAN = 0.05;

    /** How much better (in dot) the pull must fit the frame just left, while standing, to turn back: about 14 degrees past halfway. */
    static final double BACK = 0.35;

    /** How much better the pull must fit a frame with no floor near: about six degrees past halfway. */
    static final double FREE_MARGIN = 0.15;

    /** Ticks a frame with a floor must stay the best before the turn. */
    static final int FLOOR_DWELL = 2;

    /** Ticks a frame must stay the best in free flight before the turn. */
    static final int FREE_DWELL = 3;

    private GravityFrame candidate = GravityFrame.DOWN;
    private GravityFrame left;
    private int dwell;

    /**
     * The frame to be in now.
     *
     * @param current the frame the body is in
     * @param pull unit vector of where the field pulls
     * @param floor how far under the feet the nearest thing to stand on is in a frame, in blocks, for a body turned
     *     into that frame; anything above {@link #REACH} means none. It is asked only when a turn is in question.
     * @return {@code current} while no turn is due
     */
    public GravityFrame next(GravityFrame current, Vec3d pull, ToDoubleFunction<GravityFrame> floor) {

        final GravityFrame wanted = GravityFrame.of(pull);
        if (wanted == current) {
            return stay(current);
        }
        final double now = pull.dot(current.toReal(Vec3d.DOWN));
        final double there = pull.dot(wanted.toReal(Vec3d.DOWN));
        final boolean standing = floor.applyAsDouble(current) <= REACH;
        final double lean = wanted == left && standing ? BACK : LEAN;
        if (there > now + lean && floor.applyAsDouble(wanted) <= REACH) {
            return toward(wanted, FLOOR_DWELL, current);
        }
        if (now > KEEP_ON_GROUND && standing) {
            return stay(current);
        }
        if (there > now + FREE_MARGIN) {
            return toward(wanted, FREE_DWELL, current);
        }
        return stay(current);
    }

    /** Forgets a turn in progress, e.g. after the body has turned. */
    public void reset() {

        dwell = 0;
    }

    private GravityFrame stay(GravityFrame current) {

        dwell = 0;
        return current;
    }

    private GravityFrame toward(GravityFrame wanted, int ticks, GravityFrame current) {

        if (wanted != candidate) {
            candidate = wanted;
            dwell = 0;
        }
        dwell++;
        if (dwell < ticks) {
            return current;
        }
        left = current;
        return wanted;
    }
}
