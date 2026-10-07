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
package dev.loki.hillsphere.blockentity;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.loki.hillsphere.block.FieldState;
import dev.loki.hillsphere.block.HillCoreBlock;
import dev.loki.hillsphere.blockentity.setting.CoreSettingSlot;
import dev.loki.hillsphere.blockentity.setting.PolarityBehaviour;
import dev.loki.hillsphere.blockentity.setting.PolarityOption;
import dev.loki.hillsphere.blockentity.setting.StepSettingBehaviour;
import dev.loki.hillsphere.config.HillSphereConfig;
import dev.loki.hillsphere.field.CoreDriver;
import dev.loki.hillsphere.field.FieldTuning;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.registry.ModBlockEntities;
import dev.loki.hillsphere.world.WorldFields;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Turns the rotation a core receives and its settings into a gravity field. */
public class HillCoreBlockEntity extends KineticBlockEntity {

    private final CoreDriver driver = new CoreDriver();
    private PolarityBehaviour polarity;
    private StepSettingBehaviour strengthLevel;

    public HillCoreBlockEntity(BlockPos pos, BlockState state) {

        super(ModBlockEntities.HILL_CORE.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

        super.addBehaviours(behaviours);
        polarity = new PolarityBehaviour(Component.translatable("hillsphere.core.polarity"), this,
                new CoreSettingSlot(true));
        strengthLevel = new StepSettingBehaviour(Component.translatable("hillsphere.core.level"), this,
                new CoreSettingSlot(false), HillCoreBlock.MAX_LEVEL);
        behaviours.add(polarity);
        behaviours.add(strengthLevel);
    }

    @Override
    public float calculateStressApplied() {

        final int level = strengthLevel == null ? 1 : strengthLevel.getValue();
        lastStressApplied = (float) (HillSphereConfig.tuning().stressPerRpmPerLevel() * level);
        return lastStressApplied;
    }

    @Override
    public void tick() {

        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        final boolean enabled = !level.hasNeighborSignal(worldPosition) && !isOverStressed();
        final FieldTuning tuning = HillSphereConfig.tuning();
        final BlockPos p = worldPosition;
        WorldFields.of(level).update(p, driver.step(tuning, new Vec3d(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5),
                polarityValue(), strengthLevel.getValue(), Math.abs(getSpeed()), enabled));
        showState();
    }

    @Override
    public void remove() {

        if (level != null && !level.isClientSide) {
            WorldFields.of(level).remove(worldPosition);
        }
        super.remove();
    }

    public Polarity polarityValue() {

        return polarity.get().polarity();
    }

    public int getStrengthLevel() {

        return strengthLevel.getValue();
    }

    /** Sets both settings at once, as a player would with the two panels. */
    public void configure(Polarity newPolarity, int level) {

        polarity.setValue(PolarityOption.valueOf(newPolarity.name()).ordinal());
        strengthLevel.setValue(level);
    }

    /** Mirrors the field into the block state, which drives the model. */
    private void showState() {

        final FieldState shown = driver.isActive() ? FieldState.of(polarityValue()) : FieldState.OFF;
        final BlockState state = getBlockState();
        if (state.getValue(HillCoreBlock.FIELD) != shown || state.getValue(HillCoreBlock.LEVEL) != strengthLevel.getValue()) {
            level.setBlock(worldPosition,
                    state.setValue(HillCoreBlock.FIELD, shown).setValue(HillCoreBlock.LEVEL, strengthLevel.getValue()),
                    Block.UPDATE_CLIENTS);
        }
    }
}
