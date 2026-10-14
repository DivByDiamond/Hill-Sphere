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

import dev.loki.hillsphere.client.goggles.Palette;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * One setting with a few named values, such as the redstone mode or the shape: the name of the value between two
 * arrows, one pip per value; the arrows, a click and the wheel all page through.
 */
final class ChoiceRow<E extends Enum<E>> extends AbstractWidget {

    private final E[] all;
    private final String key;
    private final Consumer<E> onPick;
    private final Supplier<int[]> ramp;
    private E picked;

    /** {@code key} is the language key of the setting; each value's name is under it, in lower case. */
    ChoiceRow(int x, int y, int width, int height, E picked, String key, Supplier<int[]> ramp, Consumer<E> onPick) {

        super(x, y, width, height, Component.translatable(key));
        this.all = picked.getDeclaringClass().getEnumConstants();
        this.key = key;
        this.picked = picked;
        this.ramp = ramp;
        this.onPick = onPick;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        final var font = Minecraft.getInstance().font;
        final int[] colours = ramp.get();
        graphics.fill(getX(), getY(), getX() + width, getY() + height, Palette.argb(Palette.METAL[1], 0.95));
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, 0xFF000000 | colours[Palette.DARK]);
        final int middle = getY() + (height - 8) / 2 - 2;
        final boolean left = mouseX < getX() + width / 2;
        final boolean hover = isHovered();
        graphics.drawString(font, "<", getX() + 7, middle, 0xFF000000 | (hover && left ? colours[Palette.LIGHT] : Palette.METAL[4]));
        graphics.drawString(font, ">", getX() + width - 13, middle, 0xFF000000 | (hover && !left ? colours[Palette.LIGHT] : Palette.METAL[4]));
        graphics.drawCenteredString(font, Component.translatable(key + "." + picked.name().toLowerCase(Locale.ROOT)),
                getX() + width / 2, middle, 0xFF000000 | Palette.PALE);
        final int count = all.length;
        final int start = getX() + (width - count * 6 + 2) / 2;
        for (int i = 0; i < count; i++) {
            graphics.fill(start + i * 6, getY() + height - 6, start + i * 6 + 4, getY() + height - 4,
                    0xFF000000 | (i == picked.ordinal() ? colours[Palette.MAIN] : Palette.METAL[3]));
        }
    }

    private void step(int by) {

        picked = all[Math.floorMod(picked.ordinal() + by, all.length)];
        onPick.accept(picked);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {

        step(mouseX < getX() + width / 2.0 ? -1 : 1);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {

        if (!isMouseOver(mouseX, mouseY) || scrollY == 0) {
            return false;
        }
        step(scrollY > 0 ? -1 : 1);
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {

        defaultButtonNarrationText(output);
    }
}
