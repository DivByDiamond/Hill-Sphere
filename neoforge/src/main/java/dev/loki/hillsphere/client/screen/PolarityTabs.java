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
import dev.loki.hillsphere.field.Polarity;

import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** One tab per polarity, each in its own colour; the picked one glows. */
final class PolarityTabs extends AbstractWidget {

    private static final int GAP = 3;

    private final Consumer<Polarity> onPick;
    private Polarity picked;

    PolarityTabs(int x, int y, int width, int height, Polarity picked, Consumer<Polarity> onPick) {

        super(x, y, width, height, Component.translatable("hillsphere.core.polarity"));
        this.picked = picked;
        this.onPick = onPick;
    }

    private int tabWidth() {

        return (width - GAP * (Polarity.values().length - 1)) / Polarity.values().length;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        final Polarity[] all = Polarity.values();
        for (int i = 0; i < all.length; i++) {
            final int x = getX() + i * (tabWidth() + GAP);
            final int[] ramp = Palette.of(all[i]);
            final boolean on = all[i] == picked;
            final boolean hover = mouseX >= x && mouseX < x + tabWidth() && mouseY >= getY() && mouseY < getY() + height;
            graphics.fill(x, getY(), x + tabWidth(), getY() + height,
                    on ? Palette.argb(ramp[Palette.DARK], 0.9) : Palette.argb(Palette.METAL[hover ? 2 : 1], 0.95));
            graphics.fill(x, getY(), x + tabWidth(), getY() + 2, 0xFF000000 | (on ? ramp[Palette.LIGHT] : Palette.METAL[3]));
            if (on) {
                graphics.renderOutline(x - 1, getY() - 1, tabWidth() + 2, height + 2, Palette.argb(ramp[Palette.MAIN], 0.9));
            }
            final Component name = Component.translatable("hillsphere.core.polarity." + all[i].name().toLowerCase(Locale.ROOT));
            graphics.drawCenteredString(Minecraft.getInstance().font, name, x + tabWidth() / 2, getY() + (height - 6) / 2,
                    0xFF000000 | (on ? ramp[Palette.LIGHT] : hover ? Palette.PALE : Palette.METAL[4]));
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {

        final int index = (int) ((mouseX - getX()) / (tabWidth() + GAP));
        final Polarity next = Polarity.values()[Math.max(0, Math.min(index, Polarity.values().length - 1))];
        if (next != picked) {
            picked = next;
            onPick.accept(next);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {

        defaultButtonNarrationText(output);
    }
}
