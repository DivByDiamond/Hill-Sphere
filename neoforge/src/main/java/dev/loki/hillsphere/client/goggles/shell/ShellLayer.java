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
import dev.loki.hillsphere.client.goggles.Palette;
import dev.loki.hillsphere.client.goggles.render.Brush;
import dev.loki.hillsphere.client.goggles.render.WorldLayer;
import dev.loki.hillsphere.client.goggles.shell.look.ShellStyle;
import dev.loki.hillsphere.client.goggles.shell.look.ShellStyle.Fx;
import dev.loki.hillsphere.config.GogglesConfig;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.resolve.CoreField;
import dev.loki.hillsphere.field.view.BoundaryMesh;
import dev.loki.hillsphere.field.view.effect.ShellEffects;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/**
 * Where the nearest fields end: a sparse latitude-longitude shell at the field's radius, a denser and fainter
 * one at the edge of the full-strength plateau, both turning slowly. Where two pulling fields overlap, the
 * seam on which their pulls balance (where the resolver splits "down" between them) is drawn as rings and ribs: pale
 * for two attracting or two repelling fields, {@link #BALANCE} for an attracting next to a repelling one. Lines
 * fade where little happens on the seam (see {@link BoundaryMesh}).
 */
public final class ShellLayer implements WorldLayer {

    private static final double VIEW = 72;
    private static final int SEGMENTS = 48;
    /** Seam of an attracting and a repelling field: halfway between their colours. */
    private static final int BALANCE = Palette.mix(Palette.ATTRACT[Palette.LIGHT], Palette.REPEL[Palette.LIGHT], 0.5);

    private final ShellEase ease = new ShellEase();
    private List<CoreField> shells = List.of();
    private List<Boundary> boundaries = List.of();

    /** The seam of two fields: its lines, measured for effects from {@code start} along {@code axis}; {@code mixed} for unlike signs. */
    private record Boundary(Vec3 start, Vec3 axis, List<BoundaryMesh.Line> lines, boolean mixed) {
    }

    @Override
    public void tick(GogglesState state) {

        final List<CoreField> spheres = state.near().stream().filter(core -> core.deck() == null).toList();
        shells = spheres.subList(0, Math.min(spheres.size(), GogglesConfig.MAX_SHELLS.get()));
        ease.tick(shells);
        final int rings = ShellStyle.rings();
        final int ribs = ShellStyle.read().look().ribs();
        final List<Boundary> found = new ArrayList<>();
        for (int i = 0; i < shells.size(); i++) {
            for (int j = i + 1; j < shells.size(); j++) {
                final CoreField a = shells.get(i);
                final CoreField b = shells.get(j);
                if (a.polarity() == Polarity.LEVITATE || b.polarity() == Polarity.LEVITATE) {
                    continue;
                }
                final List<BoundaryMesh.Line> lines = BoundaryMesh.of(state.tuning(), a, b, state.near(), rings, ribs, SEGMENTS);
                if (!lines.isEmpty()) {
                    final Vec3 start = GogglesState.of(a.center());
                    found.add(new Boundary(start, GogglesState.of(b.center()).subtract(start).normalize(), lines,
                            a.polarity() != b.polarity()));
                }
            }
        }
        boundaries = found;
    }

    @Override
    public void render(Brush brush, GogglesState state, float partialTick) {

        final ShellStyle style = ShellStyle.read();
        final Fx fx = style.fx();
        final double time = state.time(partialTick);
        final double spin = time * style.look().spin();
        final double opacity = GogglesConfig.OPACITY.get() * (fx.pulse() ? ShellEffects.pulse(time, fx.pulseDepth(), fx.pulsePeriod()) : 1);
        final ShellLines lines = new ShellLines(brush, VIEW);
        for (final CoreField core : shells) {
            final int[] ramp = Palette.of(core.polarity());
            final Vec3 centre = ease.centre(core, partialTick);
            final double radius = ease.radius(core, partialTick);
            final double gain = opacity * coreGain(fx, core, ease.age(core, partialTick), time, brush.camera().distanceTo(centre) / radius);
            lines.gain(fx.wave() ? point -> wave(fx, time, point.y - centre.y) : ShellLines.Gain.NONE);
            lines.sphere(centre, radius, style.look().edgeMeridians(), style.look().edgeParallels(), spin,
                    Palette.argb(pick(style.edgeColour(), ramp[Palette.MAIN]), style.look().edgeAlpha() * gain));
            if (style.look().showPlateau()) {
                lines.sphere(centre, radius * state.tuning().plateau(), style.look().plateauMeridians(),
                        style.look().plateauParallels(), -spin,
                        Palette.argb(pick(style.plateauColour(), ramp[Palette.DARK]), style.look().plateauAlpha() * gain));
            }
        }
        for (final Boundary boundary : boundaries) {
            lines.gain(fx.wave() ? point -> wave(fx, time, point.subtract(boundary.start()).dot(boundary.axis())) : ShellLines.Gain.NONE);
            drawBoundary(lines, boundary, Palette.argb(pick(style.boundaryColour(), boundary.mixed() ? BALANCE : Palette.PALE),
                    style.look().boundaryAlpha() * opacity));
        }
    }

    /** Entry fade and edge flicker of one field. */
    private static double coreGain(Fx fx, CoreField core, double age, double time, double edgeRatio) {

        final double entry = fx.entry() ? ShellEffects.entry(age, fx.entryTicks()) : 1;
        final double seed = core.center().x() * 13 + core.center().z() * 7;
        return entry * (fx.flicker() ? ShellEffects.flicker(time, edgeRatio, fx.flickerDepth(), seed) : 1);
    }

    private static double wave(Fx fx, double time, double along) {

        return ShellEffects.wave(time, along, fx.waveDepth(), fx.waveLength(), fx.waveSpeed());
    }

    private static int pick(int custom, int bySign) {

        return custom == ShellStyle.BY_SIGN ? bySign : custom;
    }

    /** Each end of a line as bright as its weight on the seam; the square root keeps faint parts visible. */
    private static void drawBoundary(ShellLines lines, Boundary boundary, int colour) {

        final double alpha = (colour >>> 24) / 255.0;
        for (final BoundaryMesh.Line line : boundary.lines()) {
            if (line.fromWeight() > 0 || line.toWeight() > 0) {
                lines.line(GogglesState.of(line.from()), GogglesState.of(line.to()),
                        Palette.argb(colour, alpha * Math.sqrt(line.fromWeight())), Palette.argb(colour, alpha * Math.sqrt(line.toWeight())));
            }
        }
    }
}
