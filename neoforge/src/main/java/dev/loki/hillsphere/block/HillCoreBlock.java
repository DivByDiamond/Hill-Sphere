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
package dev.loki.hillsphere.block;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.loki.hillsphere.blockentity.HillCoreBlockEntity;
import dev.loki.hillsphere.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** The gravity core: a Create rotation consumer whose shaft runs through it along its axis. */
public class HillCoreBlock extends RotatedPillarKineticBlock implements IBE<HillCoreBlockEntity> {

    public static final int MAX_LEVEL = 6;
    public static final EnumProperty<FieldState> FIELD = EnumProperty.create("field", FieldState.class);
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, MAX_LEVEL);

    public HillCoreBlock(Properties properties) {

        super(properties);
        registerDefaultState(defaultBlockState().setValue(FIELD, FieldState.OFF).setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {

        super.createBlockStateDefinition(builder);
        builder.add(FIELD, LEVEL);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {

        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {

        return state.getValue(AXIS);
    }

    @Override
    public Class<HillCoreBlockEntity> getBlockEntityClass() {

        return HillCoreBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HillCoreBlockEntity> getBlockEntityType() {

        return ModBlockEntities.HILL_CORE.get();
    }
}
