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
package dev.loki.hillsphere.gametest.deck;

import dev.loki.hillsphere.Constants;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.control.RedstoneMode;
import dev.loki.hillsphere.field.shape.Shape;
import dev.loki.hillsphere.gametest.CoreGameTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Deck-shaped fields on the ground: a cube with one "down" inside. */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DeckGameTests {

    private DeckGameTests() {
    }

    /**
     * A repelling deck makes "up" the floor all over its cube, out to its corners: a pig four blocks from a slow core,
     * past the three-block radius a sphere of the same speed would have, still falls up.
     */
    @GameTest(template = "empty", timeoutTicks = 400, batch = "aRepellingDeckLiftsAPigInTheCornerOfItsCube")
    public static void aRepellingDeckLiftsAPigInTheCornerOfItsCube(GameTestHelper helper) {

        CoreGameTests.powered(helper, 8, Polarity.REPEL, 4, RedstoneMode.MANUAL).settings().reshape(Shape.DECK);
        helper.runAfterDelay(100, () -> {
            final Mob pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.9, 4.35, 5.9));
            final double startY = pig.getY();
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(pig.getY() > startY + 0.4, "a pig in the corner of a repelling deck should fall up: " + startY + " to "
                        + pig.getY());
                helper.succeed();
            });
        });
    }
}
