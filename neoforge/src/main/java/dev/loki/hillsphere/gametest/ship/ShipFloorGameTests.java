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
package dev.loki.hillsphere.gametest.ship;

import dev.loki.hillsphere.Constants;
import dev.loki.hillsphere.entity.FrameState;
import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.entity.probe.SupportProbe;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.frame.FrameChooser;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.gametest.CoreGameTests;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LevelExtension;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A planet may be a ship: a ball of blocks around a core, assembled. Its blocks are not in the world's collision, so
 * whatever asks "is there something to stand on" must ask the ship mod too, or a body walking on such a planet is
 * always "in free flight": its "down" turns only on the free-flight margin and the air controls push it around.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShipFloorGameTests {

    /** A pig between a stone wall of the world on its west and a stone ship on its east, a block from each. */
    private static final Vec3 BETWEEN = new Vec3(2.5, 0, 3.5);

    private ShipFloorGameTests() {
    }

    /** The probe sees the ship's side as a floor in the frame whose "down" points at it, just as it sees the world's wall. */
    @GameTest(template = "empty", timeoutTicks = 100, batch = "aShipCountsAsSomethingToStandOn")
    public static void aShipCountsAsSomethingToStandOn(GameTestHelper helper) {

        if (!ModList.get().isLoaded("sable")) {
            helper.succeed();
            return;
        }
        final List<BlockPos> cube = new ArrayList<>();
        cube.add(new BlockPos(5, 1, 3));
        for (int x = 4; x <= 6; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 2; z <= 4; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                    helper.setBlock(new BlockPos(0, y, z), Blocks.STONE);
                    if (x != 5 || y != 1 || z != 3) {
                        cube.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        final ShipRig ship = ShipRig.assemble(helper, cube.toArray(BlockPos[]::new));
        final Mob pig = helper.spawnWithNoFreeWill(EntityType.PIG, BETWEEN);
        helper.runAfterDelay(2, () -> {
            try {
                final double wall = SupportProbe.floorBelow(pig, GravityFrame.WEST, FrameChooser.REACH);
                final double hull = SupportProbe.floorBelow(pig, GravityFrame.EAST, FrameChooser.REACH);
                helper.assertTrue(wall <= FrameChooser.REACH, "the world's wall should be a floor, not " + wall + " at " + pig.position());
                helper.assertTrue(hull <= FrameChooser.REACH, "the ship's side should be a floor too, not " + hull + " at " + pig.position()
                        + " (the world's wall is " + wall + ")");
            } finally {
                ship.remove();
            }
            helper.succeed();
        });
    }

    /**
     * A body on the side of a ship, its "down" turned toward the ship: the ship's side is its floor. Sable must take
     * the push out of the side as the body's ground, as it does under an upright body; taken for a wall it keeps
     * the body sliding along the side (a wall barely brakes it, and a slope of a tilted ship pushes it along) and
     * steps it sideways along world "up" into the blocks.
     */
    @GameTest(template = "empty", timeoutTicks = 300, batch = "aShipSideIsTheGroundOfABodyTurnedToIt")
    public static void aShipSideIsTheGroundOfABodyTurnedToIt(GameTestHelper helper) {

        if (!ModList.get().isLoaded("sable")) {
            helper.succeed();
            return;
        }
        CoreGameTests.liftCore(helper, Polarity.ATTRACT);
        helper.runAfterDelay(100, () -> {
            final List<BlockPos> wall = new ArrayList<>();
            for (int y = 0; y <= 5; y++) {
                for (int z = 1; z <= 5; z++) {
                    helper.setBlock(new BlockPos(3, y, z), Blocks.STONE);
                    wall.add(new BlockPos(3, y, z));
                }
            }
            wall.add(0, wall.remove(3 * 5 + 2));
            final ShipRig ship = ShipRig.assemble(helper, wall.toArray(BlockPos[]::new));
            final double shipX = ship.position().x();
            helper.runAfterDelay(2, () -> {
                try {
                    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    player.setPos(helper.absoluteVec(new Vec3(6.0, 3.0, 3.5)));
                    for (int i = 0; i < 8 && LivingGravity.frameOf(player) != GravityFrame.WEST; i++) {
                        ((FrameState) player).hillsphereRefresh();
                    }
                    helper.assertTrue(LivingGravity.frameOf(player) == GravityFrame.WEST, "the player should be turned WEST");
                    player.setPos(helper.absoluteVec(new Vec3(4.0 + ship.position().x() - shipX, 3.0, 3.5)));
                    final SubLevelEntityCollision.CollisionInfo hit = SubLevelEntityCollision.collide(player, new Vec3(-0.1, 0, 0),
                            Vec3.ZERO, ((LevelExtension) helper.getLevel()).sable$getJOMLSink());
                    helper.assertTrue(hit.verticalCollisionBelow && !hit.subLevelHorizontalCollision,
                            "the ship's side should be the ground, not a wall: below=" + hit.verticalCollisionBelow
                                    + " wall=" + hit.subLevelHorizontalCollision + " motion=" + hit.motion);
                } finally {
                    ship.remove();
                }
                helper.succeed();
            });
        });
    }
}
