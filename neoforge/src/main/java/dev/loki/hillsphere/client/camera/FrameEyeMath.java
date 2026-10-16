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
package dev.loki.hillsphere.client.camera;

import dev.loki.hillsphere.compat.Ships;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.field.math.Vec3d;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

/** The geometry behind the turned camera: the frame's turn and an eye that stays in clear air. */
public final class FrameEyeMath {

    /** Eye pulls at least this far back from the feet, so the view never sits inside the body. */
    private static final double MIN_CLEARANCE = 0.25;

    /** Margin the clip gives around the camera's near plane. */
    private static final double MARGIN = 0.15;

    /** The ray starts this far off the feet: they rest exactly on the face of the block below, which would count as a hit. */
    private static final double START = 0.1;

    private FrameEyeMath() {
    }

    /** The quarter turn that carries the world's axes to where the body's axes lie. */
    public static Quaternionf turnOf(GravityFrame frame) {

        final Vec3d[] axes = frame.axes();
        final Matrix3f turn = new Matrix3f(
                (float) axes[0].x(), (float) axes[0].y(), (float) axes[0].z(),
                (float) axes[1].x(), (float) axes[1].y(), (float) axes[1].z(),
                (float) axes[2].x(), (float) axes[2].y(), (float) axes[2].z());
        return new Quaternionf().setFromNormalized(turn);
    }

    /** Walks the eye down the frame's "up" from the desired point to the last place a head fits. */
    public static Vec3 clearEye(BlockGetter level, Entity entity, Vec3 feet, Vec3 target) {

        final Vec3 delta = target.subtract(feet);
        final double dist = delta.length();
        if (dist < 1.0e-3) {
            return target;
        }
        final ClipContext clip = new ClipContext(feet.add(delta.scale(START / dist)), target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity);
        final BlockHitResult hit = level.clip(clip);
        if (hit.getType() == HitResult.Type.MISS) {
            return target;
        }
        final double clear = worldOf(level, hit.getLocation()).distanceTo(feet) - MARGIN;
        if (clear < MIN_CLEARANCE) {
            return feet;
        }
        return feet.add(delta.scale(clear / dist));
    }

    /** A hit on a ship's block lies among the ship's blocks, far off in the world; this is where it is in the world. */
    private static Vec3 worldOf(BlockGetter level, Vec3 hit) {

        if (!(level instanceof Level world)) {
            return hit;
        }
        final Vec3d at = Ships.toWorld(world, new Vec3d(hit.x, hit.y, hit.z));
        return new Vec3(at.x(), at.y(), at.z());
    }
}
