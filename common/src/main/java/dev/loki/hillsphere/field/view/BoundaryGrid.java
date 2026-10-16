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
package dev.loki.hillsphere.field.view;

import java.util.ArrayList;
import java.util.List;

/** Marching squares over the part of the half-plane both fields reach; traces a {@link BoundaryProfile}. */
final class BoundaryGrid {

    private static final int BISECT_STEPS = 20;

    private final BoundaryProfile.Pair pair;
    private final int rows;
    private final int cols;
    private final double from;
    private final double alongStep;
    private final double radialStep;
    private final double[][] values;

    BoundaryGrid(BoundaryProfile.Pair pair, int rows) {

        this.pair = pair;
        this.rows = rows;
        from = Math.max(-pair.a().radius(), pair.gap() - pair.b().radius());
        final double length = Math.min(pair.a().radius(), pair.gap() + pair.b().radius()) - from;
        radialStep = Math.min(pair.a().radius(), pair.b().radius()) / rows;
        cols = Math.max(1, (int) Math.ceil(length / radialStep));
        alongStep = length / cols;
        values = new double[cols + 1][rows + 1];
        for (int i = 0; i <= cols; i++) {
            for (int j = 0; j <= rows; j++) {
                values[i][j] = pair.value(from + i * alongStep, j * radialStep);
            }
        }
    }

    BoundaryProfile.Profile trace() {

        final List<BoundaryProfile.Segment> segments = new ArrayList<>();
        final List<BoundaryProfile.Point> rings = new ArrayList<>();
        for (int j = 0; j <= rows; j++) {
            for (int i = 0; i < cols; i++) {
                addIfPresent(rings, crossing(i, j, i + 1, j));
                if (j < rows) {
                    cell(i, j, segments);
                }
            }
        }
        return new BoundaryProfile.Profile(List.copyOf(segments), List.copyOf(rings));
    }

    /** The seam inside one cell: its crossings on the bottom, right, top and left edges, joined in pairs. */
    private void cell(int i, int j, List<BoundaryProfile.Segment> segments) {

        final List<BoundaryProfile.Point> found = new ArrayList<>(4);
        addIfPresent(found, crossing(i, j, i + 1, j));
        addIfPresent(found, crossing(i + 1, j, i + 1, j + 1));
        addIfPresent(found, crossing(i, j + 1, i + 1, j + 1));
        addIfPresent(found, crossing(i, j, i, j + 1));
        for (int k = 0; k + 1 < found.size() && found.size() % 2 == 0; k += 2) {
            segments.add(new BoundaryProfile.Segment(found.get(k), found.get(k + 1)));
        }
    }

    /** Where the seam crosses the edge between two grid nodes, or null if it does not or nothing balances there. */
    private BoundaryProfile.Point crossing(int i0, int j0, int i1, int j1) {

        final double v0 = values[i0][j0];
        final double v1 = values[i1][j1];
        if (v0 > 0 == v1 > 0) {
            return null;
        }
        double low = 0;
        double high = 1;
        for (int k = 0; k < BISECT_STEPS; k++) {
            final double mid = (low + high) / 2;
            if (value(i0, j0, i1, j1, mid) > 0 == v0 > 0) {
                low = mid;
            } else {
                high = mid;
            }
        }
        final double t = (low + high) / 2;
        return pair.point(along(i0, i1, t), radial(j0, j1, t));
    }

    private double value(int i0, int j0, int i1, int j1, double t) {

        return pair.value(along(i0, i1, t), radial(j0, j1, t));
    }

    private double along(int i0, int i1, double t) {

        return from + (i0 + (i1 - i0) * t) * alongStep;
    }

    private double radial(int j0, int j1, double t) {

        return (j0 + (j1 - j0) * t) * radialStep;
    }

    private static void addIfPresent(List<BoundaryProfile.Point> points, BoundaryProfile.Point point) {

        if (point != null) {
            points.add(point);
        }
    }
}
