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

import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;
import dev.loki.hillsphere.world.WorldFields;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import org.joml.Vector3dc;

/** A Sable ship built from blocks of a test area. Only touched by tests that checked Sable is loaded. */
final class ShipRig {

    private final GameTestHelper helper;
    private final ServerSubLevel ship;
    private final BlockPos anchor;

    private ShipRig(GameTestHelper helper, ServerSubLevel ship, BlockPos anchor) {

        this.helper = helper;
        this.ship = ship;
        this.anchor = anchor;
    }

    /** Lifts the blocks out of the world into a new ship around the first of them. */
    static ShipRig assemble(GameTestHelper helper, BlockPos... relative) {

        final List<BlockPos> blocks = Arrays.stream(relative).map(helper::absolutePos).toList();
        final BlockPos anchor = blocks.get(0);
        final BoundingBox3i bounds = new BoundingBox3i(anchor.offset(-4, -4, -4), anchor.offset(4, 4, 4));
        return new ShipRig(helper, SubLevelAssemblyHelper.assembleBlocks(helper.getLevel(), anchor, blocks, bounds), anchor);
    }

    /** Where the ship is in the world. */
    Vec3d position() {

        final Vector3dc p = ship.logicalPose().position();
        return new Vec3d(p.x(), p.y(), p.z());
    }

    /** The block of the ship that was at this place of the test area before assembly. */
    BlockPos blockOf(BlockPos relative) {

        return ship.getPlot().getCenterBlock().offset(helper.absolutePos(relative).subtract(anchor));
    }

    /** The field of the core that was at this place of the test area, or null if it has none. */
    CoreField coreField(BlockPos relative) {

        return WorldFields.of(helper.getLevel()).changes().snapshot().get(blockOf(relative));
    }

    void remove() {

        SubLevelContainer.getContainer(helper.getLevel()).removeSubLevel(ship, SubLevelRemovalReason.REMOVED);
    }
}
