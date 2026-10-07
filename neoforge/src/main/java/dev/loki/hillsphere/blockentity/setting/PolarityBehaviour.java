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

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The polarity choice of a core. It needs a type of its own: a block entity keeps its
 * behaviours by type, so two settings of the same type would replace each other.
 */
public class PolarityBehaviour extends ScrollOptionBehaviour<PolarityOption> {

    private static final String KEY = "HillPolarity";

    public static final BehaviourType<PolarityBehaviour> TYPE = new BehaviourType<>("hillsphere_polarity");

    public PolarityBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {

        super(PolarityOption.class, label, be, slot);
    }

    /** Create sends a changed value to the setting with this id; the default 0 would reach whichever comes first. */
    @Override
    public int netId() {

        return 1;
    }

    @Override
    public BehaviourType<?> getType() {

        return TYPE;
    }

    /** Saved under its own key: the inherited one is shared by every scroll setting of the block. */
    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {

        nbt.putInt(KEY, value);
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {

        value = Mth.clamp(nbt.getInt(KEY), 0, PolarityOption.values().length - 1);
    }
}
