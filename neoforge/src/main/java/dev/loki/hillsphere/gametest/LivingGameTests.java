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
package dev.loki.hillsphere.gametest;

import dev.loki.hillsphere.Constants;
import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.Polarity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Players and mobs: only straight up and down for now, see LivingGravity. */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LivingGameTests {

    private LivingGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "aMobUnderAnAttractingCoreFallsUp")
    public static void aMobUnderAnAttractingCoreFallsUp(GameTestHelper helper) {

        CoreGameTests.liftCore(helper, Polarity.ATTRACT);
        helper.runAfterDelay(100, () -> {
            final Mob pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(2.5, 1.0, 3.5));
            final double startY = pig.getY();
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(pig.getY() > startY + 0.4, "a pig under an attracting core should fall up: " + startY + " to " + pig.getY());
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "aMobStandsOnTheCeilingItFellTo")
    public static void aMobStandsOnTheCeilingItFellTo(GameTestHelper helper) {

        CoreGameTests.liftCore(helper, Polarity.ATTRACT);
        helper.runAfterDelay(100, () -> {
            final Mob pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(2.5, 1.0, 3.5));
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(pig.getY() > helper.absoluteVec(new Vec3(0, 1.5, 0)).y && pig.onGround(), "the pig should rest against the core, y " + pig.getY() + " onGround " + pig.onGround());
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "aLongFallUpHurtsOnTheCeiling")
    public static void aLongFallUpHurtsOnTheCeiling(GameTestHelper helper) {

        CoreGameTests.liftCore(helper, Polarity.ATTRACT);
        helper.runAfterDelay(100, () -> {
            final Mob pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(2.5, 1.0, 3.5));
            pig.fallDistance = 10;
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "a long fall up should hurt, health " + pig.getHealth());
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "aMobFallsSidewaysToACoreBesideIt")
    public static void aMobFallsSidewaysToACoreBesideIt(GameTestHelper helper) {

        CoreGameTests.liftCore(helper, Polarity.ATTRACT);
        helper.runAfterDelay(100, () -> {
            final Vec3 start = helper.absoluteVec(new Vec3(5.0, 3.0, 3.5));
            final Mob pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(5.0, 3.0, 3.5));
            helper.runAfterDelay(80, () -> {
                final String where = "from " + start + " to " + pig.position() + " onGround " + pig.onGround();
                helper.assertTrue(pig.getX() < start.x - 1.0, "a pig beside an attracting core should fall toward it, " + where);
                helper.assertTrue(Math.abs(pig.getY() - start.y) < 1.6, "it should not fall down, " + where);
                helper.assertTrue(pig.onGround(), "it should stand against the core, " + where);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "aMobOnAFloorStaysOnItBesideACore")
    public static void aMobOnAFloorStaysOnItBesideACore(GameTestHelper helper) {

        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        CoreGameTests.powered(helper, 128, Polarity.ATTRACT, 3);
        helper.runAfterDelay(100, () -> {
            final Mob pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(5.5, 1.0, 3.5));
            helper.runAfterDelay(20, () -> {
                final Vec3 settled = pig.position();
                helper.runAfterDelay(60, () -> {
                    helper.assertTrue(pig.position().distanceTo(settled) < 0.3, "a pig on the floor should stay on it, from " + settled + " to " + pig.position());
                    helper.assertTrue(LivingGravity.frameOf(pig).isVanilla(), "its down should stay down, not " + LivingGravity.frameOf(pig));
                    helper.succeed();
                });
            });
        });
    }
}
