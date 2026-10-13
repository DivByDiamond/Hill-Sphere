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
package dev.loki.hillsphere.client.screen;

import dev.loki.hillsphere.block.HillCoreBlock;
import dev.loki.hillsphere.blockentity.setting.CoreSettings;
import dev.loki.hillsphere.client.goggles.Palette;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The strength: a bar of one cell per level that fills smoothly, so 4.25 shows as four cells and a quarter.
 * Dragging sets it, the wheel nudges it ({@value #WHEEL} a notch, {@value #FINE} with Shift).
 */
final class LevelBar extends AbstractWidget {

    private static final double WHEEL = 0.25;
    private static final double FINE = CoreSettings.LEVEL_STEP;
    private static final int GAP = 2;

    private final Consumer<Double> onChange;
    private final Runnable onDone;
    private final Supplier<int[]> ramp;
    private double level;

    LevelBar(int x, int y, int width, int height, double level, Supplier<int[]> ramp, Consumer<Double> onChange, Runnable onDone) {

        super(x, y, width, height, Component.translatable("hillsphere.core.level"));
        this.level = level;
        this.ramp = ramp;
        this.onChange = onChange;
        this.onDone = onDone;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        final int[] colours = ramp.get();
        final int cells = HillCoreBlock.MAX_LEVEL;
        final int cell = (width - GAP * (cells - 1)) / cells;
        for (int i = 0; i < cells; i++) {
            final int x = getX() + i * (cell + GAP);
            graphics.fill(x, getY(), x + cell, getY() + height, 0xFF000000 | Palette.METAL[1]);
            final double lit = Mth.clamp(level - i, 0, 1);
            final int filled = (int) Math.round(cell * lit);
            graphics.fill(x, getY(), x + filled, getY() + height, 0xFF000000 | colours[Palette.MAIN]);
            graphics.fill(x, getY(), x + filled, getY() + 2, 0xFF000000 | colours[Palette.LIGHT]);
            graphics.fill(x, getY() + height - 2, x + cell, getY() + height, Palette.argb(colours[Palette.DARK], 0.8));
        }
        final int knob = getX() + (int) Math.round((width - 1) * level / cells);
        graphics.fill(knob - 1, getY() - 3, knob + 2, getY() + height + 3, 0xFF000000 | Palette.PALE);
    }

    private void setFrom(double mouseX) {

        final double wanted = (mouseX - getX()) / width * HillCoreBlock.MAX_LEVEL;
        change(CoreSettings.clampLevel(wanted));
    }

    private void change(double next) {

        if (next != level) {
            level = next;
            onChange.accept(next);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {

        setFrom(mouseX);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {

        setFrom(mouseX);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {

        onDone.run();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {

        if (!isMouseOver(mouseX, mouseY) || scrollY == 0) {
            return false;
        }
        change(CoreSettings.clampLevel(level + Math.signum(scrollY) * (net.minecraft.client.gui.screens.Screen.hasShiftDown() ? FINE : WHEEL)));
        onDone.run();
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {

        defaultButtonNarrationText(output);
    }
}
