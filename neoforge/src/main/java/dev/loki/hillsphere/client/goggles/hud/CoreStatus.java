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
import dev.loki.hillsphere.blockentity.setting.CoreSettings;
import dev.loki.hillsphere.config.HillSphereConfig;
import dev.loki.hillsphere.field.FieldTuning;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.control.Control;
import dev.loki.hillsphere.field.control.RedstoneMode;

/**
 * Everything the panel shows about one core, read on the client. What the panel and redstone ask of the core
 * is worked out here, because only the server ticks it.
 */
record CoreStatus(String stateKey, boolean working, Polarity polarity, RedstoneMode mode, int panelLevel, Control asked,
        double rpm, double radius, double stress, FieldTuning tuning) {

    static CoreStatus of(HillCoreBlockEntity core) {

        final FieldTuning tuning = HillSphereConfig.tuning();
        final CoreSettings settings = core.settings();
        final double rpm = Math.abs(core.getSpeed());
        final Control asked = settings.mode().apply(settings.polarity(), settings.level(),
                core.getLevel().getBestNeighborSignal(core.getBlockPos()));
        final String state = state(core, tuning, rpm, asked);
        return new CoreStatus(state, "hillsphere.readout.working".equals(state), asked.polarity(), settings.mode(),
                settings.level(), asked, rpm, tuning.radius(rpm), tuning.stress(rpm, asked.level()), tuning);
    }

    private static String state(HillCoreBlockEntity core, FieldTuning tuning, double rpm, Control asked) {

        if (asked.level() <= 0) {
            return core.settings().level() == 0 ? "hillsphere.readout.zero" : "hillsphere.readout.redstone";
        }
        if (core.isOverStressed()) {
            return "hillsphere.readout.overstressed";
        }
        return tuning.isSpinningFastEnough(rpm) ? "hillsphere.readout.working" : "hillsphere.readout.slow";
    }

    /** Stress at the top speed and the highest level, the far end of the load bar. */
    double maxStress() {

        return tuning.stress(tuning.maxRpm(), tuning.levels());
    }
}
