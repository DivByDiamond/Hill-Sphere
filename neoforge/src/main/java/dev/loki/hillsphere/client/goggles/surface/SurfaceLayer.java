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
package dev.loki.hillsphere.client.goggles.surface;

import dev.loki.hillsphere.client.goggles.GogglesState;
import dev.loki.hillsphere.client.goggles.Palette;
import dev.loki.hillsphere.client.goggles.render.Brush;
import dev.loki.hillsphere.client.goggles.render.WorldLayer;
import dev.loki.hillsphere.config.GogglesConfig;
import dev.loki.hillsphere.entity.LivingGravity;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Soft fills on the faces one can stand on: in the field's colour where the wearer can step on without
 * turning, pale where the field would turn them round (fainter next to the seam of two cores, so it does not pile up
 * on the seam's rings). A slow wave runs out from the wearer. The scan runs
 * every few ticks; drawing only reads it.
 */
public final class SurfaceLayer implements WorldLayer {

    private static final double LIFT = 0.006;
    private static final double OUTER = 0.47;
    private static final double INNER = 0.3;
    private static final double ALPHA = 0.22;
    private static final double TURN_ALPHA = 0.3;
    /** How much of a pale fill fades out right at the seam of two cores, where the goggles draw the seam itself. */
    private static final double SEAM_DIM = 0.85;

    private List<SurfaceScanner.Face> faces = List.of();
    private long scannedAt = -Integer.MAX_VALUE;

    @Override
    public void tick(GogglesState state) {

        final Minecraft mc = Minecraft.getInstance();
        if (state.near().isEmpty() || mc.level == null || mc.player == null) {
            faces = List.of();
            return;
        }
        if (state.ticks() < scannedAt || state.ticks() - scannedAt >= GogglesConfig.SCAN_TICKS.get()) {
            scannedAt = state.ticks();
            faces = SurfaceScanner.scan(mc.level, state, LivingGravity.frameOf(mc.player), GogglesConfig.RANGE.get(),
                    GogglesConfig.MAX_FACES.get());
        }
    }

    @Override
    public void render(Brush brush, GogglesState state, float partialTick) {

        final double time = state.time(partialTick);
        final double range = GogglesConfig.RANGE.get();
        final double opacity = GogglesConfig.OPACITY.get();
        for (final SurfaceScanner.Face face : faces) {
            if (!brush.hasBudget()) {
                return;
            }
            final double wave = 0.7 + 0.3 * Math.sin(time * 0.12 - face.distance() * 0.5);
            final double kind = face.turns() ? TURN_ALPHA * (1 - SEAM_DIM * face.seam()) : ALPHA;
            final double alpha = opacity * wave * Brush.fadeWithDistance(face.distance(), range) * kind;
            final int rgb = face.turns() ? Palette.PALE : Palette.of(face.polarity())[Palette.MAIN];
            final Vec3 centre = face.centre().add(face.normal().scale(LIFT));
            square(brush, centre, face, OUTER, Palette.argb(rgb, alpha * 0.5));
            square(brush, centre, face, INNER, Palette.argb(rgb, alpha));
        }
    }

    private static void square(Brush brush, Vec3 centre, SurfaceScanner.Face face, double half, int colour) {

        final Vec3 u = face.u().scale(half);
        final Vec3 v = face.v().scale(half);
        brush.quad(centre.add(u).add(v), centre.add(u).subtract(v), centre.subtract(u).subtract(v), centre.subtract(u).add(v), colour);
    }
}
