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
import dev.loki.hillsphere.config.GogglesConfig;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.resolve.CoreField;
import dev.loki.hillsphere.field.view.BoundaryProfile;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/**
 * Where the nearest fields end: a sparse latitude-longitude shell at the field's radius, a denser and fainter
 * one at the edge of the full-strength plateau, both turning slowly. Where two pulling fields overlap, the
 * surface on which their pulls balance is drawn as pale rings and ribs.
 */
public final class ShellLayer implements WorldLayer {

    private static final int SEGMENTS = 48;
    private static final int RINGS = 8;
    private static final int RIBS = 10;
    private static final double SPIN = 0.002;
    private static final double VIEW = 72;

    private List<CoreField> shells = List.of();
    private List<Boundary> boundaries = List.of();

    /** The balance surface of two fields: profile measured from {@code start} along {@code axis}. */
    private record Boundary(Vec3 start, Vec3 axis, List<BoundaryProfile.Point> profile) {
    }

    @Override
    public void tick(GogglesState state) {

        final List<CoreField> near = state.near();
        shells = near.subList(0, Math.min(near.size(), GogglesConfig.MAX_SHELLS.get()));
        final List<Boundary> found = new ArrayList<>();
        for (int i = 0; i < shells.size(); i++) {
            for (int j = i + 1; j < shells.size(); j++) {
                final CoreField a = shells.get(i);
                final CoreField b = shells.get(j);
                if (a.polarity() == Polarity.LEVITATE || b.polarity() == Polarity.LEVITATE) {
                    continue;
                }
                final List<BoundaryProfile.Point> profile = BoundaryProfile.between(state.tuning(), a, b, RINGS);
                if (!profile.isEmpty()) {
                    final Vec3 start = GogglesState.of(a.center());
                    found.add(new Boundary(start, GogglesState.of(b.center()).subtract(start).normalize(), profile));
                }
            }
        }
        boundaries = found;
    }

    @Override
    public void render(Brush brush, GogglesState state, float partialTick) {

        final double spin = state.time(partialTick) * SPIN;
        final double opacity = GogglesConfig.OPACITY.get();
        final Lines lines = new Lines(brush, VIEW);
        for (final CoreField core : shells) {
            final int[] ramp = Palette.of(core.polarity());
            final Vec3 centre = GogglesState.of(core.center());
            lines.sphere(centre, core.radius(), 12, 5, spin, Palette.argb(ramp[Palette.MAIN], 0.45 * opacity));
            lines.sphere(centre, core.radius() * state.tuning().plateau(), 20, 9, -spin, Palette.argb(ramp[Palette.DARK], 0.3 * opacity));
        }
        for (final Boundary boundary : boundaries) {
            drawBoundary(lines, boundary, Palette.argb(Palette.PALE, 0.6 * opacity));
        }
    }

    private static void drawBoundary(Lines lines, Boundary boundary, int colour) {

        final Vec3[] side = Lines.across(boundary.axis());
        final List<BoundaryProfile.Point> profile = boundary.profile();
        for (final BoundaryProfile.Point point : profile) {
            if (point.radial() > 0) {
                lines.circle(boundary.start().add(boundary.axis().scale(point.along())), side, point.radial(), 0, colour);
            }
        }
        for (int rib = 0; rib < RIBS; rib++) {
            final double angle = 2 * Math.PI * rib / RIBS;
            Vec3 from = null;
            for (final BoundaryProfile.Point point : profile) {
                final Vec3 to = boundary.start().add(boundary.axis().scale(point.along()))
                        .add(side[0].scale(Math.cos(angle) * point.radial())).add(side[1].scale(Math.sin(angle) * point.radial()));
                if (from != null) {
                    lines.line(from, to, colour);
                }
                from = to;
            }
        }
    }

    /** Lines that fade with distance from the camera. */
    private record Lines(Brush brush, double view) {

        /** Two unit vectors at right angles to the axis and each other. */
        static Vec3[] across(Vec3 axis) {

            final Vec3 helper = Math.abs(axis.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
            final Vec3 u = axis.cross(helper).normalize();
            return new Vec3[] {u, axis.cross(u).normalize()};
        }

        void sphere(Vec3 centre, double radius, int meridians, int parallels, double turn, int colour) {

            final Vec3[] flat = {new Vec3(1, 0, 0), new Vec3(0, 0, 1)};
            for (int k = 1; k <= parallels; k++) {
                final double theta = Math.PI * k / (parallels + 1);
                circle(centre.add(0, radius * Math.cos(theta), 0), flat, radius * Math.sin(theta), turn, colour);
            }
            for (int m = 0; m < meridians; m++) {
                final double phi = turn + 2 * Math.PI * m / meridians;
                final Vec3 out = new Vec3(Math.cos(phi), 0, Math.sin(phi));
                Vec3 from = centre.add(0, radius, 0);
                for (int s = 1; s <= SEGMENTS / 2; s++) {
                    final double theta = 2 * Math.PI * s / SEGMENTS;
                    final Vec3 to = centre.add(out.scale(radius * Math.sin(theta))).add(0, radius * Math.cos(theta), 0);
                    line(from, to, colour);
                    from = to;
                }
            }
        }

        void circle(Vec3 centre, Vec3[] plane, double radius, double turn, int colour) {

            Vec3 from = null;
            for (int s = 0; s <= SEGMENTS; s++) {
                final double a = turn + 2 * Math.PI * s / SEGMENTS;
                final Vec3 to = centre.add(plane[0].scale(radius * Math.cos(a))).add(plane[1].scale(radius * Math.sin(a)));
                if (from != null) {
                    line(from, to, colour);
                }
                from = to;
            }
        }

        void line(Vec3 from, Vec3 to, int colour) {

            brush.line(from, to, fade(from, colour), fade(to, colour));
        }

        private int fade(Vec3 point, int colour) {

            final double distance = point.distanceTo(brush.camera());
            final double k = (1 - Brush.smoothstep(view * 0.5, view, distance)) * Brush.smoothstep(0.5, 2.5, distance);
            return Palette.argb(colour, (colour >>> 24) / 255.0 * k);
        }
    }
}
