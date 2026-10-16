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
package dev.loki.hillsphere.gametest.side;

import dev.loki.hillsphere.Constants;
import dev.loki.hillsphere.entity.FrameState;
import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.gametest.CoreGameTests;
import dev.loki.hillsphere.gametest.ship.ShipRig;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A body that walks on a wall beside the core, in each side frame the test area allows, on a wall of the world and on
 * a wall lifted into a Sable ship: step after step neither its box nor its eye is inside a block, and it stands on the
 * wall. The steps run within one tick, so a ship cannot drift or tumble under the body while it is watched. On a ship
 * the body takes a single step: Sable carries a body on a ship by the ship's motion of the tick on every move, so more
 * moves within one tick would carry it that far again and again while the ship stays.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SideGameTests {

    /**
     * How deep a corner may be in a block before it counts: rounding, and the tick a free ship moves between the
     * pose a turn checks and the pose it is measured in; sinking is half a body or more.
     */
    private static final double SKIN = 0.1;

    /** Ticks the core needs to spin up and grow its field. */
    private static final int SPIN_UP = 100;

    /** Lookups after which the frame must have turned: the dwell is four. */
    private static final int LOOKUPS = 8;

    /** Steps the body walks forward, about a block, before it stands still for the rest. */
    private static final int WALK = 10;

    /** Steps the body takes. */
    private static final int STEPS = 40;

    private SideGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "aBodyOnAWorldWallWestStaysOutOfIt")
    public static void aBodyOnAWorldWallWestStaysOutOfIt(GameTestHelper helper) {

        side(helper, GravityFrame.WEST, false);
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "aBodyOnAWorldWallNorthStaysOutOfIt")
    public static void aBodyOnAWorldWallNorthStaysOutOfIt(GameTestHelper helper) {

        side(helper, GravityFrame.NORTH, false);
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "aBodyOnAWorldWallSouthStaysOutOfIt")
    public static void aBodyOnAWorldWallSouthStaysOutOfIt(GameTestHelper helper) {

        side(helper, GravityFrame.SOUTH, false);
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "aBodyOnAShipWallWestStaysOutOfIt")
    public static void aBodyOnAShipWallWestStaysOutOfIt(GameTestHelper helper) {

        side(helper, GravityFrame.WEST, true);
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "aBodyOnAShipWallNorthStaysOutOfIt")
    public static void aBodyOnAShipWallNorthStaysOutOfIt(GameTestHelper helper) {

        side(helper, GravityFrame.NORTH, true);
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "aBodyOnAShipWallSouthStaysOutOfIt")
    public static void aBodyOnAShipWallSouthStaysOutOfIt(GameTestHelper helper) {

        side(helper, GravityFrame.SOUTH, true);
    }

    /** Builds the wall the frame's "down" points at, beside the core, and walks a player along it from a little off it. */
    private static void side(GameTestHelper helper, GravityFrame frame, boolean ship) {

        if (ship && !ModList.get().isLoaded("sable")) {
            helper.succeed();
            return;
        }
        CoreGameTests.liftCore(helper, Polarity.ATTRACT);
        helper.runAfterDelay(SPIN_UP, () -> {
            final List<BlockPos> wall = wall(frame);
            wall.forEach(pos -> helper.setBlock(pos, Blocks.STONE));
            final ShipRig rig = ship ? ShipRig.assemble(helper, wall.toArray(BlockPos[]::new)) : null;
            helper.runAfterDelay(2, () -> walk(helper, frame, rig));
        });
    }

    /** Turns a player onto the wall and walks it along, all within one tick, so a ship holds still under it. */
    private static void walk(GameTestHelper helper, GravityFrame frame, ShipRig rig) {

        try {
            final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(helper.absoluteVec(spawn(frame)));
            for (int i = 0; i < LOOKUPS && LivingGravity.frameOf(player) != frame; i++) {
                ((FrameState) player).hillsphereRefresh();
            }
            helper.assertTrue(LivingGravity.frameOf(player) == frame, "the player should be turned " + frame + ", not " + LivingGravity.frameOf(player));
            clear(helper, player, rig, "turned");
            for (int step = 0; step < (rig == null ? STEPS : 1); step++) {
                player.travel(new Vec3(0, 0, step < WALK ? 1 : 0));
                clear(helper, player, rig, "step " + step);
            }
            helper.assertTrue(rig != null || player.onGround(), "the player should stand on the wall, " + player.position());
        } finally {
            if (rig != null) {
                rig.remove();
            }
        }
        helper.succeed();
    }

    /** Neither a corner of the box nor the eye is in a solid block of the world or of the ship. */
    private static void clear(GameTestHelper helper, Player player, ShipRig rig, String when) {

        final AABB box = player.getBoundingBox();
        final Vec3 eye = player.getEyePosition();
        final String where = when + " at " + player.position() + " eye " + eye + " box " + box;
        for (int corner = 0; corner < 8; corner++) {
            final Vec3 point = new Vec3(corner & 1, corner >> 1 & 1, corner >> 2 & 1).scale(-2 * SKIN).add(SKIN, SKIN, SKIN)
                    .add((corner & 1) == 0 ? box.minX : box.maxX, (corner & 2) == 0 ? box.minY : box.maxY,
                            (corner & 4) == 0 ? box.minZ : box.maxZ);
            helper.assertTrue(!solid(helper, rig, point), "the box must not be inside a block, corner " + point + " " + where);
        }
        helper.assertTrue(!solid(helper, rig, eye), "the eye must be in the air, " + where);
    }

    /** Whether the point is in a solid block of the world or of the ship. */
    private static boolean solid(GameTestHelper helper, ShipRig rig, Vec3 point) {

        return !helper.getLevel().noCollision(AABB.ofSize(point, 1.0E-4, 1.0E-4, 1.0E-4)) || rig != null && rig.solidAt(point);
    }

    /** A 5x5 stone wall one block off the core (at 2, 3, 3) on the side the frame's "down" points to; the nearest block first. */
    private static List<BlockPos> wall(GravityFrame frame) {

        final List<BlockPos> wall = new ArrayList<>();
        for (int a = 0; a <= 4; a++) {
            for (int b = 1; b <= 5; b++) {
                wall.add(switch (frame) {
                    case WEST -> new BlockPos(3, b, a + 1);
                    case NORTH -> new BlockPos(a, b, 4);
                    default -> new BlockPos(a, b, 2);
                });
            }
        }
        wall.add(0, wall.remove(2 * 5 + 2));
        return wall;
    }

    /** Where the player starts: half a block clear of the wall, room for a free ship to drift, level with the core. */
    private static Vec3 spawn(GravityFrame frame) {

        return switch (frame) {
            case WEST -> new Vec3(4.8, 3.0, 3.5);
            case NORTH -> new Vec3(2.5, 3.0, 5.8);
            default -> new Vec3(2.5, 3.0, 1.2);
        };
    }
}
