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
package dev.loki.hillsphere.client.particle;

import dev.loki.hillsphere.debug.Debug;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.particle.ParticlePull;
import dev.loki.hillsphere.field.particle.RainTilt;
import dev.loki.hillsphere.field.resolve.Gravity;
import dev.loki.hillsphere.world.ClientFields;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

/**
 * Answers "how should this particle bend" cheaply: the field is read once per 2x2x2 cells and the cells
 * are forgotten every few ticks, particles without a field, or far from the player, leave at once.
 */
public final class ParticleSway {

    private static final int REFRESH_TICKS = 4;
    private static final int MAX_CELLS = 4096;
    private static final Map<Long, Gravity> CELLS = new HashMap<>();

    private static long bucket = Long.MIN_VALUE;
    private static boolean enabled;
    private static double strength;
    private static double range;
    private static double rain;
    private static int seen;
    private static int pushed;

    private ParticleSway() {
    }

    /** The extra speed for a particle that falls with the given gravity, or null to leave it alone. */
    public static Vec3d fall(double x, double y, double z, double particleGravity) {

        final Gravity gravity = gravityAt(x, y, z);
        return gravity == null ? null : ParticlePull.fall(gravity, particleGravity, mix(x, y, z));
    }

    /** The launch speed turned toward the field's "up", or null to leave it alone. */
    public static Vec3d lean(double x, double y, double z, Vec3d velocity) {

        final Gravity gravity = gravityAt(x, y, z);
        return gravity == null ? null : ParticlePull.lean(velocity, gravity, mix(x, y, z));
    }

    /** The lean of the rain strip at the point, as (x, z) per block of height, or null to draw it vertical. */
    public static Vec3d rainSlope(double x, double y, double z) {

        final Gravity gravity = gravityAt(x, y, z);
        final double share = rainShare(x, y, z);
        return gravity == null || share <= 0 ? null : RainTilt.slope(gravity, share);
    }

    private static double rainShare(double x, double y, double z) {

        final LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? 0 : rain * ParticlePull.fade(Math.sqrt(player.distanceToSqr(x, y, z)), range);
    }

    private static double mix(double x, double y, double z) {

        final LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? 0 : strength * ParticlePull.fade(Math.sqrt(player.distanceToSqr(x, y, z)), range);
    }

    /** Debug only: once a second, how many falling particles were seen and how many were pushed, and what the client knows. */
    public static void trace(Vec3d push) {

        seen++;
        pushed += push == null ? 0 : 1;
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getGameTime() % 40 == 0 && seen > 0) {
            Debug.log("[particles] seen={} pushed={} cores={} enabled={} strength={} range={}", seen, pushed,
                    ClientFields.field().size(), enabled, strength, range);
            seen = 0;
            pushed = 0;
        }
    }

    private static Gravity gravityAt(double x, double y, double z) {

        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || ClientFields.field().size() == 0) {
            return null;
        }
        refresh(mc.level.getGameTime() / REFRESH_TICKS);
        if (!enabled || mc.player.distanceToSqr(x, y, z) >= range * range) {
            return null;
        }
        final long cell = BlockPos.asLong((int) Math.floor(x) >> 1, (int) Math.floor(y) >> 1, (int) Math.floor(z) >> 1);
        if (CELLS.size() > MAX_CELLS) {
            CELLS.clear();
        }
        final Gravity gravity = CELLS.computeIfAbsent(cell, k -> sample(x, y, z));
        return gravity == Gravity.VANILLA ? null : gravity;
    }

    private static Gravity sample(double x, double y, double z) {

        final double cx = (Math.floor(x / 2) + 0.5) * 2;
        final double cy = (Math.floor(y / 2) + 0.5) * 2;
        final double cz = (Math.floor(z / 2) + 0.5) * 2;
        final Gravity gravity = ClientFields.field().gravityAt(new Vec3d(cx, cy, cz));
        return ParticlePull.isVanilla(gravity) ? Gravity.VANILLA : gravity;
    }

    private static void refresh(long now) {

        if (now != bucket) {
            bucket = now;
            CELLS.clear();
            enabled = ParticleConfig.ENABLED.get();
            strength = ParticleConfig.STRENGTH.get();
            range = ParticleConfig.RANGE.get();
            rain = ParticleConfig.RAIN.get();
        }
    }
}
