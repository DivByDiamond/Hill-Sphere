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
package dev.loki.hillsphere.rule;

import dev.loki.hillsphere.config.HillSphereConfig;
import dev.loki.hillsphere.debug.Debug;
import dev.loki.hillsphere.field.resolve.Gravity;
import dev.loki.hillsphere.world.FieldLookup;

import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/** With {@code noFallDamageInField} on, a fall that ends inside any core's field does no harm. */
public final class FallRule {

    private FallRule() {
    }

    public static void register(IEventBus bus) {

        bus.addListener(LivingFallEvent.class, FallRule::onFall);
    }

    private static void onFall(LivingFallEvent event) {

        final Entity entity = event.getEntity();
        if (!(entity instanceof net.minecraft.world.entity.player.Player) && !HillSphereConfig.safeFalls()) {
            return;
        }
        final boolean inField = !Gravity.VANILLA.equals(FieldLookup.planetAt(entity)) || FieldLookup.liftAt(entity) > 0;
        Debug.log("fall {} distance={} inField={} safeFalls={}", entity.getName().getString(), event.getDistance(), inField,
                HillSphereConfig.safeFalls());
        if (inField && HillSphereConfig.safeFalls()) {
            event.setDamageMultiplier(0);
        }
    }
}
