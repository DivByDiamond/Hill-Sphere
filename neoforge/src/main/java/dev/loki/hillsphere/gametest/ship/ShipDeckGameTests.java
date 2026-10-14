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
import dev.loki.hillsphere.compat.Ships;
import dev.loki.hillsphere.config.HillSphereConfig;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.control.RedstoneMode;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;
import dev.loki.hillsphere.field.shape.Shape;
import dev.loki.hillsphere.gametest.CoreGameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** A deck-shaped core on a Sable ship fills the whole hull, whatever its shape. */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShipDeckGameTests {

    private static final BlockPos MOTOR = new BlockPos(1, 2, 3);
    private static final BlockPos CORE = new BlockPos(2, 2, 3);

    /** The far end of a beam of stone that runs east from the core. */
    private static final BlockPos FAR_END = new BlockPos(6, 2, 3);

    private ShipDeckGameTests() {
    }

    /**
     * A slow core makes a small field, but as a deck on a ship it reaches the far end of the ship, where a sphere of
     * the same speed would not; and the deck's floor is the ship's floor.
     */
    @GameTest(template = "empty", timeoutTicks = 300, batch = "aDeckOnAShipReachesTheFarEndOfTheHull")
    public static void aDeckOnAShipReachesTheFarEndOfTheHull(GameTestHelper helper) {

        if (!ModList.get().isLoaded("sable")) {
            helper.succeed();
            return;
        }
        CoreGameTests.powered(helper, 8, Polarity.ATTRACT, 4, RedstoneMode.MANUAL).settings().reshape(Shape.DECK);
        for (int x = CORE.getX() + 1; x <= FAR_END.getX(); x++) {
            helper.setBlock(new BlockPos(x, CORE.getY(), CORE.getZ()), Blocks.STONE);
        }
        final ShipRig ship = ShipRig.assemble(helper, CORE, MOTOR, new BlockPos(3, 2, 3), new BlockPos(4, 2, 3),
                new BlockPos(5, 2, 3), FAR_END);
        helper.runAfterDelay(60, () -> {
            try {
                final CoreField field = ship.coreField(CORE);
                helper.assertTrue(field != null && field.deck() != null, "the core on the ship should have a deck: " + field);
                final BlockPos end = ship.blockOf(FAR_END);
                final Vec3d far = Ships.toWorld(helper.getLevel(), new Vec3d(end.getX() + 0.5, end.getY() + 1.5, end.getZ() + 0.5));
                final CoreField sphere = new CoreField(field.center(), field.radius(), field.strength(), field.polarity());
                helper.assertTrue(sphere.fade(HillSphereConfig.tuning(), far) == 0, "a sphere this slow should not reach the far end");
                helper.assertTrue(field.fade(HillSphereConfig.tuning(), far) == 1, "the deck should cover the far end: " + field.deck());
                helper.assertTrue(field.deck().yAxis().y() > 0.7, "the deck's up should be the ship's up, which may have tipped as it"
                        + " landed: " + field.deck().yAxis());
            } finally {
                ship.remove();
            }
            helper.succeed();
        });
    }
}
