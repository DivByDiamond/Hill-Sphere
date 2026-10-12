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
package dev.loki.hillsphere.gametest.cleanup;

import java.util.Arrays;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.world.level.block.Blocks;

/**
 * The test server leaves every finished test area standing, with its motor still spinning, so a core of one test
 * would go on pulling at the tests next to it (fields reach up to 32 blocks) and the outcome would depend on the
 * order of the batches. Each core is removed as soon as its test has passed or failed.
 */
public final class CoreCleanup implements GameTestListener {

    private final List<BlockPos> cores;

    private CoreCleanup(List<BlockPos> cores) {

        this.cores = cores;
    }

    /** Removes the cores at these places in the test area once the test is over. */
    public static void removeWhenDone(GameTestHelper helper, BlockPos... relative) {

        helper.testInfo.addListener(new CoreCleanup(Arrays.stream(relative).map(helper::absolutePos).toList()));
    }

    @Override
    public void testStructureLoaded(GameTestInfo test) {
    }

    @Override
    public void testPassed(GameTestInfo test, GameTestRunner runner) {

        remove(test);
    }

    @Override
    public void testFailed(GameTestInfo test, GameTestRunner runner) {

        remove(test);
    }

    @Override
    public void testAddedForRerun(GameTestInfo oldTest, GameTestInfo newTest, GameTestRunner runner) {
    }

    private void remove(GameTestInfo test) {

        cores.forEach(pos -> test.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()));
    }
}
