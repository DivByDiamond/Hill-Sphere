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
import dev.loki.hillsphere.field.resolve.Gravity;
import dev.loki.hillsphere.field.resolve.PullSmoother;
import java.util.function.UnaryOperator;

/**
 * A ball of blocks around a core and a player-sized body walking on it the way the game moves one. The surface is
 * stepped: block faces point along the axes while the ball's normal turns. Each tick the eased pull is taken at the
 * middle of the body, a turn keeps that middle (lifted out of blocks if need be), then the body walks along its
 * floor toward a heading, climbs a block it runs into and falls along its "down" until it lands.
 */
final class StairPlanet {

    private static final double WIDTH = 0.6;
    private static final double HEIGHT = 1.8;
    private static final double SKIN = 0.01;
    private static final double PROBE = 0.25;
    private static final double LIFT = 0.125;
    private static final double FALL = 0.4;
    private static final double NUDGE = 0.01;

    /** A turn back into the frame just left within this many blocks of the last turn is a flicker, not a walk onto another face. */
    private static final double FLICKER = 2;

    /** The middle of the core block, block (0, 0, 0). */
    private static final Vec3d CORE = new Vec3d(0.5, 0.5, 0.5);

    private final double radius;
    private final double speed;
    private final FrameChooser chooser = new FrameChooser();
    private final PullSmoother pull = new PullSmoother();
    private Vec3d center;
    private GravityFrame frame;
    private GravityFrame left;
    private int turns;
    private int turnsBack;
    private Vec3d turnedAt = Vec3d.ZERO;

    /** A body of the given walking speed (blocks a tick) landed on the ball from the given direction out of the core. */
    StairPlanet(double radius, double speed, Vec3d from) {

        this.radius = radius;
        this.speed = speed;
        this.center = CORE.add(from.normalized().scale(radius + 1.5));
        this.frame = GravityFrame.of(from.scale(-1));
        fall(2 * FALL * 10);
    }

    /** Walks once around the core at the height it stands at, along the shoulder of the ball. */
    StairPlanet walkAround() {

        final int ticks = (int) (2 * Math.PI * (radius + 1) / speed);
        for (int tick = 0; tick < ticks; tick++) {
            step(out -> new Vec3d(-out.z(), 0, out.x()));
        }
        return this;
    }

    /** Walks over the top toward the given side, until the body is level with the core. */
    StairPlanet walkOver(Vec3d side) {

        final Vec3d goal = side.normalized();
        for (int tick = 0; tick < 2000 && center.y() > CORE.y(); tick++) {
            step(out -> goal.sub(out.scale(goal.dot(out) / out.dot(out))));
        }
        return this;
    }

    /** How many times "down" turned. */
    int turns() {

        return turns;
    }

    /** How many times "down" turned straight back into the frame it had just left, close to where it left it. */
    int turnsBack() {

        return turnsBack;
    }

    private void step(UnaryOperator<Vec3d> heading) {

        final Vec3d down = pull.next(new Gravity(CORE.sub(center).normalized(), 1)).direction();
        final GravityFrame next = chooser.next(frame, down, this::floor);
        if (next != frame) {
            turn(next);
        }
        walk(heading.apply(center.sub(CORE)));
        fall(FALL);
    }

    private void turn(GravityFrame to) {

        final Vec3d up = to.toReal(Vec3d.DOWN).scale(-1);
        for (double lifted = 0; lifted <= HEIGHT + LIFT; lifted += LIFT) {
            final Vec3d there = center.add(up.scale(lifted));
            if (free(to, there, 0)) {
                turnsBack += to == left && there.sub(turnedAt).length() < FLICKER ? 1 : 0;
                turnedAt = there;
                turns++;
                left = frame;
                frame = to;
                center = there;
                chooser.reset();
                return;
            }
        }
    }

    /** One step along the floor toward the heading, or up a block and on when one is in the way. */
    private void walk(Vec3d heading) {

        final Vec3d down = frame.toReal(Vec3d.DOWN);
        final Vec3d along = heading.sub(down.scale(heading.dot(down))).normalized().scale(speed);
        final Vec3d climbed = center.sub(down);
        if (free(frame, center.add(along), 0)) {
            center = center.add(along);
        } else if (free(frame, climbed, 0) && free(frame, climbed.add(along), 0)) {
            center = climbed.add(along);
        }
    }

    private void fall(double most) {

        final Vec3d down = frame.toReal(Vec3d.DOWN).scale(NUDGE);
        for (double d = 0; d < most && free(frame, center.add(down), 0); d += NUDGE) {
            center = center.add(down);
        }
    }

    /** How far under the feet of the body turned into the frame a block is, as the game's support probe asks it. */
    private double floor(GravityFrame to) {

        final Vec3d down = to.toReal(Vec3d.DOWN);
        for (double d = 0; d <= FrameChooser.REACH; d += PROBE) {
            if (!free(to, center.add(down.scale(d)), SKIN)) {
                return d;
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    /** Whether the body turned into the frame, its middle at the point, overlaps no block of the ball. */
    private boolean free(GravityFrame to, Vec3d middle, double skin) {

        final Vec3d down = to.toReal(Vec3d.DOWN);
        final Vec3d half = new Vec3d(half(down.x(), skin), half(down.y(), skin), half(down.z(), skin));
        final Vec3d lo = middle.sub(half);
        final Vec3d hi = middle.add(half);
        for (int x = (int) Math.floor(lo.x()); x < hi.x(); x++) {
            for (int y = (int) Math.floor(lo.y()); y < hi.y(); y++) {
                for (int z = (int) Math.floor(lo.z()); z < hi.z(); z++) {
                    if ((double) x * x + (double) y * y + (double) z * z <= radius * radius) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static double half(double along, double skin) {

        return (along == 0 ? WIDTH : HEIGHT) / 2 - skin;
    }
}
