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
import dev.loki.hillsphere.compat.Ships;
import dev.loki.hillsphere.config.HillSphereConfig;
import dev.loki.hillsphere.field.CoreDriver;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.control.Control;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;
import dev.loki.hillsphere.field.shape.Deck;
import dev.loki.hillsphere.field.shape.Shape;
import dev.loki.hillsphere.registry.ModBlockEntities;
import dev.loki.hillsphere.world.LevelFields;
import dev.loki.hillsphere.world.WorldFields;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Turns the rotation a core receives and its settings into a gravity field. The field is always
 * centred where the core is in the world, so a core on a ship takes its field along with the ship; a deck-shaped
 * field on a ship fills the ship's whole hull.
 */
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

        final double level = settings == null ? 0 : settings.level();
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
        final Vec3d center = Ships.toWorld(level, new Vec3d(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5));
        final CoreField field = driver.step(HillSphereConfig.tuning(), center, control.polarity(), control.level(),
                Math.abs(getSpeed()), !isOverStressed());
        WorldFields.of(level).update(p, settings.shape() == Shape.DECK ? field.withDeck(deck(center, field.radius())) : field);
        showState();
    }

    /** Destroyed, moved onto a ship or unloaded with its chunk (or its ship): either way its field is gone. */
    @Override
    public void invalidate() {

        final LevelFields fields = level == null || level.isClientSide ? null : WorldFields.peek(level);
        if (fields != null) {
            fields.remove(worldPosition);
        }
        super.invalidate();
    }

    /** A deck fills the hull of the ship the core is on, or a cube as big as the field's radius on the ground. */
    private Deck deck(Vec3d center, double radius) {

        final Deck hull = Ships.hullOf(level, worldPosition);
        return hull != null ? hull : Deck.cube(center, radius);
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
