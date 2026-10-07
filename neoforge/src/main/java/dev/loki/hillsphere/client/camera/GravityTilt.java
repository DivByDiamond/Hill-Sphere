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

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Turns the local player's camera upside down where the field has turned "down" to the sky. */
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
        return player == Minecraft.getInstance().player && LivingGravity.factor(player) < 0;
    }

    /** A half turn about the horizontal line the player faces along, so left and right swap but the heading holds. */
    @Override
    public Quaternionf tilt(TiltContext context) {

        final double yaw = Math.toRadians(context.player().getYRot());
        return new Quaternionf().rotationAxis((float) Math.PI, (float) -Math.sin(yaw), 0f, (float) Math.cos(yaw));
    }

    /** The head is now at the bottom of the body box, so the eye sits an eye-height below the top of it. */
    @Override
    public Vec3 eyeOffset(TiltContext context) {

        final Player player = context.player();
        final Vec3 feet = player.position();
        final Vec3 wanted = feet.add(0, player.getBbHeight() - player.getEyeHeight(), 0);
        return wanted.subtract(context.cameraPosFor(tilt(context)));
    }
}
