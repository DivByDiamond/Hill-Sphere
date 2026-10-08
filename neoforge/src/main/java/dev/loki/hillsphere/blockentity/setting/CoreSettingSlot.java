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
package dev.loki.hillsphere.blockentity.setting;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.loki.hillsphere.block.HillCoreBlock;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Where the settings panel sits on the core: the face opposite the window. */
public final class CoreSettingSlot extends ValueBoxTransform.Sided {

    @Override
    protected Vec3 getSouthLocation() {

        return VecHelper.voxelSpace(8, 8, 15.1f);
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction direction) {

        return direction == faceOf(state.getValue(HillCoreBlock.AXIS));
    }

    /**
     * The face for a given shaft axis. The window is on the model's east face and the panel on the opposite
     * (west) face; the directions follow the rotations the blockstate gives each axis.
     */
    static Direction faceOf(Axis shaft) {

        return shaft == Axis.X ? Direction.NORTH : Direction.WEST;
    }

    @Override
    public float getScale() {

        return 0.4f;
    }
}
