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
import dev.loki.hillsphere.blockentity.setting.CoreSettings;
import dev.loki.hillsphere.config.HillSphereConfig;
import dev.loki.hillsphere.field.CoreDriver;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.control.Control;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.registry.ModBlockEntities;
import dev.loki.hillsphere.world.WorldFields;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Turns the rotation a core receives and its settings into a gravity field. */
public class HillCoreBlockEntity extends KineticBlockEntity {

    private final CoreDriver driver = new CoreDriver();
    private CoreSettings settings;
    private Control control = new Control(Polarity.ATTRACT, 0);

    public HillCoreBlockEntity(BlockPos pos, BlockState state) {

        super(ModBlockEntities.HILL_CORE.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

        super.addBehaviours(behaviours);
        settings = new CoreSettings(Component.translatable("hillsphere.core.settings"), this, new CoreSettingSlot());
        behaviours.add(settings);
    }

    /** The load follows the level on the panel, the most the core can be asked for, so redstone cannot overstress the network. */
    @Override
    public float calculateStressApplied() {

        final int level = settings == null ? 0 : settings.level();
        lastStressApplied = (float) (HillSphereConfig.tuning().stressPerRpmPerLevel() * level);
        return lastStressApplied;
    }

    @Override
    public void tick() {

        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        final int signal = level.getBestNeighborSignal(worldPosition);
        control = settings.mode().apply(settings.polarity(), settings.level(), signal);
        final BlockPos p = worldPosition;
        WorldFields.of(level).update(p, driver.step(HillSphereConfig.tuning(),
                new Vec3d(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5), control.polarity(), control.level(),
                Math.abs(getSpeed()), !isOverStressed()));
        showState();
    }

    @Override
    public void remove() {

        if (level != null && !level.isClientSide) {
            WorldFields.of(level).remove(worldPosition);
        }
        super.remove();
    }

    public CoreSettings settings() {

        return settings;
    }

    /** What redstone and the panel ask of the core right now; updated every server tick. */
    public Control control() {

        return control;
    }

    /** Mirrors the field into the block state, which drives the model. */
    private void showState() {

        final FieldState shown = driver.isActive() ? FieldState.of(control.polarity()) : FieldState.OFF;
        final int pips = Mth.clamp((int) Math.round(control.level()), 0, HillCoreBlock.MAX_LEVEL);
        final BlockState state = getBlockState();
        if (state.getValue(HillCoreBlock.FIELD) != shown || state.getValue(HillCoreBlock.LEVEL) != pips) {
            level.setBlock(worldPosition, state.setValue(HillCoreBlock.FIELD, shown).setValue(HillCoreBlock.LEVEL, pips),
                    Block.UPDATE_CLIENTS);
        }
    }
}
