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
package dev.loki.hillsphere.client;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.loki.hillsphere.blockentity.HillCoreBlockEntity;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/** The shaft when Flywheel is off. With Flywheel the visual draws it instead. */
public class HillCoreRenderer extends KineticBlockEntityRenderer<HillCoreBlockEntity> {

    public HillCoreRenderer(BlockEntityRendererProvider.Context context) {

        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(HillCoreBlockEntity be, BlockState state) {

        return CachedBuffers.partial(ModPartials.ROTOR, state);
    }
}
