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
package dev.loki.hillsphere.client.goggles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.Gravity;
import dev.loki.hillsphere.item.HillGogglesItem;
import dev.loki.hillsphere.world.ClientFields;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * What the goggles show: the block faces you can stand on in a field, and short arrows around the wearer
 * that point where "down" is. Everything is drawn on the client only.
 */
public final class GogglesRenderer {

    private static final SurfaceScanner SCANNER = new SurfaceScanner();
    private static final int ARROW_GRID = 2;
    private static final double ARROW_SPACING = 2.5;

    private static final float[] ATTRACT = {0.25f, 0.85f, 0.9f};
    private static final float[] LEVITATE = {0.54f, 0.42f, 1f};

    private GogglesRenderer() {
    }

    public static void onRender(RenderLevelStageEvent event) {

        final Minecraft mc = Minecraft.getInstance();
        final LocalPlayer player = mc.player;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || player == null
                || !(player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof HillGogglesItem)) {
            return;
        }
        final Vec3 camera = event.getCamera().getPosition();
        final PoseStack stack = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        final VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        stack.pushPose();
        stack.translate(-camera.x, -camera.y, -camera.z);
        final PoseStack.Pose pose = stack.last();
        drawSurfaces(lines, pose, mc.level, player.blockPosition());
        drawArrows(lines, pose, player.position().add(0, player.getBbHeight() / 2, 0));
        stack.popPose();
        buffers.endBatch(RenderType.lines());
    }

    /** Highlights every face you could stand on in a field, with the field's own idea of "up". */
    private static void drawSurfaces(VertexConsumer lines, PoseStack.Pose pose, ClientLevel level, BlockPos origin) {

        final LineDrawer outer = new LineDrawer(lines, pose, ATTRACT, 0.9f);
        final LineDrawer inner = new LineDrawer(lines, pose, ATTRACT, 0.35f);
        for (final SurfaceScanner.Surface surface : SCANNER.around(level, origin)) {
            outer.square(surface.center(), surface.face(), 0.5);
            inner.square(surface.center(), surface.face(), 0.3);
        }
    }

    /** A grid of arrows around the wearer; each shows the gravity the field gives at its own point. */
    private static void drawArrows(VertexConsumer lines, PoseStack.Pose pose, Vec3 around) {

        for (int x = -ARROW_GRID; x <= ARROW_GRID; x++) {
            for (int y = -ARROW_GRID; y <= ARROW_GRID; y++) {
                for (int z = -ARROW_GRID; z <= ARROW_GRID; z++) {
                    final Vec3 at = around.add(x * ARROW_SPACING, y * ARROW_SPACING, z * ARROW_SPACING);
                    arrowAt(lines, pose, at);
                }
            }
        }
    }

    private static void arrowAt(VertexConsumer lines, PoseStack.Pose pose, Vec3 at) {

        final Vec3d point = new Vec3d(at.x, at.y, at.z);
        final Gravity g = ClientFields.field().gravityAt(point);
        final double lift = ClientFields.field().liftAt(point);
        if (lift != 0) {
            final double along = -(1 - lift);
            new LineDrawer(lines, pose, LEVITATE, 0.8f).arrow(at, new Vec3(0, along * 1.2, 0));
        } else if (g.strength() != 1 || g.direction().y() > -0.999) {
            final Vec3d d = g.direction();
            new LineDrawer(lines, pose, ATTRACT, 0.8f).arrow(at, new Vec3(d.x(), d.y(), d.z()).scale(Math.min(1.5, g.strength())));
        }
    }
}
