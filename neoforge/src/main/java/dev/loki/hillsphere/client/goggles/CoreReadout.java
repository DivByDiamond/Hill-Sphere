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
package dev.loki.hillsphere.client.goggles;

import dev.loki.hillsphere.blockentity.HillCoreBlockEntity;
import dev.loki.hillsphere.blockentity.setting.CoreSettings;
import dev.loki.hillsphere.config.HillSphereConfig;
import dev.loki.hillsphere.field.FieldTuning;
import dev.loki.hillsphere.field.control.Control;
import dev.loki.hillsphere.item.HillGogglesItem;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** With the goggles on, a core under the crosshair shows its speed, radius, load and why it is idle. */
public final class CoreReadout {

    private static final int COLOR = 0xFFE8E6FF;
    private static final int LINE = 11;

    private CoreReadout() {
    }

    public static void onRender(RenderGuiEvent.Post event) {

        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof HillGogglesItem)
                || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !(mc.level.getBlockEntity(hit.getBlockPos()) instanceof HillCoreBlockEntity core)) {
            return;
        }
        final GuiGraphics graphics = event.getGuiGraphics();
        final int x = graphics.guiWidth() / 2 + 14;
        int y = graphics.guiHeight() / 2 - 14;
        for (final Component line : lines(core)) {
            graphics.drawString(mc.font, line, x, y, COLOR);
            y += LINE;
        }
    }

    private static List<Component> lines(HillCoreBlockEntity core) {

        final FieldTuning tuning = HillSphereConfig.tuning();
        final double rpm = Math.abs(core.getSpeed());
        return List.of(
                Component.translatable("hillsphere.readout.state", Component.translatable(status(core, tuning, rpm))),
                Component.translatable("hillsphere.readout.mode", Component.translatable("hillsphere.core.mode." + core.settings().mode().name().toLowerCase(Locale.ROOT))),
                Component.translatable("hillsphere.readout.level", format(core.settings().level()), format(asked(core).level())),
                Component.translatable("hillsphere.readout.speed", format(rpm)),
                Component.translatable("hillsphere.readout.radius", format(tuning.radius(rpm))),
                Component.translatable("hillsphere.readout.load", format(tuning.stress(rpm, core.settings().level()))));
    }

    private static String status(HillCoreBlockEntity core, FieldTuning tuning, double rpm) {

        if (asked(core).level() <= 0) {
            return core.settings().level() == 0 ? "hillsphere.readout.zero" : "hillsphere.readout.redstone";
        }
        if (core.isOverStressed()) {
            return "hillsphere.readout.overstressed";
        }
        return tuning.isSpinningFastEnough(rpm) ? "hillsphere.readout.working" : "hillsphere.readout.slow";
    }

    /** What the panel and the redstone around the core ask of it; worked out here because only the server ticks it. */
    private static Control asked(HillCoreBlockEntity core) {

        final CoreSettings settings = core.settings();
        return settings.mode().apply(settings.polarity(), settings.level(), core.getLevel().getBestNeighborSignal(core.getBlockPos()));
    }

    private static String format(double value) {

        return String.format(Locale.ROOT, "%.1f", value);
    }
}
