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
package dev.loki.hillsphere.client.goggles.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.hillsphere.client.goggles.GogglesMode.Layer;
import dev.loki.hillsphere.client.goggles.GogglesState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Draws the world layers of the current mode after particles, all in world coordinates (a player standing on a
 * wall sees the same picture as one standing on the floor). Uses its own buffers, so lines and quads can be
 * mixed freely, and caps the shapes drawn in one frame.
 */
public final class WorldOverlay {

    private static final int SHAPES_PER_FRAME = 20_000;

    private final GogglesState state;
    private final Map<Layer, WorldLayer> layers;
    private final MultiBufferSource.BufferSource buffers;

    public WorldOverlay(GogglesState state, Map<Layer, WorldLayer> layers) {

        this.state = state;
        this.layers = layers;
        final SequencedMap<RenderType, ByteBufferBuilder> fixed = new LinkedHashMap<>();
        fixed.put(OverlayTypes.FILLS, new ByteBufferBuilder(OverlayTypes.FILLS.bufferSize()));
        fixed.put(OverlayTypes.LINES, new ByteBufferBuilder(OverlayTypes.LINES.bufferSize()));
        this.buffers = MultiBufferSource.immediateWithBuffers(fixed, new ByteBufferBuilder(256));
    }

    public void tick() {

        layers.forEach((layer, drawer) -> {
            if (state.shows(layer)) {
                drawer.tick(state);
            }
        });
    }

    public void onRender(RenderLevelStageEvent event) {

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !state.worn()) {
            return;
        }
        final float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        final PoseStack stack = event.getPoseStack();
        final Brush brush = new Brush(buffers.getBuffer(OverlayTypes.LINES), buffers.getBuffer(OverlayTypes.FILLS),
                stack.last(), event.getCamera().getPosition(), SHAPES_PER_FRAME);
        layers.forEach((layer, drawer) -> {
            if (state.shows(layer) && brush.hasBudget()) {
                drawer.render(brush, state, partialTick);
            }
        });
        buffers.endBatch();
    }
}
