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
package dev.loki.hillsphere.client.pose;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.field.math.Vec3d;

import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

/**
 * Turns a living thing's model with its frame, easing from the old way to the new one the way a compass
 * needle settles. The renderer opens and closes the transform for every body it draws; while the frame
 * is vanilla this stays out of the stack entirely, so the stack cannot tip.
 */
public final class FramePose {

    /**
     * How long a model takes to settle onto a new "down", in milliseconds: the same half second the
     * camera eases a quarter turn in, so body and view turn as one rigid thing.
     */
    private static final float EASE_MS = 500.0F;

    /** A turn being played out for one entity: where it came from, where it is going, and since when. */
    private record Turn(long since, Quaternionf from, Quaternionf to) {

        Quaternionf at(long now) {

            final float t = Math.max(0.0F, Math.min(1.0F, (now - since) / EASE_MS));
            if (t >= 1.0F) {
                return new Quaternionf(to);
            }
            return new Quaternionf(from).slerp(to, t);
        }
    }

    private static final Map<LivingEntity, Turn> TURNS = new WeakHashMap<>();

    private FramePose() {
    }

    /** Opens the turn: a push and the eased rotation about the body's place in the world. */
    public static void push(PoseStack stack, LivingEntity entity, float partialTick) {

        final GravityFrame frame = LivingGravity.frameOf(entity);
        if (frame.isVanilla()) {
            TURNS.remove(entity);
            return;
        }
        final Quaternionf wanted = turnOf(frame);
        final long now = entity.level().getGameTime() * 50L + Math.round(partialTick * 50.0F);
        Turn turn = TURNS.get(entity);
        if (turn == null || !turn.to().equals(wanted)) {
            Quaternionf from = turn == null ? new Quaternionf() : turn.at(now);
            if (from.dot(wanted) > 0.9999F) {
                from = new Quaternionf(wanted);
            }
            turn = new Turn(now, from, wanted);
            TURNS.put(entity, turn);
        }
        stack.pushPose();
        stack.mulPose(turn.at(now));
    }

    /** Closes exactly what {@link #push} opened. */
    public static void pop(PoseStack stack, LivingEntity entity) {

        if (LivingGravity.frameOf(entity).isVanilla()) {
            return;
        }
        stack.popPose();
    }

    /** The quarter turn that carries the world's axes to where the body's axes lie. */
    private static Quaternionf turnOf(GravityFrame frame) {

        final Vec3d[] axes = frame.axes();
        final Matrix3f turn = new Matrix3f(
                (float) axes[0].x(), (float) axes[0].y(), (float) axes[0].z(),
                (float) axes[1].x(), (float) axes[1].y(), (float) axes[1].z(),
                (float) axes[2].x(), (float) axes[2].y(), (float) axes[2].z());
        return new Quaternionf().setFromNormalized(turn);
    }
}
