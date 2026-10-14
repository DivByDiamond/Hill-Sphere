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

import dev.loki.hillsphere.blockentity.setting.CoreSettings;
import dev.loki.hillsphere.client.goggles.Palette;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.control.RedstoneMode;
import dev.loki.hillsphere.field.shape.Shape;
import dev.loki.hillsphere.network.CoreSettingsPayload;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The screen of a core, in the colours of the goggles: polarity tabs, the redstone mode, the shape and a bar for the
 * strength, all tinted by the polarity picked. A change goes to the server at once for tabs and mode, when
 * the bar is let go, and for anything still unsent when the screen closes.
 */
public final class CoreSettingsScreen extends Screen {

    private static final int WIDTH = 232;
    private static final int HEIGHT = 188;
    private static final int PAD = 12;

    private final BlockPos pos;
    private Polarity polarity;
    private RedstoneMode mode;
    private Shape shape;
    private double level;
    private boolean unsent;

    private CoreSettingsScreen(CoreSettings settings) {

        super(Component.translatable("hillsphere.core.settings"));
        pos = settings.blockEntity.getBlockPos();
        polarity = settings.polarity();
        mode = settings.mode();
        shape = settings.shape();
        level = settings.level();
    }

    public static void open(CoreSettings settings) {

        Minecraft.getInstance().setScreen(new CoreSettingsScreen(settings));
    }

    private int[] ramp() {

        return Palette.of(polarity);
    }

    @Override
    protected void init() {

        final int left = (width - WIDTH) / 2 + PAD;
        final int top = (height - HEIGHT) / 2;
        final int inner = WIDTH - 2 * PAD;
        addRenderableWidget(new PolarityTabs(left, top + 38, inner, 20, polarity, p -> {
            polarity = p;
            send();
        }));
        addRenderableWidget(new ChoiceRow<>(left, top + 76, inner, 22, mode, "hillsphere.core.mode", this::ramp, m -> {
            mode = m;
            send();
        }));
        addRenderableWidget(new ChoiceRow<>(left, top + 114, inner, 22, shape, "hillsphere.core.shape", this::ramp, s -> {
            shape = s;
            send();
        }));
        addRenderableWidget(new LevelBar(left, top + 158, inner, 12, level, this::ramp, v -> {
            level = v;
            unsent = true;
        }, () -> {
            if (unsent) {
                send();
            }
        }));
    }

    /** Dim and panel go behind the widgets; the vanilla blur would smear everything but the buttons. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        renderTransparentBackground(graphics);
        final int left = (width - WIDTH) / 2;
        final int top = (height - HEIGHT) / 2;
        final int[] colours = ramp();
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, Palette.argb(Palette.METAL[0], 0.94));
        graphics.renderOutline(left, top, WIDTH, HEIGHT, 0xFF000000 | Palette.METAL[3]);
        graphics.fill(left, top, left + WIDTH, top + 3, 0xFF000000 | colours[Palette.MAIN]);
        graphics.fill(left + 3, top + 3, left + WIDTH - 3, top + 4, Palette.argb(colours[Palette.LIGHT], 0.35));
        graphics.drawString(font, title, left + PAD, top + 12, 0xFF000000 | Palette.PALE);
        caption(graphics, left + PAD, top + 28, "polarity");
        caption(graphics, left + PAD, top + 66, "mode");
        caption(graphics, left + PAD, top + 104, "shape");
        caption(graphics, left + PAD, top + 144, "level");
        final String value = String.format(Locale.ROOT, "%.2f", level);
        graphics.drawString(font, value, left + WIDTH - PAD - font.width(value), top + 144, 0xFF000000 | colours[Palette.LIGHT]);
    }

    private void caption(GuiGraphics graphics, int x, int y, String key) {

        graphics.drawString(font, Component.translatable("hillsphere.core." + key).getString().toUpperCase(Locale.ROOT), x, y,
                0xFF000000 | Palette.METAL[4], false);
    }

    @Override
    public void removed() {

        if (unsent) {
            send();
        }
    }

    private void send() {

        unsent = false;
        PacketDistributor.sendToServer(new CoreSettingsPayload(pos, polarity, level, mode, shape));
    }
}
