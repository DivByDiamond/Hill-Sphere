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
package dev.loki.hillsphere.client.air;

import dev.loki.hillsphere.config.HillSphereConfig;
import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.entity.probe.SupportProbe;
import dev.loki.hillsphere.field.control.AirControl;
import dev.loki.hillsphere.field.frame.FrameChooser;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.world.FieldLookup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Steering in a field with nothing to push off. In free flight - no floor within reach under the feet - the movement
 * keys thrust along the look and across it, jump and sneak along the frame's "up", and the air slows the player down,
 * most in the middle of the field; a hop off a floor stays a vanilla hop. Where a levitation core makes the player
 * nearly weightless, jump and sneak push up and down like swimming, floor or not.
 */
public final class AirControls {

    /** Lift above which gravity is weak enough that jump and sneak swim instead of thrusting. */
    public static final double WEIGHTLESS = 0.5;

    /** Swimming push per tick in a weightless field; with air drag this settles near 8 blocks a second. */
    private static final double SWIM = 0.008;

    private AirControls() {
    }

    public static void tick(ClientTickEvent.Post event) {

        final Minecraft mc = Minecraft.getInstance();
        final LocalPlayer player = mc.player;
        if (player == null || mc.isPaused() || player.onGround() || player.isPassenger() || player.isInWater()
                || player.isSpectator() || player.getAbilities().flying || player.isFallFlying()) {
            return;
        }
        final GravityFrame frame = LivingGravity.frameOf(player);
        final Vec3 up = LivingGravity.above(frame, Vec3.ZERO, 1);
        final int keys = (mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0);
        final boolean weightless = FieldLookup.liftAt(player) >= WEIGHTLESS;
        Vec3 speed = player.getDeltaMovement();
        if (weightless) {
            speed = speed.add(up.scale(keys * SWIM));
        }
        final double depth = FieldLookup.depthAt(player);
        if (depth > 0 && SupportProbe.floorBelow(player, frame, FrameChooser.REACH) > FrameChooser.REACH) {
            final AirControl air = HillSphereConfig.airControl();
            final Vec3 look = player.getLookAngle();
            final Vec3d thrust = air.thrust(player.xxa, weightless ? 0 : keys, player.zza, new Vec3d(look.x, look.y, look.z),
                    new Vec3d(up.x, up.y, up.z), depth);
            speed = speed.add(thrust.x(), thrust.y(), thrust.z()).scale(air.keptSpeed(depth));
        }
        player.setDeltaMovement(speed);
    }
}
