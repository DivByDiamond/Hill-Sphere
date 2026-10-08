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

import dev.loki.hillsphere.blockentity.HillCoreBlockEntity;
import dev.loki.hillsphere.client.goggles.Palette;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A small panel next to the crosshair while a core is in sight: its state in the core's colour, bars for speed
 * (with a mark where the field starts), radius and load, six cells for the level (dim: set on the panel,
 * bright: what it runs at now), and the redstone mode.
 */
final class CorePanel {

    private static final int WIDTH = 152;
    private static final int ROW = 11;
    private static final int PAD = 5;
    private static final int BAR_X = 52;
    private static final int BAR_WIDTH = 50;
    private static final int TEXT = 0xFF000000 | Palette.PALE;
    private static final int MUTED = 0xFF000000 | Palette.METAL[4];

    private CorePanel() {
    }

    static void render(GuiGraphics graphics) {

        final Minecraft mc = Minecraft.getInstance();
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !(mc.level.getBlockEntity(hit.getBlockPos()) instanceof HillCoreBlockEntity core)) {
            return;
        }
        final CoreStatus status = CoreStatus.of(core);
        final int[] ramp = status.working() ? Palette.of(status.polarity()) : Palette.OFF;
        final Font font = mc.font;
        final int left = graphics.guiWidth() / 2 + 16;
        final int top = graphics.guiHeight() / 2 - 34;
        graphics.fill(left, top, left + WIDTH, top + PAD * 2 + ROW * 6, Palette.argb(Palette.METAL[0], 0.8));
        graphics.fill(left, top, left + 2, top + PAD * 2 + ROW * 6, 0xFF000000 | ramp[Palette.MAIN]);
        final int x = left + PAD + 2;
        int y = top + PAD;
        graphics.drawString(font, Component.translatable(status.stateKey()), x, y, 0xFF000000 | ramp[Palette.LIGHT]);
        y += ROW;
        bar(graphics, font, x, y, "speed", status.rpm() / status.tuning().maxRpm(), String.format(Locale.ROOT, "%.0f", status.rpm()), ramp);
        tick(graphics, x + BAR_X + (int) (BAR_WIDTH * status.tuning().minRpm() / status.tuning().maxRpm()), y);
        y += ROW;
        bar(graphics, font, x, y, "radius", status.radius() / status.tuning().maxRadius(), String.format(Locale.ROOT, "%.1f", status.radius()), ramp);
        y += ROW;
        bar(graphics, font, x, y, "load", status.stress() / Math.max(1, status.maxStress()), String.format(Locale.ROOT, "%.0f", status.stress()), ramp);
        y += ROW;
        levels(graphics, font, x, y, status, ramp);
        y += ROW;
        graphics.drawString(font, label("redstone"), x, y, MUTED);
        graphics.drawString(font, Component.translatable("hillsphere.core.mode." + status.mode().name().toLowerCase(Locale.ROOT)),
                x + BAR_X, y, TEXT);
    }

    private static void bar(GuiGraphics graphics, Font font, int x, int y, String name, double share, String value, int[] ramp) {

        graphics.drawString(font, label(name), x, y, MUTED);
        final int bx = x + BAR_X;
        graphics.fill(bx, y + 2, bx + BAR_WIDTH, y + 7, 0xFF000000 | Palette.METAL[1]);
        final int filled = (int) Math.round(BAR_WIDTH * Math.max(0, Math.min(1, share)));
        graphics.fill(bx, y + 2, bx + filled, y + 7, 0xFF000000 | ramp[Palette.MAIN]);
        graphics.fill(bx, y + 2, bx + filled, y + 3, 0xFF000000 | ramp[Palette.LIGHT]);
        graphics.drawString(font, value, bx + BAR_WIDTH + 4, y, TEXT);
    }

    private static void tick(GuiGraphics graphics, int x, int y) {

        graphics.fill(x, y + 1, x + 1, y + 8, 0xFF000000 | Palette.PALE);
    }

    /** Six cells: dim up to the panel's level, bright up to the level the core runs at now (fractions too). */
    private static void levels(GuiGraphics graphics, Font font, int x, int y, CoreStatus status, int[] ramp) {

        graphics.drawString(font, label("level"), x, y, MUTED);
        final int cells = status.tuning().levels();
        final int cell = (BAR_WIDTH + 2) / cells - 2;
        for (int i = 0; i < cells; i++) {
            final int cx = x + BAR_X + i * (cell + 2);
            final int base = i < status.panelLevel() ? ramp[Palette.DARK] : Palette.METAL[1];
            graphics.fill(cx, y + 2, cx + cell, y + 7, 0xFF000000 | base);
            final double lit = Math.max(0, Math.min(1, status.asked().level() - i));
            graphics.fill(cx, y + 2, cx + (int) Math.round(cell * lit), y + 7, 0xFF000000 | ramp[Palette.LIGHT]);
        }
        graphics.drawString(font, String.format(Locale.ROOT, "%d/%.1f", status.panelLevel(), status.asked().level()),
                x + BAR_X + BAR_WIDTH + 4, y, TEXT);
    }

    private static Component label(String name) {

        return Component.translatable("hillsphere.goggles.panel." + name);
    }
}
