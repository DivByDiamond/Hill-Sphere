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
package dev.loki.hillsphere.client.goggles.hud;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.loki.hillsphere.client.goggles.GogglesMode.Layer;
import dev.loki.hillsphere.client.goggles.GogglesState;
import dev.loki.hillsphere.client.goggles.Palette;
import dev.loki.hillsphere.field.view.FieldProbe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

/**
 * Everything the goggles put on the screen: a tint at the screen edges in the colour of the field around the
 * wearer, the "down" compass, the panel of the core in sight and the name of the current mode in the corner.
 */
public final class HudOverlay {

    private static final double VIGNETTE = 0.28;
    private static final int LABEL_MARGIN = 4;

    private final GogglesState state;

    public HudOverlay(GogglesState state) {

        this.state = state;
    }

    public void render(GuiGraphics graphics, float partialTick) {

        final Minecraft mc = Minecraft.getInstance();
        if (!state.worn() || mc.options.hideGui || mc.level == null) {
            return;
        }
        if (state.shows(Layer.VIGNETTE)) {
            vignette(graphics, state.probe(state.eye()), state.time(partialTick));
        }
        if (state.shows(Layer.COMPASS)) {
            DownCompass.render(graphics, state);
        }
        if (state.shows(Layer.PANEL)) {
            CorePanel.render(graphics);
        }
        label(graphics, mc, state.modeBanner(partialTick));
    }

    /** Mode name in the upper left corner; brighter, with a backing, right after a switch. */
    private void label(GuiGraphics graphics, Minecraft mc, double banner) {

        final Component text = Component.translatable("hillsphere.goggles.mode",
                Component.translatable(state.mode().translationKey()));
        if (banner > 0) {
            graphics.fill(LABEL_MARGIN - 2, LABEL_MARGIN - 2, LABEL_MARGIN + mc.font.width(text) + 2, LABEL_MARGIN + 10,
                    Palette.argb(Palette.METAL[0], 0.7 * banner));
        }
        final int colour = Palette.mix(Palette.METAL[4], Palette.PALE, banner);
        graphics.drawString(mc.font, text, LABEL_MARGIN, LABEL_MARGIN, Palette.argb(colour, 0.6 + 0.4 * banner));
    }

    /** Four bands fading inwards from the screen edges. */
    private static void vignette(GuiGraphics graphics, FieldProbe probe, double time) {

        if (!probe.inField()) {
            return;
        }
        final double alpha = VIGNETTE * probe.influence() * (0.85 + 0.15 * Math.sin(time * 0.08));
        final int outer = Palette.argb(Palette.of(probe.polarity())[Palette.MAIN], alpha);
        final int inner = Palette.argb(Palette.of(probe.polarity())[Palette.MAIN], 0);
        final float w = graphics.guiWidth();
        final float h = graphics.guiHeight();
        final float band = Math.min(w, h) * 0.14f;
        final Matrix4f pose = graphics.pose().last().pose();
        final VertexConsumer quads = graphics.bufferSource().getBuffer(RenderType.gui());
        band(quads, pose, new float[] {0, 0, w, 0, w - band, band, band, band}, outer, inner);
        band(quads, pose, new float[] {w, h, 0, h, band, h - band, w - band, h - band}, outer, inner);
        band(quads, pose, new float[] {0, h, 0, 0, band, band, band, h - band}, outer, inner);
        band(quads, pose, new float[] {w, 0, w, h, w - band, h - band, w - band, band}, outer, inner);
        graphics.flush();
    }

    /** Two outer corners then two inner ones, as x, y pairs. */
    private static void band(VertexConsumer quads, Matrix4f pose, float[] xy, int outer, int inner) {

        for (int i = 0; i < 4; i++) {
            quads.addVertex(pose, xy[i * 2], xy[i * 2 + 1], 0).setColor(i < 2 ? outer : inner);
        }
    }
}
