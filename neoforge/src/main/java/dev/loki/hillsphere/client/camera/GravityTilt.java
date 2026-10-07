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

import com.playsi.aero_cam_sync.api.AcsHandle;
import com.playsi.aero_cam_sync.api.AeroCamSyncApi;
import com.playsi.aero_cam_sync.api.TiltContext;
import com.playsi.aero_cam_sync.api.TiltSource;
import dev.loki.hillsphere.Constants;
import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.math.Vec3d;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

/** Turns the local player's camera with the frame the field has put them in. */
public final class GravityTilt implements TiltSource {

    private static final int PRIORITY = 100;

    private GravityTilt() {
    }

    public static void register() {

        final AcsHandle acs = AeroCamSyncApi.forMod(Constants.MOD_ID);
        acs.addTiltSource(PRIORITY, new GravityTilt());
    }

    @Override
    public boolean appliesTo(TiltContext context) {

        final Player player = context.player();
        return player == Minecraft.getInstance().player && !LivingGravity.frameOf(player).isVanilla();
    }

    /** The turn that takes the world's up to where the player's head points. */
    @Override
    public Quaternionf tilt(TiltContext context) {

        final Vec3d[] axes = LivingGravity.frameOf(context.player()).axes();
        final Matrix3f turn = new Matrix3f(
                (float) axes[0].x(), (float) axes[0].y(), (float) axes[0].z(),
                (float) axes[1].x(), (float) axes[1].y(), (float) axes[1].z(),
                (float) axes[2].x(), (float) axes[2].y(), (float) axes[2].z());
        return new Quaternionf().setFromNormalized(turn);
    }
}
