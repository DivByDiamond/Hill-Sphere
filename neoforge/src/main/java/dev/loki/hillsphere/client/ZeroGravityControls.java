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
package dev.loki.hillsphere.client;

import dev.loki.hillsphere.world.FieldLookup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** In a levitation field the jump key pushes you up and the sneak key down, like swimming. */
final class ZeroGravityControls {

    /** Lift above which gravity is weak enough that the player steers by thrust. */
    private static final double WEIGHTLESS = 0.5;
    /** Per tick; with air drag this settles near 8 blocks a second. */
    private static final double THRUST = 0.008;

    private ZeroGravityControls() {
    }

    static void tick(ClientTickEvent.Post event) {

        final Minecraft mc = Minecraft.getInstance();
        final LocalPlayer player = mc.player;
        if (player == null || mc.isPaused() || player.onGround() || player.isPassenger() || player.isInWater()
                || player.isSpectator() || player.getAbilities().flying) {
            return;
        }
        final int direction = (mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0);
        if (direction != 0 && FieldLookup.liftAt(player) >= WEIGHTLESS) {
            player.setDeltaMovement(player.getDeltaMovement().add(0, direction * THRUST, 0));
        }
    }
}
