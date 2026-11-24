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
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The core's lines in Create's goggle tooltip (shown with Create's goggles or ours), laid out like Create's own:
 * a white heading, then grey names with values in colour, indented as Create indents them. Create itself adds the
 * stress the core draws below.
 */
public final class CoreTooltip {

    private static final String INDENT = "    ";
    private static final String LINE = "     ";

    private CoreTooltip() {
    }

    public static void addTo(List<Component> tooltip, HillCoreBlockEntity core) {

        final CoreStatus status = CoreStatus.of(core);
        final int colour = (status.working() ? Palette.of(status.polarity()) : Palette.OFF)[Palette.LIGHT];
        tooltip.add(Component.literal(INDENT).append(Component.translatable("hillsphere.core.settings")).append(":"));
        tooltip.add(Component.literal(LINE).append(Component.translatable(status.stateKey()).withColor(colour)));
        row(tooltip, "polarity", Component.translatable("hillsphere.core.polarity."
                + status.polarity().name().toLowerCase(Locale.ROOT)).withColor(Palette.of(status.polarity())[Palette.LIGHT]));
        row(tooltip, "level", Component.literal(String.format(Locale.ROOT, "%.2f / %.2f", status.asked().level(), status.panelLevel()))
                .withStyle(ChatFormatting.GOLD));
        row(tooltip, "radius", Component.translatable("hillsphere.goggles.blocks", String.format(Locale.ROOT, "%.1f", status.radius()))
                .withStyle(ChatFormatting.AQUA));
        if (!status.tuning().isSpinningFastEnough(status.rpm())) {
            row(tooltip, "speed", Component.translatable("hillsphere.goggles.needs_rpm",
                    String.format(Locale.ROOT, "%.0f", status.tuning().minRpm())).withStyle(ChatFormatting.RED));
        }
        row(tooltip, "redstone", Component.translatable("hillsphere.core.mode." + status.mode().name().toLowerCase(Locale.ROOT))
                .withStyle(ChatFormatting.WHITE));
    }

    private static void row(List<Component> tooltip, String name, MutableComponent value) {

        tooltip.add(Component.literal(LINE).append(Component.translatable("hillsphere.goggles.panel." + name)
                .withStyle(ChatFormatting.GRAY)).append(Component.literal(" ")).append(value));
    }
}
