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

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A setting with a few levels. Create sizes its settings panel by the number of values, so a
 * plain 1 to 4 would be a tiny bar; here every level sits {@value #STEP} values apart, which
 * gives a wide bar with a notch for each level. It has its own behaviour type, see {@link PolarityBehaviour}.
 */
public class StepSettingBehaviour extends ScrollValueBehaviour {

    public static final BehaviourType<StepSettingBehaviour> TYPE = new BehaviourType<>("hillsphere_level");

    private static final String KEY = "HillLevel";
    private static final int STEP = 10;

    public StepSettingBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot, int levels) {

        super(label, be, slot);
        between(1, levels);
        value = 1;
    }

    /** Create sends a changed value to the setting with this id; see {@link PolarityBehaviour#netId()}. */
    @Override
    public int netId() {

        return 2;
    }

    @Override
    public BehaviourType<?> getType() {

        return TYPE;
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {

        return new ValueSettingsBoard(label, (max - 1) * STEP, STEP, ImmutableList.of(Component.literal("Level")),
                new ValueSettingsFormatter(this::format));
    }

    @Override
    public void setValueSettings(Player player, ValueSettings settings, boolean ctrlDown) {

        final int level = levelOf(settings);
        if (level != getValue()) {
            playFeedbackSound(this);
        }
        setValue(level);
    }

    @Override
    public ValueSettings getValueSettings() {

        return new ValueSettings(0, (getValue() - 1) * STEP);
    }

    /** Saved under its own key: the inherited one is shared by every scroll setting of the block. */
    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {

        nbt.putInt(KEY, value);
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {

        value = Mth.clamp(nbt.getInt(KEY), 1, max);
    }

    private int levelOf(ValueSettings settings) {

        return Mth.clamp(Math.round(settings.value() / (float) STEP) + 1, 1, max);
    }

    private MutableComponent format(ValueSettings settings) {

        return Component.literal(String.valueOf(levelOf(settings)));
    }
}
