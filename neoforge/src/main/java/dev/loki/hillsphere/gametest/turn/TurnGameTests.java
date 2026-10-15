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
package dev.loki.hillsphere.gametest.turn;

import dev.loki.hillsphere.Constants;
import dev.loki.hillsphere.entity.FrameState;
import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.gametest.CoreGameTests;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Turning "down" on a small planet: a stone ball around the lifted core (x 1..4, y 2..5, z 2..5, the core and its
 * motor inside). A body that turns onto a face of the ball it leans on, as on a riser of a stepped sphere, must
 * stand on that face and never sink into the ball.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TurnGameTests {

    /** Where the pig starts: touching the ball's east face, its middle level with the core. */
    private static final Vec3 BESIDE = new Vec3(4.45, 3.05, 3.5);

    /** The ball's east face, relative to the test area. */
    private static final double FACE = 4.0;

    /** How much a box may poke into a block before it counts: rounding, not sinking. */
    private static final double SKIN = 1.0E-3;

    private TurnGameTests() {
    }

    /** A pig touching the face turns onto it; tick after tick its box stays out of the ball, and it ends up standing on the face. */
    @GameTest(template = "empty", timeoutTicks = 400, batch = "aMobTurningOntoAFaceItTouchesStaysOutOfIt")
    public static void aMobTurningOntoAFaceItTouchesStaysOutOfIt(GameTestHelper helper) {

        CoreGameTests.liftCore(helper, Polarity.ATTRACT);
        ball(helper);
        helper.runAfterDelay(100, () -> {
            final Mob pig = helper.spawnWithNoFreeWill(EntityType.PIG, BESIDE);
            final double face = helper.absoluteVec(new Vec3(FACE, 0, 0)).x;
            watch(helper, pig, face, 40);
        });
    }

    /** Fresh after a turn, before anything has moved the body, its box is the body turned into the new frame. */
    @GameTest(template = "empty", timeoutTicks = 400, batch = "aTurnLeavesTheBoxOfTheTurnedBody")
    public static void aTurnLeavesTheBoxOfTheTurnedBody(GameTestHelper helper) {

        CoreGameTests.liftCore(helper, Polarity.ATTRACT);
        ball(helper);
        helper.runAfterDelay(100, () -> {
            final Mob pig = helper.spawnWithNoFreeWill(EntityType.PIG, BESIDE);
            for (int i = 0; i < 20 && LivingGravity.frameOf(pig).isVanilla(); i++) {
                ((FrameState) pig).hillsphereRefresh();
            }
            final GravityFrame frame = LivingGravity.frameOf(pig);
            helper.assertTrue(frame == GravityFrame.WEST, "the pig should have turned onto the east face, not " + frame);
            final AABB turned = LivingGravity.box(frame, pig.position(), pig.getDimensions(pig.getPose()));
            helper.assertTrue(same(pig.getBoundingBox(), turned), "the box should be the turned body " + turned + ", not " + pig.getBoundingBox());
            helper.succeed();
        });
    }

    /** Each tick: the pig's box is clear of every block and outside the ball's face; at the end it stands on the face. */
    private static void watch(GameTestHelper helper, Mob pig, double face, int left) {

        final String where = "at " + pig.position() + " box " + pig.getBoundingBox() + " frame " + LivingGravity.frameOf(pig);
        helper.assertTrue(pig.getBoundingBox().minX > face - SKIN, "the pig must not sink into the ball, " + where);
        helper.assertTrue(helper.getLevel().noCollision(pig, pig.getBoundingBox().deflate(SKIN)), "the pig must not be inside blocks, " + where);
        if (left > 0) {
            helper.runAfterDelay(1, () -> watch(helper, pig, face, left - 1));
            return;
        }
        helper.assertTrue(LivingGravity.frameOf(pig) == GravityFrame.WEST && pig.onGround(), "the pig should stand on the face, " + where);
        helper.succeed();
    }

    /** A 3x3x3 stone ball around the core, leaving the core and its motor in place. */
    private static void ball(GameTestHelper helper) {

        final BlockPos core = new BlockPos(2, 3, 3);
        final BlockPos motor = new BlockPos(1, 3, 3);
        for (int x = 1; x <= 3; x++) {
            for (int y = 2; y <= 4; y++) {
                for (int z = 2; z <= 4; z++) {
                    final BlockPos at = new BlockPos(x, y, z);
                    if (!at.equals(core) && !at.equals(motor)) {
                        helper.setBlock(at, Blocks.STONE);
                    }
                }
            }
        }
    }

    private static boolean same(AABB a, AABB b) {

        return a.getCenter().distanceTo(b.getCenter()) < SKIN && Math.abs(a.getXsize() - b.getXsize()) < SKIN
                && Math.abs(a.getYsize() - b.getYsize()) < SKIN && Math.abs(a.getZsize() - b.getZsize()) < SKIN;
    }
}
