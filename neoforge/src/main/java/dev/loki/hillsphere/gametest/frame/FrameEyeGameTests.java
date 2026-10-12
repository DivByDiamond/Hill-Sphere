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

import dev.loki.hillsphere.Constants;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The eye on a wall is beside the feet, not above them: what it is in, and what flying does with "up". */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FrameEyeGameTests {

    private static final String EMPTY = "empty";

    private FrameEyeGameTests() {
    }

    /** A pig standing on the side of the core has its head out along +x and does not suffocate in the core. */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "theEyeIsBesideTheFeetOnAWall")
    public static void theEyeIsBesideTheFeetOnAWall(GameTestHelper helper) {

        CoreGameTests.liftCore(helper, Polarity.ATTRACT);
        helper.runAfterDelay(100, () -> {
            final Mob pig = helper.spawnWithNoFreeWill(EntityType.PIG, FrameRig.BESIDE);
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(LivingGravity.frameOf(pig) == GravityFrame.WEST, "the pig should stand on the core's side");
                final Vec3 eye = pig.getEyePosition();
                final String where = "eye " + eye + " feet " + pig.position();
                helper.assertTrue(eye.x > pig.getX() + 0.5 && Math.abs(eye.y - pig.getY()) < 1.0E-6, "the eye is out along +x, " + where);
                helper.assertTrue(eye.distanceTo(pig.getEyePosition(1.0F)) < 1.0E-6, "both eyes agree, " + where);
                helper.assertTrue(!pig.isInWall(), "the pig is not inside the core, " + where);
                helper.assertTrue(pig.getHealth() == pig.getMaxHealth(), "nor hurt by it, health " + pig.getHealth());
                helper.succeed();
            });
        });
    }

    /** Water at the head of a player on a wall is water at the eyes, even with stone at the feet's column. */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "waterAtTheHeadOnAWallIsAtTheEyes")
    public static void waterAtTheHeadOnAWallIsAtTheEyes(GameTestHelper helper) {

        FrameRig.withPlayer(helper, FrameRig.BESIDE, GravityFrame.WEST, player -> {
            final BlockPos eye = BlockPos.containing(player.getEyePosition());
            final BlockPos feet = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
            helper.assertTrue(!eye.equals(feet), "the eye and vanilla's probe should be in different blocks, " + eye);
            helper.getLevel().setBlockAndUpdate(feet, Blocks.STONE.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(eye, Blocks.WATER.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(eye.above(), Blocks.WATER.defaultBlockState());
            player.baseTick();
            helper.assertTrue(player.getEyeInFluidType() == NeoForgeMod.WATER_TYPE.value(),
                    "the eyes are under water, found " + player.getEyeInFluidType());
        });
    }

    /** Flying on a wall holds still: the pull along the frame's "down" is cancelled as it is on a floor. */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "flyingOnAWallHoldsStill")
    public static void flyingOnAWallHoldsStill(GameTestHelper helper) {

        FrameRig.withPlayer(helper, FrameRig.BESIDE, GravityFrame.WEST, player -> {
            final Vec3 start = player.position();
            player.setDeltaMovement(Vec3.ZERO);
            player.travel(Vec3.ZERO);
            helper.assertTrue(player.getDeltaMovement().x < -0.01, "without flight the player falls toward the core, " + player.getDeltaMovement());
            player.setPos(start);
            player.setDeltaMovement(Vec3.ZERO);
            player.getAbilities().flying = true;
            player.travel(Vec3.ZERO);
            helper.assertTrue(player.getDeltaMovement().length() < 1.0E-3, "flying it holds still, " + player.getDeltaMovement());
        });
    }
}
