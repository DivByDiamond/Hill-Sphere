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

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.loki.hillsphere.client.goggles.GogglesState;
import dev.loki.hillsphere.client.goggles.Palette;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.Gravity;
import dev.loki.hillsphere.field.view.FieldProbe;
import dev.loki.hillsphere.world.ClientFields;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A small shaded 3D arrow in the lower right corner that points where "down" is as seen from the eyes, with
 * the pull in g under it. The direction is turned by the camera's own rotation, so it stays right when the
 * view is tilted on a wall or a ceiling. With (almost) no pull it shows a ring and "0 g" instead.
 */
final class DownCompass {

    private static final float SIZE = 18;
    private static final int MARGIN = 34;
    private static final double WEIGHTLESS = 0.05;
    private static final Vector3f LIGHT = new Vector3f(-0.4f, -0.6f, 0.7f).normalize();
    /** The arrow along +y: shaft corners, then the head's base corners and its tip. */
    private static final float SHAFT = 0.09f;
    private static final float HEAD = 0.26f;

    private DownCompass() {
    }

    static void render(GuiGraphics graphics, GogglesState state) {

        final Minecraft mc = Minecraft.getInstance();
        final Gravity gravity = ClientFields.field().gravityAt(state.eye());
        final FieldProbe probe = state.probe(state.eye());
        final int[] ramp = probe.inField() ? Palette.of(probe.polarity()) : Palette.OFF;
        final float cx = graphics.guiWidth() - MARGIN;
        final float cy = graphics.guiHeight() - MARGIN - 10;
        final Matrix4f pose = graphics.pose().last().pose();
        final VertexConsumer quads = graphics.bufferSource().getBuffer(RenderType.gui());
        disc(quads, pose, cx, cy, Palette.argb(Palette.METAL[0], 0.6), Palette.argb(ramp[Palette.DARK], 0.9));
        final double g = gravity.strength();
        if (Math.abs(g) > WEIGHTLESS) {
            final Vec3d d = gravity.direction();
            final Vector3f view = new Vector3f((float) d.x(), (float) d.y(), (float) d.z())
                    .rotate(new Quaternionf(mc.gameRenderer.getMainCamera().rotation()).conjugate())
                    .mul(1, -1, 1).mul((float) Math.signum(g));
            final float length = (float) (0.55 + 0.45 * Math.min(1, Math.abs(g) / 2));
            arrow(quads, pose, cx, cy, new Quaternionf().rotationTo(new Vector3f(0, 1, 0), view.normalize()), length, ramp);
        }
        graphics.flush();
        final String text = String.format(Locale.ROOT, "%.2f g", Math.abs(g) > WEIGHTLESS ? g : 0);
        graphics.drawCenteredString(mc.font, text, (int) cx, (int) (cy + SIZE + 6), 0xFF000000 | ramp[Palette.LIGHT]);
    }

    private static void disc(VertexConsumer quads, Matrix4f pose, float cx, float cy, int fill, int rim) {

        final int segments = 24;
        for (int i = 0; i < segments; i++) {
            final double a = 2 * Math.PI * i / segments;
            final double b = 2 * Math.PI * (i + 1) / segments;
            final float r = SIZE + 3;
            vertex(quads, pose, cx, cy, 0, fill);
            vertex(quads, pose, cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r, 0, fill);
            vertex(quads, pose, cx + (float) Math.cos(b) * r, cy + (float) Math.sin(b) * r, 0, fill);
            vertex(quads, pose, cx, cy, 0, fill);
            vertex(quads, pose, cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r, 0, rim);
            vertex(quads, pose, cx + (float) Math.cos(a) * (r + 1), cy + (float) Math.sin(a) * (r + 1), 0, rim);
            vertex(quads, pose, cx + (float) Math.cos(b) * (r + 1), cy + (float) Math.sin(b) * (r + 1), 0, rim);
            vertex(quads, pose, cx + (float) Math.cos(b) * r, cy + (float) Math.sin(b) * r, 0, rim);
        }
    }

    /** A square shaft and a four-sided head, flat shaded; {@code length} 0..1 of the widget's size. */
    private static void arrow(VertexConsumer quads, Matrix4f pose, float cx, float cy, Quaternionf turn, float length, int[] ramp) {

        final float tail = -0.6f * length;
        final float neck = 0.35f * length;
        final float tip = 0.9f * length;
        for (int side = 0; side < 4; side++) {
            final Vector3f a = corner(side, SHAFT, tail);
            final Vector3f b = corner(side + 1, SHAFT, tail);
            face(quads, pose, cx, cy, turn, ramp, a, b, corner(side + 1, SHAFT, neck), corner(side, SHAFT, neck));
            final Vector3f top = new Vector3f(0, tip, 0);
            face(quads, pose, cx, cy, turn, ramp, corner(side, HEAD, neck), corner(side + 1, HEAD, neck), top, top);
        }
        face(quads, pose, cx, cy, turn, ramp, corner(0, HEAD, neck), corner(3, HEAD, neck), corner(2, HEAD, neck), corner(1, HEAD, neck));
        face(quads, pose, cx, cy, turn, ramp, corner(0, SHAFT, tail), corner(3, SHAFT, tail), corner(2, SHAFT, tail), corner(1, SHAFT, tail));
    }

    private static Vector3f corner(int index, float half, float y) {

        final double a = Math.PI / 2 * index + Math.PI / 4;
        return new Vector3f((float) Math.cos(a) * half * 1.414f, y, (float) Math.sin(a) * half * 1.414f);
    }

    private static void face(VertexConsumer quads, Matrix4f pose, float cx, float cy, Quaternionf turn, int[] ramp, Vector3f... corners) {

        final Vector3f[] p = new Vector3f[corners.length];
        for (int i = 0; i < corners.length; i++) {
            p[i] = new Vector3f(corners[i]).rotate(turn).mul(SIZE);
        }
        final Vector3f normal = new Vector3f(p[1]).sub(p[0]).cross(new Vector3f(p[2]).sub(p[0]));
        if (normal.lengthSquared() < 1e-6) {
            normal.set(new Vector3f(p[3]).sub(p[0]).cross(new Vector3f(p[2]).sub(p[0])));
        }
        final double light = Math.abs(normal.normalize().dot(LIGHT));
        final int colour = 0xFF000000 | Palette.mix(ramp[Palette.DARK], ramp[Palette.LIGHT], light);
        for (final Vector3f v : p) {
            vertex(quads, pose, cx + v.x, cy + v.y, SIZE + v.z, colour);
        }
    }

    private static void vertex(VertexConsumer quads, Matrix4f pose, float x, float y, float z, int colour) {

        quads.addVertex(pose, x, y, z).setColor(colour);
    }
}
