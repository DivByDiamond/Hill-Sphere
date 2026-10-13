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
package dev.loki.hillsphere.client.goggles.shell;

import dev.loki.hillsphere.client.goggles.GogglesState;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.phys.Vec3;

/**
 * Cores arrive once a tick but are drawn every frame, so a field that grows by a block a tick would jump in
 * steps. This remembers where each core was a tick ago and blends toward where it is now by the part of the
 * tick that has passed.
 */
final class ShellEase {

    /** A core seen again within this many blocks of where it was is the same core. */
    private static final double SAME = 4;

    private record Seen(Vec3d centre, double radius) {
    }

    private final Map<CoreField, Seen> before = new IdentityHashMap<>();
    private List<CoreField> last = List.of();

    /** Call once a tick with the cores now in sight. */
    void tick(List<CoreField> now) {

        before.clear();
        for (final CoreField core : now) {
            CoreField match = null;
            double best = SAME;
            for (final CoreField old : last) {
                final double d = old.center().sub(core.center()).length();
                if (old.polarity() == core.polarity() && d < best) {
                    best = d;
                    match = old;
                }
            }
            if (match != null) {
                before.put(core, new Seen(match.center(), match.radius()));
            }
        }
        last = now;
    }

    double radius(CoreField core, float partialTick) {

        final Seen seen = before.get(core);
        return seen == null ? core.radius() : seen.radius() + (core.radius() - seen.radius()) * partialTick;
    }

    Vec3 centre(CoreField core, float partialTick) {

        final Vec3 now = GogglesState.of(core.center());
        final Seen seen = before.get(core);
        if (seen == null) {
            return now;
        }
        final Vec3 was = GogglesState.of(seen.centre());
        return was.add(now.subtract(was).scale(partialTick));
    }
}
