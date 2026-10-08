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
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import dev.loki.hillsphere.block.HillCoreBlock;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.control.RedstoneMode;

import java.util.Locale;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * All the settings of a core on one panel: polarity, level and redstone mode, one row each. Create sizes
 * the panel by the largest value, so every option sits {@value #STEP} values apart, which makes a wide bar
 * with a notch for each choice; rows with fewer choices stop at their last one.
 */
public class CoreSettings extends BlockEntityBehaviour implements ValueSettingsBehaviour {

    public static final BehaviourType<CoreSettings> TYPE = new BehaviourType<>("hillsphere_settings");

    private static final int STEP = 10;
    private static final int ROW_POLARITY = 0;
    private static final int ROW_LEVEL = 1;
    private static final int ROW_MODE = 2;

    private final Component label;
    private final ValueBoxTransform slot;
    private Polarity polarity = Polarity.ATTRACT;
    private int level = 1;
    private RedstoneMode mode = RedstoneMode.OFF_ON_SIGNAL;
    private int lastRow = ROW_LEVEL;

    public CoreSettings(Component label, SmartBlockEntity be, ValueBoxTransform slot) {

        super(be);
        this.label = label;
        this.slot = slot;
    }

    public Polarity polarity() {

        return polarity;
    }

    /** The level on the panel, 0 to {@value HillCoreBlock#MAX_LEVEL}: the ceiling that redstone may scale. */
    public int level() {

        return level;
    }

    public RedstoneMode mode() {

        return mode;
    }

    /** Sets everything at once, as a player would with the panel. */
    public void configure(Polarity newPolarity, int newLevel, RedstoneMode newMode) {

        polarity = newPolarity;
        level = Mth.clamp(newLevel, 0, HillCoreBlock.MAX_LEVEL);
        mode = newMode;
        changed();
    }

    @Override
    public BehaviourType<?> getType() {

        return TYPE;
    }

    @Override
    public boolean isSafeNBT() {

        return true;
    }

    @Override
    public boolean isActive() {

        return true;
    }

    @Override
    public boolean testHit(Vec3 hit) {

        final Vec3 local = hit.subtract(Vec3.atLowerCornerOf(getPos()));
        return slot.testHit(getWorld(), getPos(), blockEntity.getBlockState(), local);
    }

    @Override
    public ValueBoxTransform getSlotPositioning() {

        return slot;
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {

        return new ValueSettingsBoard(label, HillCoreBlock.MAX_LEVEL * STEP, STEP,
                ImmutableList.of(Component.translatable("hillsphere.core.polarity"), Component.translatable("hillsphere.core.level"),
                        Component.translatable("hillsphere.core.mode")),
                new ValueSettingsFormatter(this::format));
    }

    @Override
    public ValueSettings getValueSettings() {

        return new ValueSettings(lastRow, STEP * switch (lastRow) {
            case ROW_POLARITY -> polarity.ordinal();
            case ROW_MODE -> mode.ordinal();
            default -> level;
        });
    }

    @Override
    public void setValueSettings(Player player, ValueSettings settings, boolean ctrlDown) {

        final int before = fingerprint(polarity, level, mode);
        final int choice = Math.round(settings.value() / (float) STEP);
        lastRow = Mth.clamp(settings.row(), ROW_POLARITY, ROW_MODE);
        switch (lastRow) {
            case ROW_POLARITY -> polarity = Polarity.values()[Mth.clamp(choice, 0, Polarity.values().length - 1)];
            case ROW_MODE -> mode = RedstoneMode.values()[Mth.clamp(choice, 0, RedstoneMode.values().length - 1)];
            default -> level = Mth.clamp(choice, 0, HillCoreBlock.MAX_LEVEL);
        }
        if (fingerprint(polarity, level, mode) != before) {
            playFeedbackSound(this);
            changed();
        }
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {

        nbt.putInt("HillPolarity", polarity.ordinal());
        nbt.putInt("HillLevel", level);
        nbt.putInt("HillMode", mode.ordinal());
        super.write(nbt, registries, clientPacket);
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {

        polarity = Polarity.values()[Mth.clamp(nbt.getInt("HillPolarity"), 0, Polarity.values().length - 1)];
        level = Mth.clamp(nbt.getInt("HillLevel"), 0, HillCoreBlock.MAX_LEVEL);
        mode = RedstoneMode.values()[Mth.clamp(nbt.getInt("HillMode"), 0, RedstoneMode.values().length - 1)];
        super.read(nbt, registries, clientPacket);
    }

    private void changed() {

        blockEntity.setChanged();
        blockEntity.sendData();
    }

    private static int fingerprint(Polarity polarity, int level, RedstoneMode mode) {

        return (polarity.ordinal() * 16 + level) * 16 + mode.ordinal();
    }

    private MutableComponent format(ValueSettings settings) {

        final int choice = Math.round(settings.value() / (float) STEP);
        return switch (settings.row()) {
            case ROW_POLARITY -> name("polarity", Polarity.values()[Mth.clamp(choice, 0, Polarity.values().length - 1)].name());
            case ROW_MODE -> name("mode", RedstoneMode.values()[Mth.clamp(choice, 0, RedstoneMode.values().length - 1)].name());
            default -> Component.literal(String.valueOf(Mth.clamp(choice, 0, HillCoreBlock.MAX_LEVEL)));
        };
    }

    private static MutableComponent name(String group, String option) {

        return Component.translatable("hillsphere.core." + group + "." + option.toLowerCase(Locale.ROOT));
    }
}
