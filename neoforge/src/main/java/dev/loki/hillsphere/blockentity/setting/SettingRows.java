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
package dev.loki.hillsphere.blockentity.setting;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import dev.loki.hillsphere.block.HillCoreBlock;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.control.RedstoneMode;

import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

/** The rows of the core panel and how a raw value on them turns into a choice, its name and back. */
final class SettingRows {

    /** Distance between two neighbouring choices on a row. */
    static final int STEP = 10;
    static final int POLARITY = 0;
    static final int LEVEL = 1;
    static final int MODE = 2;

    private SettingRows() {
    }

    static List<Component> labels() {

        return List.of(Component.translatable("hillsphere.core.polarity"), Component.translatable("hillsphere.core.level"),
                Component.translatable("hillsphere.core.mode"));
    }

    /** The choice index a panel value points at. */
    static int choice(ValueSettings settings) {

        return Math.round(settings.value() / (float) STEP);
    }

    static Polarity polarity(int index) {

        return Polarity.values()[Mth.clamp(index, 0, Polarity.values().length - 1)];
    }

    static RedstoneMode mode(int index) {

        return RedstoneMode.values()[Mth.clamp(index, 0, RedstoneMode.values().length - 1)];
    }

    static int level(int index) {

        return Mth.clamp(index, 0, HillCoreBlock.MAX_LEVEL);
    }

    static MutableComponent format(ValueSettings settings) {

        final int choice = choice(settings);
        return switch (settings.row()) {
            case POLARITY -> name("polarity", polarity(choice).name());
            case MODE -> name("mode", mode(choice).name());
            default -> Component.literal(String.valueOf(level(choice)));
        };
    }

    private static MutableComponent name(String group, String option) {

        return Component.translatable("hillsphere.core." + group + "." + option.toLowerCase(Locale.ROOT));
    }
}
