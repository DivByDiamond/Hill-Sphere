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
package dev.loki.hillsphere.client.screen;

import net.minecraft.client.gui.GuiGraphics;

/** Draws one of Create's grey input fields: lit along the top, dark top-left and white bottom-right edges, a notch on the left. */
final class InputField {

    private static final int FILL = 0xFF8B8B8B;
    private static final int LIT = 0xFFA3A3A3;
    private static final int SHADE = 0xFF373737;
    private static final int EDGE = 0xFFFFFFFF;

    private InputField() {
    }

    static void draw(GuiGraphics graphics, int x, int y, int w, int h) {

        graphics.fill(x, y, x + w, y + h, FILL);
        graphics.fill(x + 1, y + 2, x + w - 1, y + 4, LIT);
        graphics.fill(x, y, x + w, y + 1, SHADE);
        graphics.fill(x, y, x + 1, y + h, SHADE);
        graphics.fill(x, y + h - 1, x + w, y + h, EDGE);
        graphics.fill(x + w - 1, y, x + w, y + h, EDGE);
        final int mid = y + h / 2;
        for (int k = 1; k <= 3; k++) {
            graphics.fill(x - k, mid - 4 + k, x - k + 1, mid + 4 - k, FILL);
            graphics.fill(x - k, mid - 4 + k, x - k + 1, mid - 3 + k, SHADE);
            graphics.fill(x - k, mid + 3 - k, x - k + 1, mid + 4 - k, EDGE);
        }
    }
}
