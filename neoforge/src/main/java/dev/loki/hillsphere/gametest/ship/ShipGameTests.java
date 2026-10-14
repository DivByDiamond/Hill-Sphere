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
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.control.RedstoneMode;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;
import dev.loki.hillsphere.gametest.CoreGameTests;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Cores on Sable ships; they pass trivially when Sable is not loaded. */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShipGameTests {

    private static final BlockPos MOTOR = new BlockPos(1, 2, 3);
    private static final BlockPos CORE = new BlockPos(2, 2, 3);
    /** The pull test's puller rides a mast that carries its core five blocks over the floor. */
    private static final BlockPos MAST_MOTOR = new BlockPos(1, 5, 3);
    private static final BlockPos MAST_CORE = new BlockPos(2, 5, 3);
    /** The mast, anchor first: the anchor sits mid-height so it can reach both ends of the ship. */
    private static final List<BlockPos> MAST = List.of(new BlockPos(1, 2, 3),
            new BlockPos(1, 0, 2), new BlockPos(1, 0, 3), new BlockPos(1, 0, 4),
            new BlockPos(2, 0, 2), new BlockPos(2, 0, 4),
            new BlockPos(1, 1, 3), new BlockPos(1, 3, 3), new BlockPos(1, 4, 3));
    /** The ship to be pulled, right under the mast's core so that the pull is straight up. */
    private static final BlockPos UNDER = new BlockPos(2, 0, 3);
    /** The core of the lift test: a levitating core in the world, with a ship five blocks over it. */
    private static final BlockPos FLOOR_MOTOR = new BlockPos(1, 0, 3);
    private static final BlockPos FLOOR_CORE = new BlockPos(2, 0, 3);
    private static final BlockPos OVER = new BlockPos(2, 5, 3);

    private ShipGameTests() {
    }

    /** False without Sable, having already passed the test. */
    private static boolean sable(GameTestHelper helper) {

        if (ModList.get().isLoaded("sable")) {
            return true;
        }
        helper.succeed();
        return false;
    }

    /** A ship of a motor and a core; null without Sable, after the test has already passed. */
    private static ShipRig coreShip(GameTestHelper helper, Polarity polarity, int level) {

        if (!sable(helper)) {
            return null;
        }
        CoreGameTests.powered(helper, 128, polarity, level, RedstoneMode.MANUAL);
        return ShipRig.assemble(helper, CORE, MOTOR);
    }

    /** A ship whose core stands on a mast, five blocks above whatever the mast stands on. */
    private static ShipRig mastShip(GameTestHelper helper, Polarity polarity, int level) {

        if (!sable(helper)) {
            return null;
        }
        CoreGameTests.powered(helper, MAST_MOTOR, MAST_CORE, 128, polarity, level, RedstoneMode.MANUAL);
        MAST.forEach(block -> helper.setBlock(block, Blocks.STONE));
        final List<BlockPos> hull = new ArrayList<>(MAST);
        hull.add(MAST_MOTOR);
        hull.add(MAST_CORE);
        return ShipRig.assemble(helper, hull.toArray(new BlockPos[0]));
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "aCoreOnAShipTakesItsFieldAlongAndHoldsTheShip")
    public static void aCoreOnAShipTakesItsFieldAlongAndHoldsTheShip(GameTestHelper helper) {

        final ShipRig ship = coreShip(helper, Polarity.LEVITATE, 6);
        if (ship == null) {
            return;
        }
        final double startY = ship.position().y();
        helper.runAfterDelay(120, () -> {
            try {
                final CoreField field = ship.coreField(CORE);
                helper.assertTrue(field != null, "the core on the ship should have a field");
                final BlockPos block = ship.blockOf(CORE);
                final Vec3d plot = new Vec3d(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
                helper.assertTrue(field.center().sub(plot).length() > 64, "the field should be in the world, not among the ship's blocks");
                helper.assertTrue(field.center().sub(ship.position()).length() < 2, "the field should ride with the ship: "
                        + field.center() + " vs " + ship.position());
                helper.assertTrue(ship.position().y() > startY - 3, "a weightless core should keep its ship from falling: " + startY + " to "
                        + ship.position().y());
            } finally {
                ship.remove();
            }
            helper.succeed();
        });
    }

    /**
     * The pull is straight up: on the test plots the world sits 10⁶–10⁷ blocks from the origin,
     * and there horizontal motion of a ship does not measure, while vertical does.
     */
    @GameTest(template = "empty", timeoutTicks = 300, batch = "anAttractingShipPullsAnotherShipIn")
    public static void anAttractingShipPullsAnotherShipIn(GameTestHelper helper) {

        final ShipRig puller = mastShip(helper, Polarity.ATTRACT, 6);
        if (puller == null) {
            return;
        }
        helper.runAfterDelay(60, () -> {
            helper.setBlock(UNDER, Blocks.STONE);
            final ShipRig pulled = ShipRig.assemble(helper, UNDER);
            rises(helper, "an attracting core on one ship should pull another ship in", pulled, pulled.position().y(), 0,
                    puller);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "aLevitatingCoreUnderAShipHoldsItUp")
    public static void aLevitatingCoreUnderAShipHoldsItUp(GameTestHelper helper) {

        if (!sable(helper)) {
            return;
        }
        CoreGameTests.powered(helper, FLOOR_MOTOR, FLOOR_CORE, 256, Polarity.LEVITATE, 6, RedstoneMode.MANUAL);
        helper.runAfterDelay(60, () -> {
            helper.setBlock(OVER, Blocks.STONE);
            final ShipRig ship = ShipRig.assemble(helper, OVER);
            final double startY = ship.position().y();
            helper.runAfterDelay(100, () -> {
                final double y = ship.position().y();
                ship.remove();
                helper.assertTrue(y > startY - 3, "a levitating core in the world should keep a ship over it from falling: " + startY + " to " + y);
                helper.succeed();
            });
        });
    }

    /** Waits for the rig to rise clear of where it stood, however slowly the physics runs, then finishes. */
    private static void rises(GameTestHelper helper, String because, ShipRig risen, double startY, int waited,
            ShipRig... alsoRemoved) {

        final double height = risen.position().y() - startY;
        if (height > 1 || waited >= 200) {
            try {
                helper.assertTrue(height > 1, because + ": " + startY + " to " + risen.position().y());
            } finally {
                risen.remove();
                for (final ShipRig also : alsoRemoved) {
                    also.remove();
                }
            }
            helper.succeed();
            return;
        }
        helper.runAfterDelay(1, () -> rises(helper, because, risen, startY, waited + 1, alsoRemoved));
    }
}
