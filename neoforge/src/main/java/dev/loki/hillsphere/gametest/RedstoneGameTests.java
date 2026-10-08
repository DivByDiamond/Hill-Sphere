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
import dev.loki.hillsphere.block.FieldState;
import dev.loki.hillsphere.block.HillCoreBlock;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.control.RedstoneMode;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** What each redstone mode does to a spinning core, without and with a signal. */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RedstoneGameTests {

    private static final BlockPos CORE = new BlockPos(2, 2, 3);
    private static final int SPIN_UP = 100;

    private RedstoneGameTests() {
    }

    /** Runs a core in the mode, checks its look before a signal, then again after a redstone block powers it. */
    private static void check(GameTestHelper helper, RedstoneMode mode, int level, FieldState without, FieldState with) {

        CoreGameTests.powered(helper, 128, Polarity.ATTRACT, level, mode);
        helper.runAfterDelay(SPIN_UP, () -> {
            helper.assertTrue(helper.getBlockState(CORE).getValue(HillCoreBlock.FIELD) == without,
                    mode + " without a signal should look " + without + ", is " + helper.getBlockState(CORE).getValue(HillCoreBlock.FIELD));
            helper.setBlock(CORE.above(), Blocks.REDSTONE_BLOCK);
            helper.runAfterDelay(SPIN_UP, () -> {
                helper.assertTrue(helper.getBlockState(CORE).getValue(HillCoreBlock.FIELD) == with,
                        mode + " with a signal should look " + with + ", is " + helper.getBlockState(CORE).getValue(HillCoreBlock.FIELD));
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "manualIgnoresRedstone")
    public static void manualIgnoresRedstone(GameTestHelper helper) {

        check(helper, RedstoneMode.MANUAL, 3, FieldState.ATTRACT, FieldState.ATTRACT);
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "onWithSignalNeedsTheSignal")
    public static void onWithSignalNeedsTheSignal(GameTestHelper helper) {

        check(helper, RedstoneMode.ON_WITH_SIGNAL, 3, FieldState.OFF, FieldState.ATTRACT);
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "analogFollowsTheSignal")
    public static void analogFollowsTheSignal(GameTestHelper helper) {

        check(helper, RedstoneMode.ANALOG, 6, FieldState.OFF, FieldState.ATTRACT);
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "reverseFlipsTheField")
    public static void reverseFlipsTheField(GameTestHelper helper) {

        check(helper, RedstoneMode.REVERSE_ON_SIGNAL, 3, FieldState.ATTRACT, FieldState.REPEL);
    }
}
