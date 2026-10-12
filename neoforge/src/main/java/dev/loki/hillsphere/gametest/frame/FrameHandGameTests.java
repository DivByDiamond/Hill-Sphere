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
import dev.loki.hillsphere.field.frame.GravityFrame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** What leaves the hands of a player standing on a ceiling: placed blocks, thrown snowballs, dropped items. */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FrameHandGameTests {

    private static final String EMPTY = "empty";

    private FrameHandGameTests() {
    }

    /** On a ceiling a furnace faces the player and a piston pushes away from where the player looks. */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "blocksPlacedOnACeilingFaceTheRealLook")
    public static void blocksPlacedOnACeilingFaceTheRealLook(GameTestHelper helper) {

        FrameRig.withPlayer(helper, FrameRig.UNDER, GravityFrame.UP, player -> {
            player.setYRot(0.0F);
            player.setXRot(0.0F);
            helper.assertTrue(player.getLookAngle().z < -0.99, "looking along virtual +z is looking north, " + player.getLookAngle());
            place(helper, player, Items.FURNACE, new BlockPos(5, 1, 1));
            helper.assertBlockProperty(new BlockPos(5, 2, 1), BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
            player.setXRot(90.0F);
            helper.assertTrue(player.getLookAngle().y > 0.99, "looking down on a ceiling is looking up, " + player.getLookAngle());
            place(helper, player, Items.PISTON, new BlockPos(5, 1, 5));
            helper.assertBlockProperty(new BlockPos(5, 2, 5), BlockStateProperties.FACING, Direction.DOWN);
        });
    }

    /** A snowball starts just below the real eye and flies along the real look. */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "aSnowballOnACeilingFliesAlongTheLook")
    public static void aSnowballOnACeilingFliesAlongTheLook(GameTestHelper helper) {

        FrameRig.withPlayer(helper, FrameRig.UNDER, GravityFrame.UP, player -> {
            player.setYRot(40.0F);
            player.setXRot(30.0F);
            final Snowball ball = new Snowball(helper.getLevel(), player);
            final Vec3 muzzle = player.getEyePosition().subtract(FrameRig.up(GravityFrame.UP, 0.1));
            helper.assertTrue(ball.position().distanceTo(muzzle) < 1.0E-3, "the snowball starts below the eye, " + ball.position() + " vs " + muzzle);
            ball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 0.0F);
            final double along = ball.getDeltaMovement().normalize().dot(player.getLookAngle());
            helper.assertTrue(along > 0.99, "it flies along the look, " + ball.getDeltaMovement() + " vs " + player.getLookAngle());
        });
    }

    /** A dropped item leaves from below the real eye and is tossed the way the player looks. */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "anItemDroppedOnACeilingIsTossedAlongTheLook")
    public static void anItemDroppedOnACeilingIsTossedAlongTheLook(GameTestHelper helper) {

        FrameRig.withPlayer(helper, FrameRig.UNDER, GravityFrame.UP, player -> {
            player.setYRot(0.0F);
            player.setXRot(0.0F);
            final ItemEntity item = player.drop(new ItemStack(Items.STICK), false, false);
            helper.assertTrue(item != null, "the item should be dropped");
            final Vec3 hand = player.getEyePosition().subtract(FrameRig.up(GravityFrame.UP, 0.3));
            helper.assertTrue(item.position().distanceTo(hand) < 1.0E-3, "the item starts below the eye, " + item.position() + " vs " + hand);
            final double along = item.getDeltaMovement().dot(player.getLookAngle());
            helper.assertTrue(along > 0.15, "it is tossed along the look, " + item.getDeltaMovement() + " vs " + player.getLookAngle());
        });
    }

    /** Places the item on top of a fresh stone block, as the player clicking it. */
    private static void place(GameTestHelper helper, Player player, Item item, BlockPos stone) {

        helper.setBlock(stone, Blocks.STONE);
        final ItemStack stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final BlockPos at = helper.absolutePos(stone);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(at).add(0, 0.5, 0), Direction.UP, at, false)));
    }
}
