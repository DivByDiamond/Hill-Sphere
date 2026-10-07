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

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;
import dev.loki.hillsphere.field.Polarity;

/** A choice in the polarity setting of a core. Icons are borrowed from Create's own set. */
public enum PolarityOption implements INamedIconOptions {
    ATTRACT(Polarity.ATTRACT, AllIcons.I_TARGET),
    REPEL(Polarity.REPEL, AllIcons.I_ROTATE_CCW),
    LEVITATE(Polarity.LEVITATE, AllIcons.I_ACTIVE);

    private final Polarity polarity;
    private final AllIcons icon;

    PolarityOption(Polarity polarity, AllIcons icon) {

        this.polarity = polarity;
        this.icon = icon;
    }

    public Polarity polarity() {

        return polarity;
    }

    @Override
    public AllIcons getIcon() {

        return icon;
    }

    @Override
    public String getTranslationKey() {

        return "hillsphere.core.polarity." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
