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
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.loki.hillsphere.block.HillCoreBlock;
import dev.loki.hillsphere.client.screen.CoreSettingsScreen;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.control.RedstoneMode;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The settings of a core: polarity, level and redstone mode. A right click on the panel face opens
 * {@link CoreSettingsScreen}, where polarity and mode are switches and the level a slider.
 *
 * <p>It derives from {@link ScrollValueBehaviour} on purpose: Create only draws the outline on the face
 * for behaviours of that class. The panel Create offers for it is switched off, since its sliders
 * suit numbers, not choices.
 */
public class CoreSettings extends ScrollValueBehaviour {

    public static final BehaviourType<CoreSettings> TYPE = new BehaviourType<>("hillsphere_settings");

    /** The level moves in steps of this size, so 4.25 is a valid level. */
    public static final double LEVEL_STEP = 0.05;

    private Polarity polarity = Polarity.ATTRACT;
    private double level = 1;
    private RedstoneMode mode = RedstoneMode.OFF_ON_SIGNAL;

    public CoreSettings(Component label, SmartBlockEntity be, ValueBoxTransform slot) {

        super(label, be, slot);
    }

    public Polarity polarity() {

        return polarity;
    }

    /** The level on the panel, 0 to {@value HillCoreBlock#MAX_LEVEL}: the ceiling that redstone may scale. */
    public double level() {

        return level;
    }

    public RedstoneMode mode() {

        return mode;
    }

    /** Sets everything at once, as a player would with the screen; a level outside the range or off the step is brought in. */
    public void configure(Polarity newPolarity, double newLevel, RedstoneMode newMode) {

        final double before = level;
        final Polarity beforePolarity = polarity;
        final RedstoneMode beforeMode = mode;
        polarity = newPolarity;
        level = clampLevel(newLevel);
        mode = newMode;
        if (level != before || polarity != beforePolarity || mode != beforeMode) {
            blockEntity.setChanged();
            blockEntity.sendData();
        }
    }

    /** The nearest valid level; anything that is not a number counts as 0. */
    public static double clampLevel(double wanted) {

        if (!Double.isFinite(wanted)) {
            return 0;
        }
        final double stepped = Math.round(wanted / LEVEL_STEP) * LEVEL_STEP;
        return Mth.clamp(Math.round(stepped * 100) / 100.0, 0, HillCoreBlock.MAX_LEVEL);
    }

    @Override
    public BehaviourType<?> getType() {

        return TYPE;
    }

    @Override
    public boolean acceptsValueSettings() {

        return false;
    }

    /** The face outline shows the panel level, the one number that says how the core runs. */
    @Override
    public String formatValue() {

        return String.format(java.util.Locale.ROOT, "%.2f", level).replaceAll("\\.?0+$", "");
    }

    @Override
    public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult) {

        super.onShortInteract(player, hand, side, hitResult);
        if (getWorld().isClientSide) {
            final CoreSettings self = this;
            CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> CoreSettingsScreen.open(self));
        }
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {

        nbt.putInt("HillPolarity", polarity.ordinal());
        nbt.putDouble("HillLevel", level);
        nbt.putInt("HillMode", mode.ordinal());
        super.write(nbt, registries, clientPacket);
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {

        polarity = pick(Polarity.values(), nbt.getInt("HillPolarity"));
        level = clampLevel(nbt.getDouble("HillLevel"));
        mode = pick(RedstoneMode.values(), nbt.getInt("HillMode"));
        super.read(nbt, registries, clientPacket);
    }

    private static <E> E pick(E[] values, int index) {

        return values[Mth.clamp(index, 0, values.length - 1)];
    }
}
