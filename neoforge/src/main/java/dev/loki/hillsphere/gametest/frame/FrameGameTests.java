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

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import dev.loki.hillsphere.Constants;
import dev.loki.hillsphere.block.HillCoreBlock;
import dev.loki.hillsphere.blockentity.HillCoreBlockEntity;
import dev.loki.hillsphere.entity.FrameState;
import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.control.RedstoneMode;
import dev.loki.hillsphere.gametest.CoreGameTests;
import dev.loki.hillsphere.gametest.cleanup.CoreCleanup;
import dev.loki.hillsphere.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** What the frame changes about feet, eyes and steps: the ceiling cases vanilla can never reach. */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FrameGameTests {

    private static final String EMPTY = "empty";

    private FrameGameTests() {
    }

    /** Friction stands on the core the pig hangs from, not on the air where its feet would be in vanilla. */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "theBlockUnderTheFeetIsTheCore")
    public static void theBlockUnderTheFeetIsTheCore(GameTestHelper helper) {

        CoreGameTests.liftCore(helper, Polarity.ATTRACT);
        final double floor = helper.absoluteVec(Vec3.ZERO).y;
        helper.runAfterDelay(100, () -> {
            final Mob pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(2.5, 1.0, 3.5));
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(!LivingGravity.frameOf(pig).isVanilla(), "the pig should hang under the core");
                helper.assertTrue(pig.getY() > floor + 2.6, "the pig should be pressed against the core, y " + (pig.getY() - floor));
                final BlockPos on = pig.getBlockPosBelowThatAffectsMyMovement();
                helper.assertTrue(on.equals(helper.absolutePos(new BlockPos(2, 3, 3))),
                        "the feet stand on the core, block " + on + " at " + pig.position());
                helper.succeed();
            });
        });
    }

    /** The eye is the head of the turned body: below the feet when the pig hangs from a ceiling. */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "theEyeIsBelowTheFeetOnACeiling")
    public static void theEyeIsBelowTheFeetOnACeiling(GameTestHelper helper) {

        CoreGameTests.liftCore(helper, Polarity.ATTRACT);
        helper.runAfterDelay(100, () -> {
            final Mob pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(2.5, 1.0, 3.5));
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(!LivingGravity.frameOf(pig).isVanilla(), "the pig should hang under the core");
                helper.assertTrue(pig.getEyePosition().y < pig.getY() - 0.1,
                        "the eye should be below the feet, eye " + pig.getEyePosition().y + " feet " + pig.getY());
                helper.assertTrue(pig.getEyePosition(1.0F).y < pig.getY() - 0.1,
                        "the interpolated eye should be below the feet too, eye " + pig.getEyePosition(1.0F).y);
                helper.succeed();
            });
        });
    }

    /** A pig on a ceiling jumps a block and more away from it, whatever the pull; the hop scales with it. */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "aPigJumpsOffTheCeiling")
    public static void aPigJumpsOffTheCeiling(GameTestHelper helper) {

        CoreGameTests.liftCore(helper, Polarity.ATTRACT);
        helper.runAfterDelay(100, () -> {
            final Mob pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(2.5, 1.0, 3.5));
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(!LivingGravity.frameOf(pig).isVanilla() && pig.onGround(), "the pig should stand on the core");
                final double rest = pig.getY();
                final double[] lowest = {rest};
                pig.getJumpControl().jump();
                watch(pig, lowest, 20, helper);
                helper.runAfterDelay(25, () -> {
                    final String what = "hop " + (rest - lowest[0]) + " at pull " + ((FrameState) pig).hillsphereStrength();
                    helper.assertTrue(rest - lowest[0] > 0.9, "the pig should hop clear of a block, " + what);
                    helper.assertTrue(pig.onGround() && Math.abs(pig.getY() - rest) < 0.01, "and land back on the core, " + what);
                    helper.succeed();
                });
            });
        });
    }

    /** Keeps the lowest point (the farthest from the ceiling) of the pig over the next few ticks. */
    private static void watch(Mob pig, double[] lowest, int left, GameTestHelper helper) {

        lowest[0] = Math.min(lowest[0], pig.getY());
        if (left > 0) {
            helper.runAfterDelay(1, () -> watch(pig, lowest, left - 1, helper));
        }
    }

    /** A slab under a ceiling is a step along the frame's "up"; without it the pig could never get past. */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "aPigStepsOverASlabOnTheCeiling")
    public static void aPigStepsOverASlabOnTheCeiling(GameTestHelper helper) {

        for (int x = 0; x < 7; x++) {
            helper.setBlock(new BlockPos(x, 4, 3), Blocks.STONE);
        }
        helper.setBlock(new BlockPos(3, 3, 3),
                Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        repelCoreBelow(helper);
        final Vec3 origin = helper.absoluteVec(Vec3.ZERO);
        helper.runAfterDelay(100, () -> {
            final Mob pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(2.5, 2.0, 3.5));
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(!LivingGravity.frameOf(pig).isVanilla(), "the pig should press against the ceiling");
                helper.assertTrue(pig.getY() > origin.y + 3.5, "the pig should stand on the ceiling, y " + (pig.getY() - origin.y));
                shove(helper, pig, origin.x + 4.6, 60);
                helper.runAfterDelay(70, () -> {
                    helper.assertTrue(pig.getX() > origin.x + 4.45,
                            "a pig should step over the slab, x " + (pig.getX() - origin.x) + " y " + (pig.getY() - origin.y));
                    helper.succeed();
                });
            });
        });
    }

    /** A repelling core at the floor pushes a pig up to the ceiling, the mirror of the attracting one. */
    private static void repelCoreBelow(GameTestHelper helper) {

        final BlockPos motor = new BlockPos(1, 0, 3);
        final BlockPos core = new BlockPos(2, 0, 3);
        helper.setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST));
        helper.setBlock(core, ModBlocks.HILL_CORE.get().defaultBlockState().setValue(HillCoreBlock.AXIS, Axis.X));
        CoreCleanup.removeWhenDone(helper, core);
        ((CreativeMotorBlockEntity) helper.getBlockEntity(motor)).generatedSpeed.setValue(128);
        ((HillCoreBlockEntity) helper.getBlockEntity(core)).settings().configure(Polarity.REPEL, 3, RedstoneMode.MANUAL);
    }

    /** Hands the pig a sideways nudge each tick until it clears the slab or the nudge budget runs out. */
    private static void shove(GameTestHelper helper, Mob pig, double limit, int left) {

        if (left > 0 && pig.getX() < limit) {
            pig.setDeltaMovement(0.4, 0.0, 0.0);
            helper.runAfterDelay(1, () -> shove(helper, pig, limit, left - 1));
        }
    }
}
