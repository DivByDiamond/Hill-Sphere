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
package dev.loki.hillsphere.gametest.frame;

import dev.loki.hillsphere.entity.FrameState;
import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.gametest.CoreGameTests;

import java.util.function.Consumer;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * A stand-in player hung in a turned frame beside the lifted core (it sits at x 2..3, y 3..4, z 3..4), for
 * checks that need a player's eye, hands or flight without a client. The stand-in is not in the world and
 * does not tick, so its frame is looked up by hand until the field has turned it.
 */
final class FrameRig {

    /** Under the core: the field pulls up and the player stands on the core as on a ceiling. */
    static final Vec3 UNDER = new Vec3(2.5, 1.0, 3.5);

    /** East of the core: the field pulls toward -x and the core is a wall. */
    static final Vec3 BESIDE = new Vec3(5.0, 3.0, 3.5);

    /** Lookups after which the frame must have turned: the dwell is four. */
    private static final int LOOKUPS = 8;

    /** Ticks the core needs to spin up and grow its field. */
    private static final int SPIN_UP = 100;

    private FrameRig() {
    }

    /** Spins the core up, hangs a player at the given place and hands it over once its frame is the expected one. */
    static void withPlayer(GameTestHelper helper, Vec3 at, GravityFrame expected, Consumer<Player> check) {

        CoreGameTests.liftCore(helper, Polarity.ATTRACT);
        helper.runAfterDelay(SPIN_UP, () -> {
            final Player player = helper.makeMockPlayer(GameType.CREATIVE);
            player.setPos(helper.absoluteVec(at));
            for (int i = 0; i < LOOKUPS && LivingGravity.frameOf(player) != expected; i++) {
                ((FrameState) player).hillsphereRefresh();
            }
            helper.assertTrue(LivingGravity.frameOf(player) == expected,
                    "the player should be turned to " + expected + ", not " + LivingGravity.frameOf(player));
            check.accept(player);
            helper.succeed();
        });
    }

    /** The frame's "up" in the world, scaled. */
    static Vec3 up(GravityFrame frame, double length) {

        return LivingGravity.above(frame, Vec3.ZERO, length);
    }
}
